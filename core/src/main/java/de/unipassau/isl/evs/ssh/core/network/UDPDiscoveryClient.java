package de.unipassau.isl.evs.ssh.core.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ScheduledFuture;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_MILLIS_BETWEEN_BROADCASTS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.DEFAULT_PORT;

public class UDPDiscoveryClient extends AbstractComponent {
    public static final Key<UDPDiscoveryClient> KEY = new Key<>(UDPDiscoveryClient.class);
    private static final String TAG = UDPDiscoveryClient.class.getSimpleName();
    /**
     * The channel listening for incoming UDP connections on the port of the client.
     * Use {@link ChannelFuture#sync()} to wait for client startup.
     */
    private ChannelFuture channel;
    private WifiManager.MulticastLock multicastLock;
    private boolean isDiscoveryRunning = true;
    private ScheduledFuture<?> retryFuture;


    public void startDiscovery() {
        Log.i(TAG, "startDiscovery, " + (isDiscoveryRunning ? "already" : "currently not") + " running");
        if (!isDiscoveryRunning) {
            isDiscoveryRunning = true;

            // Acquire lock
            if (multicastLock == null) {
                final WifiManager wifi = (WifiManager) requireComponent(ContainerService.KEY_CONTEXT)
                        .getSystemService(Context.WIFI_SERVICE);
                multicastLock = wifi.createMulticastLock(getClass().getSimpleName());
                multicastLock.setReferenceCounted(false);
            }
            multicastLock.acquire();

            // Setup UDP Channel
            if (channel == null) {
                Bootstrap b = new Bootstrap()
                        .channel(NioDatagramChannel.class)
                        .group(requireComponent(Client.KEY).getExecutor())
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(LoggingHandler.class.getSimpleName(), new LoggingHandler(LogLevel.TRACE));
                                ch.pipeline().addLast(ResponseHandler.class.getSimpleName(), new ResponseHandler());
                            }
                        })
                        .option(ChannelOption.SO_BROADCAST, true);
                channel = b.bind(CoreConstants.DISCOVERY_PORT);
            }

            sendDiscoveryRequest();
            scheduleDiscoveryRetry();
        }
    }

    private ScheduledFuture<?> scheduleDiscoveryRetry() {
        Log.v(TAG, "scheduleDiscoveryRetry, status: " + retryFuture);
        if (retryFuture == null || retryFuture.isDone()) {
            retryFuture = requireComponent(Client.KEY).getExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    if (isDiscoveryRunning) {
                        sendDiscoveryRequest();
                        scheduleDiscoveryRetry();
                    }
                }
            }, CLIENT_MILLIS_BETWEEN_BROADCASTS, TimeUnit.MILLISECONDS);
        }
        return retryFuture;
    }

    private ChannelFuture sendDiscoveryRequest() {
        Log.v(TAG, "sendDiscoveryRequest");
        final ByteBuf payload = Unpooled.copiedBuffer(CoreConstants.DISCOVERY_PAYLOAD_REQUEST, CharsetUtil.UTF_8);
        final InetSocketAddress recipient = new InetSocketAddress(CoreConstants.BROADCAST_ADDRESS, DEFAULT_PORT);
        final DatagramPacket request = new DatagramPacket(payload, recipient);
        return channel.channel().writeAndFlush(request);
    }

    public void stopDiscovery() {
        Log.d(TAG, "stopDiscovery " + (isDiscoveryRunning ? "currently running" : ""));
        isDiscoveryRunning = false;
        if (retryFuture != null && !retryFuture.isDone()) {
            retryFuture.cancel(true);
        }
        if (multicastLock != null) {
            if (multicastLock.isHeld()) {
                multicastLock.release();
            }
            multicastLock = null;
        }
    }

    private class ResponseHandler extends ChannelHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof DatagramPacket) {
                final DatagramPacket datagram = (DatagramPacket) msg;
                String messageData = datagram.content().toString(CharsetUtil.UTF_8);
                if (messageData.startsWith(CoreConstants.DISCOVERY_PAYLOAD_RESPONSE)) {
                    final InetAddress address = datagram.sender().getAddress();
                    int port = Integer.parseInt(messageData.substring(CoreConstants.DISCOVERY_PAYLOAD_RESPONSE.length()));
                    Log.i(TAG, "UDP response received " + address + ":" + port);
                    stopDiscovery();
                    requireComponent(Client.KEY).onDiscoverySuccessful(address, port);
                    return;
                }
            }
            super.channelRead(ctx, msg);
        }
    }
}
