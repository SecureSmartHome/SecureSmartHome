package de.unipassau.isl.evs.ssh.core.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.naming.UnresolvableNamingException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.ScheduledFuture;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_MILLIS_BETWEEN_BROADCASTS;


/**
 * This component is responsible for sending UDP discovery packets and signalling the new address and port back to the
 * {@link Client} if a new Master has been found.
 *
 * @author Phil, Niko
 */
public class UDPDiscoveryClient extends AbstractComponent {
    public static final Key<UDPDiscoveryClient> KEY = new Key<>(UDPDiscoveryClient.class);

    private static final String TAG = UDPDiscoveryClient.class.getSimpleName();

    /**
     * The channel listening for incoming UDP packets.
     */
    private ChannelFuture channel;
    /**
     * The lock used to prevent android from hibernating the network Stack while UDP discovery is running.
     */
    private WifiManager.MulticastLock multicastLock;
    /**
     * A boolean indicating when the discovery has been started using {@link #startDiscovery()} and no new Master has been
     * found or {@link #stopDiscovery()} has been called.
     */
    private boolean isDiscoveryRunning = false;
    /**
     * The Future returned from the {@link io.netty.channel.EventLoop} for the next scheduled retry of {@link #sendDiscoveryRequest()}
     */
    private ScheduledFuture<?> retryFuture;

