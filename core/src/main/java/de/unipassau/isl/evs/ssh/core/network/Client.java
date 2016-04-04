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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_HANDSHAKE_FINISHED;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_LOCAL_CONNECTION;

/**
 * A netty stack holding a connection to the Master and handling communication with it using a netty pipeline.
 * For details about the pipeline, see {@link ClientHandshakeHandler}.
 * For details about UDP discovery, see {@link #initClient()}, {@link #shouldReconnectTCP()} and the {@link UDPDiscoveryClient}.
 * This component is used by the Slave and the end-user android App.
 *
 * @author Phil Werli & Niko Fink
 */
public class Client extends AbstractComponent {
    public static final Key<Client> KEY = new Key<>(Client.class);
    static final String PREF_TOKEN_ACTIVE = Client.class.getName() + ".PREF_TOKEN_ACTIVE";
    static final String PREF_TOKEN_PASSIVE = Client.class.getName() + ".PREF_TOKEN_PASSIVE";
    static final String LAST_HOST = Client.class.getName() + ".LAST_HOST";
    static final String LAST_PORT = Client.class.getName() + ".LAST_PORT";
    static final String PREF_HOST = Client.class.getName() + ".PREF_HOST";
    static final String PREF_PORT = Client.class.getName() + ".PREF_PORT";
    private static final String TAG = Client.class.getSimpleName();

    /**
     * If this number of milliseconds between two disconnects is exceeded, they are deemed as unrelated
     */
    private static final long CLIENT_MILLIS_BETWEEN_DISCONNECTS = TimeUnit.SECONDS.toMillis(10);
    /**
     * If there are more disconnects within {@link #CLIENT_MILLIS_BETWEEN_DISCONNECTS} than this number,
     * the client should retry connecting with the explicitly set master or try UDP discovery.
     */
    private static final int CLIENT_MAX_DISCONNECTS = 5;
    /**
     * An Android BroadcastReceiver that is notified once the Phone connects to or is disconnected from a WiFi network.
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isConnectionLocal()) {
                final boolean connectionEstablished = isConnectionEstablished();
                final long timeout;
                // only search for 30 seconds if a connection is already established
                if (connectionEstablished) {
                    timeout = TimeUnit.SECONDS.toMillis(30);
                } else {
                    timeout = 0;
                }
                Log.d(TAG, (connectionEstablished ? "Rescanning" : "Scanning") + " network for possible local connections after NetworkInfo change: " + intent);
                requireComponent(UDPDiscoveryClient.KEY).startDiscovery(timeout);
            }
        }
    };
    /**
     * The channel listening for incoming TCP connections on the port of the client.
     * Use {@link ChannelFuture#sync()} to wait for client startup.
     */
    private ChannelFuture channelFuture;
    /**
     * Boolean indicating if the client connection is active.
     */
    private boolean isActive;
    /**
     * Int used to calculate the time between the last and the current timeout.
     */
    private long lastDisconnect = 0;
    /**
     * Int that saves how many timeouts happened in a row.
     */
    private int disconnectsInARow = 0;

    private final List<ClientConnectionListener> listeners = new ArrayList<>();

