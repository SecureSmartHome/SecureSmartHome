/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.core.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
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

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DISCOVERY_HOST;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DISCOVERY_PAYLOAD_REQUEST;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DISCOVERY_PAYLOAD_RESPONSE;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DISCOVERY_SERVER_PORT;

/**
 * This component is responsible for sending UDP discovery packets and signalling the new address and port back to the
 * {@link Client} if a new Master has been found.
 *
 * @author Phil Werli & Niko Fink
 */
public class UDPDiscoveryClient extends AbstractComponent {
    public static final Key<UDPDiscoveryClient> KEY = new Key<>(UDPDiscoveryClient.class);

    private static final String TAG = UDPDiscoveryClient.class.getSimpleName();

    /**
     * The maximum number of seconds the broadcast waits to be sent again.
     */
    private static final long CLIENT_MILLIS_BETWEEN_BROADCASTS = TimeUnit.SECONDS.toMillis(2);

    /**
     * The channel listening for incoming UDP packets.
     */
    private ChannelFuture channel;
    /**
     * The lock used to prevent android from hibernating the network Stack while UDP discovery is running.
     */
    private WifiManager.MulticastLock multicastLock;
    /**
     * A boolean indicating when the discovery has been started using {@link #startDiscovery(long)} and no new Master has been
     * found or {@link #stopDiscovery()} has been called.
     */
    private boolean isDiscoveryRunning = false;
    /**
     * The Future returned from the {@link io.netty.channel.EventLoop} for the next scheduled retry of {@link #sendDiscoveryRequest()}
     */
    private ScheduledFuture<?> retryFuture;
    /**
     * The timestamp when discovery should stop, or {@code 0} if discovery should run indefinitely.
     */
    private long timeout = 0;

    /**
     * Start discovery if it is not running yet and send the first discovery request.
     */
    public void startDiscovery(long timeout) {
        if (this.timeout > 0 && timeout > 0) {
            this.timeout = Math.max(this.timeout, System.currentTimeMillis() + timeout);
        } else if (timeout > 0) {
            this.timeout = System.currentTimeMillis() + timeout;
        } else {
            this.timeout = 0;
        }
        Log.i(TAG, "startDiscovery, " + (isDiscoveryRunning ? "already" : "currently not") + " running with timeout " + timeout);
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
                        .group(requireComponent(ExecutionServiceComponent.KEY))
                        .handler(new ResponseHandler())
                        .option(ChannelOption.SO_BROADCAST, true);
                channel = b.bind(0);
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
            if (requireComponent(Client.KEY).isChannelOpen() && timeout == 0) {
                Log.d(TAG, "scheduleDiscoveryRetry() running indefinitely, but Client Channel is open. Was stopDiscovery called?");
            }
            retryFuture = requireComponent(ExecutionServiceComponent.KEY).schedule(new Runnable() {
                @Override
                public void run() {
                    if (timeout > 0 && System.currentTimeMillis() > timeout) {
                        Log.i(TAG, "Stopping discovery after timeout");
                        stopDiscovery();
                    }
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
        timeout = 0;
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
     * Send a single UDP discovery request. It contains of {@link CoreConstants.NettyConstants#DISCOVERY_PAYLOAD_REQUEST}
     * as header followed by the Public Key of the sought-after Master.
     * Both byte arrays are prefixed by their length as int.
     *
     * @return the ChannelFuture returned by {@link io.netty.channel.Channel#write(Object)}
     */
    private ChannelFuture sendDiscoveryRequest() {
        final NamingManager namingManager = requireComponent(NamingManager.KEY);
        if (namingManager.isMasterIDKnown()) {
            Log.v(TAG, "sendDiscoveryRequest looking for Master " + namingManager.getMasterID());
        } else {
            Log.v(TAG, "sendDiscoveryRequest looking for any Master");
        }

        final byte[] header = DISCOVERY_PAYLOAD_REQUEST.getBytes();
        final byte[] ownIDBytes = namingManager.getOwnID().getIDBytes();
        final ByteBuf buffer = channel.channel().alloc().buffer();
        buffer.writeInt(header.length);
        buffer.writeBytes(header);
        buffer.writeInt(ownIDBytes.length);
        buffer.writeBytes(ownIDBytes);

        if (namingManager.isMasterIDKnown()) {
            buffer.writeBoolean(true);
            final byte[] masterIDBytes = namingManager.getMasterID().getIDBytes();
            buffer.writeInt(masterIDBytes.length);
            buffer.writeBytes(masterIDBytes);
        } else {
            buffer.writeBoolean(false);
        }

        final InetSocketAddress recipient = new InetSocketAddress(DISCOVERY_HOST, DISCOVERY_SERVER_PORT);
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
            try {
                final DatagramPacket request = (DatagramPacket) msg;
                final ByteBuf buffer = request.content();

                final int dataStart = buffer.readerIndex();
                final String messageType = readString(buffer);
                if (DISCOVERY_PAYLOAD_RESPONSE.equals(messageType)) {
                    String addressString = readString(buffer);
                    // the address originally sent from the master is discarded, as using the address from which the
                    // message came works even if I'm in a different subnet
                    final InetAddress address = request.sender().getAddress();
                    final int port = buffer.readInt();
                    final InetSocketAddress socketAddress = new InetSocketAddress(address, port);

                    final int dataEnd = buffer.readerIndex();
                    if (checkSignature(buffer, dataStart, dataEnd)) {
                        // got a new address for the master!
                        Log.i(TAG, "UDP response received " + socketAddress);
                        stopDiscovery();
                        requireComponent(Client.KEY).onMasterFound(socketAddress);
                    } else {
                        Log.i(TAG, "UDP response received " + socketAddress
                                + ", but signature is invalid");
                    }
                } else if (!DISCOVERY_PAYLOAD_REQUEST.equals(messageType)) {
                    //discard own requests that are echoed by the router and requests sent by other clients and warn about all other packets
                    Log.d(TAG, "Discarding UDP packet with illegal message type: " + messageType);
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        private boolean checkSignature(ByteBuf buffer, int dataStart, int dataEnd) {
            try {
                final NamingManager namingManager = requireComponent(NamingManager.KEY);
                if (namingManager.isMasterKnown()) {
                    Signature signature = Signature.getInstance("ECDSA");
                    signature.initVerify(namingManager.getMasterCertificate());
                    signature.update(buffer.nioBuffer(0, dataEnd - dataStart));
                    final byte[] sign = readSign(buffer);
                    return signature.verify(sign);
                } else {
                    return true; //trust for now until I received the master cert matching my Master's ID
                }
            } catch (GeneralSecurityException e) {
                Log.w(TAG, "Could not validate signature", e);
                return false;
            }
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