    /**
     * Start discovery if it is not running yet and send the first discovery request.
     */
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
                        .handler(new ResponseHandler())
                        .option(ChannelOption.SO_BROADCAST, true);
                channel = b.bind(CoreConstants.DISCOVERY_PORT);
            }

            sendDiscoveryRequest();
            scheduleDiscoveryRetry();
        }
    }

    /**
     * Schedule the next run of {@link #sendDiscoveryRequest()} if it hasn't been scheduled yet.
     *
     * @return the Future returned by {@link io.netty.channel.EventLoop#schedule(Callable, long, TimeUnit)}
     */
    private synchronized ScheduledFuture<?> scheduleDiscoveryRetry() {
        Log.v(TAG, "scheduleDiscoveryRetry()");
        // don't schedule a second execution if one is already pending
        final boolean isExecutionPending = retryFuture != null && !retryFuture.isDone();
        if (isDiscoveryRunning && !isExecutionPending) {
            if (requireComponent(Client.KEY).isChannelOpen()) {
                Log.d(TAG, "scheduleDiscoveryRetry(), but Client Channel is open. Was stopDiscovery called?");
            }
            retryFuture = requireComponent(Client.KEY).getExecutor().schedule(new Runnable() {
                @Override
                public void run() {
                    if (isDiscoveryRunning) {
                        sendDiscoveryRequest();
                        /* Mark this future as completed, so that the next discovery request will be scheduled.
                         * Otherwise retryFuture.isDone() would be false until this method terminates and the following
                         * recursive call wouldn't schedule the next execution. */
                        retryFuture = null;
                        scheduleDiscoveryRetry();
                    }
                }
            }, CLIENT_MILLIS_BETWEEN_BROADCASTS, TimeUnit.MILLISECONDS);
            retryFuture.addListener(new FutureListener<Object>() {
                @Override
                public void operationComplete(Future future) throws Exception {
                    if (!future.isSuccess()) {
                        Log.w(TAG, "Could not reschedule execution of UDP discovery", future.cause());
                    }
                }
            });
        } else {
            Log.d(TAG, "not scheduling another retry because " +
                    "isDiscoveryRunning = " + isDiscoveryRunning +
                    ", retryFuture = " + retryFuture);
        }
        return retryFuture;
    }

    /**
     * Stop the discovery and cancel all pending discovery requests.
     */
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

    /**
     * Send a single UDP discovery request. It contains of {@link CoreConstants#DISCOVERY_PAYLOAD_REQUEST}
     * as header followed by the Public Key of the sought-after Master.
     * Both byte arrays are prefixed by their length as int.
     *
     * @return the ChannelFuture returned by {@link io.netty.channel.Channel#write(Object)}
     */
    private ChannelFuture sendDiscoveryRequest() {
        Log.v(TAG, "sendDiscoveryRequest");

        final X509Certificate masterCert;
        try {
            masterCert = requireComponent(NamingManager.KEY).getMasterCert();
        } catch (UnresolvableNamingException e) {
            Log.w(TAG, "Can't search for Master via UDP discovery when no Master Certificate is available," +
                    "will retry later", e);
            return channel.channel().newFailedFuture(e);
        }
        final byte[] header = CoreConstants.DISCOVERY_PAYLOAD_REQUEST.getBytes();
        final byte[] pubKeyEncoded = masterCert.getPublicKey().getEncoded();
        final ByteBuf buffer = channel.channel().alloc().buffer(
                header.length + pubKeyEncoded.length + (Integer.SIZE / Byte.SIZE) * 2);
        buffer.writeInt(header.length);
        buffer.writeBytes(header);
        buffer.writeInt(pubKeyEncoded.length);
        buffer.writeBytes(pubKeyEncoded);

        final InetSocketAddress recipient = new InetSocketAddress(
                CoreConstants.DISCOVERY_HOST, CoreConstants.DISCOVERY_PORT);
        final DatagramPacket request = new DatagramPacket(buffer, recipient);
        return channel.channel().writeAndFlush(request);
    }

    /**
     * The ChannelHandler that receives and parses incoming UDP responses and forwards them to the Client
     * if they indicate a new master.
     */
    private class ResponseHandler extends ChannelHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof DatagramPacket) {
                final DatagramPacket request = (DatagramPacket) msg;
                final ByteBuf buffer = request.content();

                final int dataStart = buffer.readerIndex();
                final boolean headerValid = checkHeader(buffer);
                if (headerValid) {
                    final String address = readHeader(buffer);
                    final int port = buffer.readInt();
                    final int dataEnd = buffer.readerIndex();
                    if (checkSignature(buffer, dataStart, dataEnd)) {
                        // got a new address for the master!
                        ReferenceCountUtil.release(request);
                        Log.i(TAG, "UDP response received " + address + ":" + port);

                        stopDiscovery();
                        requireComponent(Client.KEY).onDiscoverySuccessful(address, port);
                    }
                }
            }
            // forward all other packets to the pipeline
            super.channelRead(ctx, msg);
        }

        private boolean checkSignature(ByteBuf buffer, int dataStart, int dataEnd) throws UnresolvableNamingException {
            try {
                Signature signature = Signature.getInstance("ECDSA");
                signature.initVerify(requireComponent(NamingManager.KEY).getMasterCert());
                signature.update(buffer.nioBuffer(0, dataEnd - dataStart));
                final byte[] sign = readSign(buffer);
                return signature.verify(sign);
            } catch (GeneralSecurityException e) {
                Log.w(TAG, "Could not validate signature", e);
                return false;
            }
        }

        /**
         * Read and verify header.
         */
        private boolean checkHeader(ByteBuf buffer) {
            final int headerLength = buffer.readInt();
            final byte[] expectedHeader = CoreConstants.DISCOVERY_PAYLOAD_REQUEST.getBytes();
            if (headerLength != expectedHeader.length) {
                return false;
            }
            byte[] actualHeader = new byte[expectedHeader.length];
            buffer.readBytes(actualHeader);
            return Arrays.equals(expectedHeader, actualHeader);
        }

        /**
         * Read master address.
         */
        private String readHeader(ByteBuf buffer) {
            final int length = buffer.readInt();
            if (length < 0) {
                return null;
            }
            byte[] value = new byte[length];
            buffer.readBytes(value);
            return new String(value);
        }

        /**
         * Read signature.
         */
        private byte[] readSign(ByteBuf buffer) {
            final int length = buffer.readInt();
            if (length < 0) {
                return null;
            }
            byte[] value = new byte[length];
            buffer.readBytes(value);
            return value;
        }
    }
}