    /**
     * Configure netty and initialize related Components.
     * Afterwards call {@link #initClient()} method to start the netty IO client asynchronously.
     */
    @Override
    public void init(Container container) {
        super.init(container);
        // Configure netty
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory() {
            @Override
            public InternalLogger newInstance(String name) {
                return new NettyInternalLogger(name);
            }
        });
        ResourceLeakDetector.setLevel(CoreConstants.NettyConstants.RESOURCE_LEAK_DETECTION);
        // And try to connect
        isActive = true;
        initClient();
        // register BroadcastListener
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        requireComponent(ContainerService.KEY_CONTEXT).registerReceiver(broadcastReceiver, filter);
    }

    /**
     * Initializes the netty client and tries to connect to the server.
     */
    protected synchronized void initClient() {
        Log.d(TAG, "initClient");
        if (!isActive) {
            Log.w(TAG, "Not starting Client that has been explicitly shut-down");
            return;
        }
        if (isChannelOpen()) {
            Log.w(TAG, "Not starting Client that is already connected");
            return;
        } else {
            // Close channels open from previous connections
            if (channelFuture != null && channelFuture.channel() != null) {
                Log.v(TAG, "Cleaning up " + (channelFuture.channel().isOpen() ? "open" : "closed")
                        + " Channel from previous connection attempt");
                channelFuture.channel().close();
            }
            // Clean-up the closed channel as it is no longer needed
            channelFuture = null;
        }

        // And queue the (re-)connect
        final Future<?> future = requireComponent(ExecutionServiceComponent.KEY).submit(new Runnable() {
            @Override
            public void run() {
                attemptConnectClient();
            }
        });
        future.addListener(new FutureListener<Object>() {
            @Override
            public void operationComplete(Future future) throws Exception {
                if (!future.isSuccess()) {
                    Log.w(TAG, "Could not schedule connect to master", future.cause());
                }
            }
        });
    }

    /**
     * Checks if the address of the master is known and not to many connections attempts have been made and
     * connects to the found address or starts UDP discovery.
     */
    private void attemptConnectClient() {
        // Read the previous host and port from the shared preferences
        InetSocketAddress address = getLastAddress();

        // Connect to TCP if the address of the Server/Master is known and not too many connection attempts have failed
        boolean shouldReconnectTCP = shouldReconnectTCP();
        if (address != null && !shouldReconnectTCP) {
            // if too many attempts failed with the last address, retry with the configured address
            editPrefs().setLastAddress(null).commit();
            lastDisconnect = 0;
            disconnectsInARow = 0;
            shouldReconnectTCP = true;
            address = getConfiguredAddress();
        } else if (address == null) {
            // if no previous address is found, use the configured address
            address = getConfiguredAddress();
        }

        if (address != null && shouldReconnectTCP) {
            connectClient(address);
        } else {
            if (address == null) {
                Log.w(TAG, "No master known, starting UDP discovery");
            } else {
                Log.w(TAG, "Too many disconnects from " + address + ", trying UDP discovery");
            }
            requireComponent(UDPDiscoveryClient.KEY).startDiscovery(0);
        }
    }

    /**
     * A connection attempt should be made if no more than CLIENT_MAX_DISCONNECTS attempts have been made
     * with the last attempt having been made no more than CLIENT_MILLIS_BETWEEN_DISCONNECTS ago.
     */
    private boolean shouldReconnectTCP() {
        return disconnectsInARow < CLIENT_MAX_DISCONNECTS
                || System.currentTimeMillis() - lastDisconnect > CLIENT_MILLIS_BETWEEN_DISCONNECTS;
    }

    /**
     * Tries to establish a TCP connection to the Server with the given host and port.
     * If the connect ist successful, {@link #getHandshakeHandler()} is used to add the
     * required Handlers to the pipeline.
     * If the connection fails, {@link #channelClosed(Channel)} is called until to many retries are made and the Client
     * switches to searching the master via UDP discovery using the {@link UDPDiscoveryClient}.
     */
    private void connectClient(InetSocketAddress address) {
        Log.i(TAG, "Client connecting to " + address);
        notifyClientConnecting(address.getHostString(), address.getPort());

        // TCP Connection
        Bootstrap b = new Bootstrap()
                .group(requireComponent(ExecutionServiceComponent.KEY))
                .channel(NioSocketChannel.class)
                .handler(getHandshakeHandler())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(5));

        // Wait for the start of the client
        channelFuture = b.connect(address);
        channelFuture.addListener(new ChannelFutureListener() {
            /**
             * Called once the operation completes, either because the connect was successful or because of an error.
             */
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    Log.v(TAG, "Channel open");
                    channelOpen(future.channel());
                } else {
                    Log.v(TAG, "Channel open failed");
                    channelClosed(future.channel());
                }
            }
        });
        channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
            /**
             * Called once the connection is closed.
             */
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Log.v(TAG, "Channel closed");
                channelClosed(future.channel());
            }
        });
    }

    /**
     * HandshakeHandler can be changed or mocked for testing
     *
     * @return the ClientHandshakeHandler to use
     */
    @NonNull
    protected ChannelHandler getHandshakeHandler() {
        return new ClientHandshakeHandler(getContainer());
    }

    /**
     * Called once the TCP connection is established.
     *
     * @see ClientHandshakeHandler#channelActive(ChannelHandlerContext) triggers the Handshake after this method is complete
     */
    protected void channelOpen(Channel channel) {
        if (channel != this.channelFuture.channel()) {
            return; //channel has already been exchanged by new one, don't stop discovery
        }
        requireComponent(UDPDiscoveryClient.KEY).stopDiscovery();
    }

    /**
     * Called once the TCP connection is closed or if it couldn't be established at all.
     * Increments the disconnect counter and tries to re-establish the connection.
     */
    protected synchronized void channelClosed(Channel channel) {
        if (this.channelFuture != null && this.channelFuture.channel() != channel) {
            return; //channel has already been exchanged by new one, don't start another client
        }
        notifyClientDisconnected();
        if (isActive) {
            long time = System.currentTimeMillis();
            final long diff = time - lastDisconnect;
            if (lastDisconnect <= 0 || diff <= CLIENT_MILLIS_BETWEEN_DISCONNECTS) {
                // if the last disconnect was in the near past, increment the counter of disconnects in a row
                lastDisconnect = time;
                disconnectsInARow++;
                Log.w(TAG, disconnectsInARow + ". disconnect within the last " + diff + "ms, retrying");
            } else {
                // otherwise just retry
                lastDisconnect = time;
                Log.i(TAG, "First disconnect recently, retrying");
            }
            initClient();
        } else {
            Log.i(TAG, "Client disconnected, but not restarting because destroy() has been called");
        }
    }

    /**
     * Called by {@link UDPDiscoveryClient} once it found a possible address of the master.
     * Saves the new address.
     */
    public void onMasterFound(InetSocketAddress address) {
        onMasterFound(address, null);
    }

    /**
     * Called by {@link UDPDiscoveryClient} once it found a possible address of the master.
     * Saves the new address.
     */
    public void onMasterFound(InetSocketAddress address, String token) {
        Log.i(TAG, "discovery successful, found " + address + " with token " + token);
        final PrefEditor editor = editPrefs().setLastAddress(address);
        if (token != null) {
            editor.setActiveRegistrationToken(token);
        }
        editor.commit();
        addressChanged(address);
    }

    public void onMasterConfigured(InetSocketAddress address) {
        Log.i(TAG, "master configured as " + address);
        editPrefs().setConfiguredAddress(address).commit();
        addressChanged(address);
    }

    private void addressChanged(InetSocketAddress address) {
        lastDisconnect = 0;
        disconnectsInARow = 0;
        notifyMasterFound();
        if (!address.equals(getAddress()) && channelFuture != null) {
            Log.i(TAG, "Found new address, closing old connection " + channelFuture.channel());
            channelFuture.channel().close(); //close the current connection if a new address was found
        }
        initClient();
    }

    //Internal Getters//////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Stop listening, close all connections and shut down the executors.
     */
    public void destroy() {
        Log.d(TAG, "stopClient");
        isActive = false;
        if (channelFuture != null && channelFuture.channel() != null) {
            channelFuture.channel().close();
        }
        requireComponent(ContainerService.KEY_CONTEXT).unregisterReceiver(broadcastReceiver);
        super.destroy();
    }

    //Public Getters////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Channel for the {@link IncomingDispatcher} and the {@link ClientOutgoingRouter}
     */
    @Nullable
    Channel getChannel() {
        return channelFuture != null ? channelFuture.channel() : null;
    }

    /**
     * @return the local Address this client is listening on
     */
    @Nullable
    public InetSocketAddress getAddress() {
        if (channelFuture == null || channelFuture.channel() == null) {
            return null;
        } else {
            return (InetSocketAddress) channelFuture.channel().localAddress();
        }
    }

    /**
     * @return {@code true}, if the Client TCP channel is currently open
     */
    public boolean isChannelOpen() {
        return channelFuture != null && channelFuture.channel() != null && channelFuture.channel().isOpen();
    }

    /**
     * @return {@code true}, if the Client TCP channel is currently open and the handshake and authentication were successful
     */
    public boolean isConnectionEstablished() {
        return isChannelOpen() && channelFuture.channel().attr(ATTR_HANDSHAKE_FINISHED).get() == Boolean.TRUE;
    }

    /**
     * @return {@code true}, if the Client is connected to the Master via a local, home network
     */
    public boolean isConnectionLocal() {
        return isConnectionEstablished() && channelFuture.channel().attr(ATTR_LOCAL_CONNECTION).get() == Boolean.TRUE;
    }

    //Shared Preferences////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Blocks until the Client has been completely shut down.
     *
     * @throws InterruptedException
     */
    public void awaitShutdown() throws InterruptedException {
        if (channelFuture != null && channelFuture.channel() != null) {
            channelFuture.channel().closeFuture().await();
        }
    }

    private SharedPreferences getSharedPrefs() {
        return requireComponent(ContainerService.KEY_CONTEXT).getSharedPreferences();
    }

    /**
     * @return the token sent to the Master for active registration
     * @see de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket.ActiveRegistrationRequest
     */
    public String getActiveRegistrationToken() {
        return getSharedPrefs().getString(PREF_TOKEN_ACTIVE, null);
    }

    /**
     * @return the token sent to the Master for active registration
     * @see de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket.ActiveRegistrationRequest
     */
    public byte[] getActiveRegistrationTokenBytes() {
        return DeviceConnectInformation.decodeToken(getActiveRegistrationToken());
    }

    /**
     * @return the token expected from the Master for passive registration
     * @see de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket.ServerAuthenticationResponse
     */
    @Nullable
    public String getPassiveRegistrationToken() {
        return getSharedPrefs().getString(PREF_TOKEN_PASSIVE, null);
    }

    /**
     * @return the token expected from the Master for passive registration
     * @see de.unipassau.isl.evs.ssh.core.network.handshake.HandshakePacket.ServerAuthenticationResponse
     */
    public byte[] getPassiveRegistrationTokenBytes() {
        return DeviceConnectInformation.decodeToken(getPassiveRegistrationToken());
    }

    /**
     * @return the Address the Client will trz to connect to
     */
    @Nullable
    public InetSocketAddress getConnectAddress() {
        InetSocketAddress address = getLastAddress();
        if (address == null) {
            address = getConfiguredAddress();
        }
        return address;
    }

    /**
     * @return the address the last successful connection was established to
     */
    @Nullable
    public InetSocketAddress getLastAddress() {
        return getPrefsAddress(LAST_HOST, LAST_PORT);
    }

    /**
     * @return the master address that was configured by the user
     */
    @Nullable
    public InetSocketAddress getConfiguredAddress() {
        return getPrefsAddress(PREF_HOST, PREF_PORT);
    }

    @Nullable
    private InetSocketAddress getPrefsAddress(String prefsHost, String prefsPort) {
        final SharedPreferences prefs = getSharedPrefs();
        final String host = prefs.getString(prefsHost, null);
        final int port = prefs.getInt(prefsPort, -1);
        if (host == null || port < 0) {
            return null;
        } else {
            return InetSocketAddress.createUnresolved(host, port);
        }
    }

    /**
     * @return an editor for modifying all preferences related to the client
     */
    public PrefEditor editPrefs() {
        return new PrefEditor(getSharedPrefs().edit());
    }

    public static class PrefEditor {
        private final SharedPreferences.Editor editor;

        public PrefEditor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        public PrefEditor setActiveRegistrationToken(@Nullable String token) {
            editor.putString(PREF_TOKEN_ACTIVE, token);
            return this;
        }

        public PrefEditor setActiveRegistrationToken(@Nullable byte[] token) {
            if (token == null) {
                return setActiveRegistrationToken((String) null);
            } else {
                return setActiveRegistrationToken(DeviceConnectInformation.encodeToken(token));
            }
        }

        public PrefEditor setPassiveRegistrationToken(@Nullable String token) {
            editor.putString(PREF_TOKEN_PASSIVE, token);
            return this;
        }

        public PrefEditor setPassiveRegistrationToken(@Nullable byte[] token) {
            if (token == null) {
                return setActiveRegistrationToken((String) null);
            } else {
                return setPassiveRegistrationToken(DeviceConnectInformation.encodeToken(token));
            }
        }

        public PrefEditor setLastAddress(@Nullable InetSocketAddress address) {
            setPrefsAddress(address, LAST_HOST, LAST_PORT);
            return this;
        }

        public PrefEditor setConfiguredAddress(@Nullable InetSocketAddress address) {
            setPrefsAddress(address, PREF_HOST, PREF_PORT);
            return this;
        }

        private void setPrefsAddress(@Nullable InetSocketAddress address, String prefHost, String prefPort) {
            if (address == null) {
                editor.remove(prefPort);
                editor.remove(prefHost);
            } else {
                editor.putInt(prefPort, address.getPort());
                editor.putString(prefHost, address.getHostString());
            }
        }

        public boolean commit() {
            return editor.commit();
        }

        public void apply() {
            editor.apply();
        }
    }

    //Listeners/////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void addListener(ClientConnectionListener object) {
        listeners.add(object);
    }

    public void removeListener(ClientConnectionListener object) {
        listeners.remove(object);
    }

    private void notifyMasterFound() {
        for (ClientConnectionListener listener : listeners) {
            listener.onMasterFound();
        }
    }

    private void notifyClientConnecting(String host, int port) {
        for (ClientConnectionListener listener : listeners) {
            listener.onClientConnecting(host, port);
        }
    }

    void notifyClientConnected() {
        for (ClientConnectionListener listener : listeners) {
            listener.onClientConnected();
        }
    }

    private void notifyClientDisconnected() {
        for (ClientConnectionListener listener : listeners) {
            listener.onClientDisconnected();
        }
    }

    void notifyClientRejected(@Nullable String message) {
        for (ClientConnectionListener listener : listeners) {
            listener.onClientRejected(message);
        }
    }
}
