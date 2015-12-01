package de.unipassau.isl.evs.ssh.master.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetSocketAddress;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.network.Client;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

/**
 * This component is responsible for responding to UDP discovery packets, signalling the address and port back to
 * {@link Client}s searching for this Master.
 *
 * @author Niko
 */
public class UDPDiscoveryServer extends AbstractComponent {
    public static final Key<UDPDiscoveryServer> KEY = new Key<>(UDPDiscoveryServer.class);

    private static final String TAG = UDPDiscoveryServer.class.getSimpleName();

    /**
     * The channel listening for incoming UDP connections on the port of the client.
     * Use {@link ChannelFuture#sync()} to wait for client startup.
     */
    private ChannelFuture channel;
    /**
     * The lock used to prevent android from hibernating the network Stack while UDP discovery is running.
     */
    private WifiManager.MulticastLock multicastLock;

    @Override
    public void init(Container container) {
        super.init(container);

        // Acquire lock
        final WifiManager wifi = (WifiManager) requireComponent(ContainerService.KEY_CONTEXT)
                .getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock(getClass().getSimpleName());
        multicastLock.acquire();

        // Setup UDP Channel
        Bootstrap b = new Bootstrap()
                .channel(NioDatagramChannel.class)
                .group(requireComponent(Server.KEY).getExecutor())
                .handler(new RequestHandler())
                .option(ChannelOption.SO_BROADCAST, true);
        channel = b.bind(CoreConstants.NettyConstants.DISCOVERY_PORT);
    }

    @Override
    public void destroy() {
        if (multicastLock != null) {
            if (multicastLock.isHeld()) {
                multicastLock.release();
            }
            multicastLock = null;
        }
        if (channel.channel().isActive()) {
            channel.channel().close();
        }
        super.destroy();
    }

    /**
     * Send a response with the connection information of this Master to the requesting Client.
     *
     * @param request the request sent from a {@link Client}
     * @return the ChannelFuture returned by {@link io.netty.channel.Channel#write(Object)}
     */
    private ChannelFuture sendDiscoveryResponse(DatagramPacket request) {
        final int port = ((InetSocketAddress) requireComponent(Server.KEY).getAddress()).getPort();
        final String string = CoreConstants.NettyConstants.DISCOVERY_PAYLOAD_RESPONSE + port;
        Log.i(TAG, "sendDiscoveryResponse: " + string);
        final ByteBuf payload = Unpooled.copiedBuffer(string, CharsetUtil.UTF_8);
        final DatagramPacket response = new DatagramPacket(payload, request.sender());
        return channel.channel().writeAndFlush(response);
    }

    /**
     * The ChannelHandler that receives and parses incoming UDP requests and calls {@link #sendDiscoveryResponse(DatagramPacket)}
     * in order to respond to them.
     */
    private class RequestHandler extends ChannelHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof DatagramPacket) {
                final DatagramPacket request = (DatagramPacket) msg;
                String messageData = request.content().toString(CharsetUtil.UTF_8);
                if (messageData.startsWith(CoreConstants.NettyConstants.DISCOVERY_PAYLOAD_REQUEST)) {
                    Log.d(TAG, "UDP request " + request.sender() + " received: " + messageData);
                    sendDiscoveryResponse(request);
                    ReferenceCountUtil.release(request);
                    return;
                }
            }
            // forward all other packets to the pipeline
            super.channelRead(ctx, msg);
        }
    }
}
