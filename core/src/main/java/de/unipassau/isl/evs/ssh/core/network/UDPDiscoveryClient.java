package de.unipassau.isl.evs.ssh.core.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
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
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.CLIENT_MILLIS_BETWEEN_BROADCASTS;

/**
 * This component is responsible for sending UDP discovery packets and signalling the new address and port back to the
 * {@link Client} if a new Master has been found.
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
     * Send a single UDP discovery request
     *
     * @return the ChannelFuture returned by {@link io.netty.channel.Channel#write(Object)}
     */
    private ChannelFuture sendDiscoveryRequest() {
        Log.v(TAG, "sendDiscoveryRequest");
        final ByteBuf payload = Unpooled.copiedBuffer(CoreConstants.DISCOVERY_PAYLOAD_REQUEST, CharsetUtil.UTF_8);
        final InetSocketAddress recipient = new InetSocketAddress(CoreConstants.DISCOVERY_HOST, CoreConstants.DISCOVERY_PORT);
        final DatagramPacket request = new DatagramPacket(payload, recipient);
        return channel.channel().writeAndFlush(request);
    }

    /**
     * Schedule the next run of {@link #sendDiscoveryRequest()} if it hasn't been scheduled yet.
     *
     * @return the Future returned by {@link io.netty.channel.EventLoop#schedule(Callable, long, TimeUnit)}
     */
    private synchronized ScheduledFuture<?> scheduleDiscoveryRetry() {
        Log.v(TAG, "scheduleDiscoveryRetry, status: " + retryFuture);
        if (retryFuture == null || retryFuture.isDone()) { // don't schedule a second execution if one is already pending
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
     * The ChannelHandler that receives and parses incoming UDP responses and forwards them to the Client
     * if they indicate a new master.
     */
    private class ResponseHandler extends ChannelHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof DatagramPacket) {
                final DatagramPacket response = (DatagramPacket) msg;
                String messageData = response.content().toString(CharsetUtil.UTF_8);

                if (messageData.startsWith(CoreConstants.DISCOVERY_PAYLOAD_RESPONSE)) {
                    final InetAddress address = response.sender().getAddress();
                    int port = Integer.parseInt(messageData.substring(CoreConstants.DISCOVERY_PAYLOAD_RESPONSE.length()));
                    ReferenceCountUtil.release(response);
                    // got a new address for the master!
                    Log.i(TAG, "UDP response received " + address + ":" + port);

                    stopDiscovery();
                    requireComponent(Client.KEY).onDiscoverySuccessful(address, port);
                    return;
                } else if (messageData.startsWith(CoreConstants.DISCOVERY_PAYLOAD_REQUEST)) {
                    // discard own requests that are echoed by the router
                    ReferenceCountUtil.release(response);
                    return;
                }
            }
            // forward all other packets to the pipeline
            super.channelRead(ctx, msg);
        }
    }
}
