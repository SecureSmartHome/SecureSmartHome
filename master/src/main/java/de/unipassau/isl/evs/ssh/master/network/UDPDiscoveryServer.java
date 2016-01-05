package de.unipassau.isl.evs.ssh.master.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.Signature;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;
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
 * @author Niko Fink
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
        channel = b.bind(CoreConstants.NettyConstants.DISCOVERY_SERVER_PORT);
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
        Log.i(TAG, "sendDiscoveryResponse with connection data " + addressString + ":" + port + " to " + request.sender());

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
            Log.w(TAG, "Could not send UDP discovery response", e);
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
            try {
                final DatagramPacket request = (DatagramPacket) msg;
                final ByteBuf buffer = request.content();
                buffer.markReaderIndex();
                final String messageType = readString(buffer);
                if (DISCOVERY_PAYLOAD_RESPONSE.equals(messageType)) {
                    Log.d(TAG, "UDP discovery Server can't handle UDP response from " + request.sender());
                    return;
                } else if (!DISCOVERY_PAYLOAD_REQUEST.equals(messageType)) {
                    Log.d(TAG, "Discarding UDP packet with illegal message type from " + request.sender() + ": " + messageType);
                    return;
                }
                final DeviceID ownID = requireComponent(NamingManager.KEY).getOwnID();
                final DeviceID clientID = readDeviceID(buffer);
                if (clientID == null) {
                    Log.d(TAG, "Discarding UDP inquiry without client ID from " + request.sender());
                    return;
                }

                final boolean isMasterKnown = buffer.readBoolean();
                if (isMasterKnown) {
                    // if the master is known to the device, the IDs must match
                    final DeviceID masterID = readDeviceID(buffer);
                    if (!ownID.equals(masterID)) {
                        Log.d(TAG, "Discarding UDP inquiry from " + clientID + "(" + request.sender() + ") " +
                                "that is not looking for me (" + ownID + ") but " + masterID);
                        return;
                    }
                } else {
                    // if the device doesn't know his master, the master must know the device
                    if (!isDeviceRegistered(clientID)) {
                        Log.d(TAG, "Discarding UDP inquiry from " + clientID + "(" + request.sender() + ") " +
                                "that is looking for any master and is not registered");
                        return;
                    }
                }
                Log.d(TAG, "UDP inquiry received from " + clientID + "(" + request.sender() + ") that is looking for "
                        + (isMasterKnown ? "me" : "any master and is registered here"));
                sendDiscoveryResponse(request);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        private boolean isDeviceRegistered(DeviceID clientID) {
            return requireComponent(SlaveController.KEY).getSlave(clientID) != null
                    || requireComponent(UserManagementController.KEY).getUserDevice(clientID) != null;
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
         * Read a DeviceID.
         */
        private DeviceID readDeviceID(ByteBuf buffer) {
            final int length = buffer.readInt();
            if (length != DeviceID.ID_LENGTH) {
                return null;
            }
            byte[] value = new byte[DeviceID.ID_LENGTH];
            buffer.readBytes(value);
            return new DeviceID(value);
        }
    }
}
