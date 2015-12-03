package de.unipassau.isl.evs.ssh.master.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.ReferenceCountUtil;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DISCOVERY_PAYLOAD_REQUEST;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DISCOVERY_PAYLOAD_RESPONSE;

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
        final ByteBuf buffer = channel.channel().alloc().heapBuffer();

        final byte[] header = DISCOVERY_PAYLOAD_RESPONSE.getBytes();
        final String addressString = request.recipient().getAddress().getHostAddress();
        final byte[] address = addressString.getBytes();
        final int port = ((InetSocketAddress) requireComponent(Server.KEY).getAddress()).getPort();
        Log.i(TAG, "sendDiscoveryResponse " + addressString + ":" + port);

        buffer.writeInt(header.length);
        buffer.writeBytes(header);
        buffer.writeInt(address.length);
        buffer.writeBytes(address);
        buffer.writeInt(port);

        try {
            Signature signature = Signature.getInstance("ECDSA");
            signature.initSign(requireComponent(KeyStoreController.KEY).getOwnPrivateKey());
            signature.update(buffer.nioBuffer());
            final byte[] sign = signature.sign();
            buffer.writeInt(sign.length);
            buffer.writeBytes(sign);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        final DatagramPacket response = new DatagramPacket(buffer, request.sender());
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
                final ByteBuf buffer = request.content();
                final String messageType = readString(buffer);
                if (DISCOVERY_PAYLOAD_REQUEST.equals(messageType)) {
                    if (checkPubKey(buffer)) {
                        Log.d(TAG, "UDP inquiry received from " + request.sender() + " looking for me");
                        sendDiscoveryResponse(request);
                        ReferenceCountUtil.release(request);
                        return;
                    } else {
                        Log.d(TAG, "UDP inquiry received from " + request.sender() + ", but looking for another master");
                    }
                }
            }
            // forward all other packets to the pipeline
            super.channelRead(ctx, msg);
        }

        /**
         * Read a string.
         */
        private String readString(ByteBuf buffer) {
            final int length = buffer.readInt();
            if (length < 0 || length > 0xFFFF) {
                return null;
            }
            byte[] value = new byte[length];
            buffer.readBytes(value);
            return new String(value);
        }

        /**
         * Read and verify public key.
         */
        private boolean checkPubKey(ByteBuf buffer) {
            final X509Certificate masterCert = requireComponent(NamingManager.KEY).getMasterCertificate();
            final int pubKeyLength = buffer.readInt();
            final byte[] expectedPubKey = masterCert.getPublicKey().getEncoded();
            if (pubKeyLength != expectedPubKey.length) {
                return false;
            }
            byte[] actualPubKey = new byte[expectedPubKey.length];
            buffer.readBytes(actualPubKey);
            return Arrays.equals(expectedPubKey, expectedPubKey);
        }
    }
}
