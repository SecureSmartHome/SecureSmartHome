package de.unipassau.isl.evs.ssh.core.network;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

import static android.content.Context.MODE_PRIVATE;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.FILE_SHARED_PREFS;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_HANDSHAKE_FINISHED;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.DEFAULT_LOCAL_PORT;

/**
 * A netty stack accepting connections to and from the master and handling communication with them using a netty pipeline.
 * For details about the pipeline, see {@link ClientHandshakeHandler}.
 * For details about switching to UDP discovery, see {@link #initClient()} and {@link #shouldReconnectTCP()}.
 * This component is used by the Slave and the end-user android App.
 *
 * @author Phil Werli
 */
public class Client extends AbstractComponent {
    public static final Key<Client> KEY = new Key<>(Client.class);

    /**
     * The minimum number of seconds between
     */
    private static final long CLIENT_MILLIS_BETWEEN_DISCONNECTS = TimeUnit.SECONDS.toMillis(10);
    /**
     * Default value for maximum timeouts.
     */
    private static final int CLIENT_MAX_DISCONNECTS = 3;

    private static final String TAG = Client.class.getSimpleName();
    static final String PREF_TOKEN_ACTIVE = Client.class.getName() + ".PREF_TOKEN_ACTIVE";
    static final String PREF_TOKEN_PASSIVE = Client.class.getName() + ".PREF_TOKEN_PASSIVE";
    static final String PREF_HOST = Client.class.getName() + ".PREF_HOST";
    static final String PREF_PORT = Client.class.getName() + ".PREF_PORT";

    /**
     * Receives messages from system components and decides how to route them to the targets.
     */
    private final ClientOutgoingRouter outgoingRouter = new ClientOutgoingRouter();
    /**
     * Boolean if the client connection is active.
     */
    private boolean isActive;
    /**
     * The EventLoopGroup used for accepting connections
     */
    private EventLoopGroup executor;
    /**
     * The channel listening for incoming TCP connections on the port of the client.
     * Use {@link ChannelFuture#sync()} to wait for client startup.
     */
    private ChannelFuture channelFuture;
    /**
     * Distributes incoming messages to the responsible handlers.
     */
    private final ClientIncomingDispatcher incomingDispatcher = new ClientIncomingDispatcher();
    /**
     * Send UDP Broadcasts when the Server can't be reached
     */
    private final UDPDiscoveryClient udpDiscovery = new UDPDiscoveryClient();
    /**
     * Int used to calculate the time between the last and the current timeout.
     */
    private long lastDisconnect = 0;
    /**
     * Int that saves how many timeouts happened in a row.
     */
    private int disconnectsInARow = 0;

    /**
     * Configure netty and add related Components to the Container.
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
        // Add related components
        container.register(IncomingDispatcher.KEY, incomingDispatcher);
        container.register(OutgoingRouter.KEY, outgoingRouter);
        container.register(UDPDiscoveryClient.KEY, udpDiscovery);
        // And try to connect
        isActive = true;
        initClient();
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
        if (isChannelOpen() && isExecutorAlive()) {
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
        getAliveExecutor().submit(new Runnable() {
            @Override
            public void run() {
                // Read the previous host and port from the shared preferences
                final String host = getHost();
                final int port = getPort();

                // Connect to TCP if the address of the Server/Master is known and not too many connection attempts have failed
                //TODO: uncomment with different behaviour for slave and app.
                //TODO would like to uncomment, any details about the problem? (Niko, 2015-12-20)
                //final NamingManager namingManager = requireComponent(NamingManager.KEY);
                //if (!namingManager.isMasterIDKnown()) {
                //    Log.w(TAG, "MasterID is null, waiting for onMasterFound(host, port)");
                //} else
                if (host != null && shouldReconnectTCP()) {
                    connectClient(host, port);
                } else {
                    if (host == null) {
                        Log.w(TAG, "No master known, starting UDP discovery");
                    } else {
                        Log.w(TAG, "Too many disconnects from " + host + ":" + port + ", trying UDP discovery");
                    }
                    requireComponent(UDPDiscoveryClient.KEY).startDiscovery();
                }
            }
        });
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
    protected void connectClient(String host, int port) {
        Log.i(TAG, "Client connecting to " + host + ":" + port);
        notifyClientConnecting();

        // TCP Connection
        Bootstrap b = new Bootstrap()
                .group(executor)
                .channel(NioSocketChannel.class)
                .handler(getHandshakeHandler())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(5));

        // Wait for the start of the client
        channelFuture = b.connect(host, port);
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
     * HandshakeHandle can be changed or mocked for testing
     *
     * @return the ClientHandshakeHandler to use
     */
    @NonNull
    protected ChannelHandler getHandshakeHandler() {
        return new ClientHandshakeHandler(this, getContainer());
    }

    /**
     * Called once the TCP connection is established.
     *
     * @see ClientHandshakeHandler#channelActive(ChannelHandlerContext) triggers the Handshake after this method is complete
     */
    protected void channelOpen(Channel channel) {
        requireComponent(UDPDiscoveryClient.KEY).stopDiscovery();
    }

    /**
     * Called once the TCP connection is closed or if it couldn't be established at all.
     * Increments the disconnect counter and tries to re-establish the connection.
     */
    protected synchronized void channelClosed(Channel channel) {
        if (channel != this.channelFuture.channel()) {
            return; //channel has already been exchanged by new one, don't start another client
        }
        notifyClientDisconnected();
        if (isActive && isExecutorAlive() && !executor.isShuttingDown()) {
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
     * Saves the new address
     */
    public void onMasterFound(InetAddress address, int port) {
        onMasterFound(address, port, null);
    }

    public void onMasterFound(InetAddress address, int port, String token) {
        Log.i(TAG, "discovery successful, found " + address + ":" + port + " with token " + token);
        editPrefs()
                .setHost(address.getHostAddress())
                .setPort(port)
                .setActiveRegistrationToken(token)
                .commit();
        lastDisconnect = 0;
        disconnectsInARow = 0;
        notifyMasterFound();
        initClient();
    }

    /**
     * Stop listening, close all connections and shut down the executors.
     */
    public void destroy() {
        Log.d(TAG, "stopClient");
        isActive = false;
        if (channelFuture != null && channelFuture.channel() != null) {
            channelFuture.channel().close();
        }
        if (executor != null) {
            executor.shutdownGracefully();
        }
        getContainer().unregister(udpDiscovery);
        getContainer().unregister(outgoingRouter);
        getContainer().unregister(incomingDispatcher);
        super.destroy();
    }

    //Internal Getters//////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * EventLoopGroup for the {@link ClientIncomingDispatcher} and the {@link ClientOutgoingRouter}
     */
    @Nullable
    EventLoopGroup getExecutor() {
        return executor;
    }

    /**
     * EventLoopGroup for the {@link ClientIncomingDispatcher} and the {@link ClientOutgoingRouter}
     */
    @NonNull
    EventLoopGroup getAliveExecutor() {
        if (!isExecutorAlive()) {
            // Setup the Executor and Connection Pool
            executor = new NioEventLoopGroup(0, new DefaultExecutorServiceFactory("client"));
        }
        return executor;
    }

    /**
     * Channel for the {@link ClientIncomingDispatcher} and the {@link ClientOutgoingRouter}
     */
    @Nullable
    Channel getChannel() {
        return channelFuture != null ? channelFuture.channel() : null;
    }

    /**
     * {@link IncomingDispatcher} that will be registered to the Pipeline by the {@link ClientHandshakeHandler}
     */
    @NonNull
    ClientIncomingDispatcher getIncomingDispatcher() {
        return incomingDispatcher;
    }

    //Public Getters////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return the local Address this client is listening on
     */
    @Nullable
    public SocketAddress getAddress() {
        if (channelFuture == null || channelFuture.channel() == null) {
            return null;
        } else {
            return channelFuture.channel().localAddress();
        }
    }

    /**
     * @return {@code true}, if the Client TCP channel is currently open
     */
    public boolean isChannelOpen() {
        return channelFuture != null && channelFuture.channel() != null && channelFuture.channel().isOpen();
    }

    /**
     * @return {@code true}, if the Executor that is used for processing data has not been shut down
     */
    public boolean isExecutorAlive() {
        return executor != null && !executor.isTerminated() && !executor.isShutdown();
    }

    /**
     * @return {@code true}, if the Client TCP channel is currently open and the handshake and authentication were successful
     */
    public boolean isConnectionEstablished() {
        return isChannelOpen() && channelFuture.channel().attr(ATTR_HANDSHAKE_FINISHED).get() == Boolean.TRUE;
    }

    //Shared Preferences////////////////////////////////////////////////////////////////////////////////////////////////

    private SharedPreferences getSharedPrefs() {
        return requireComponent(ContainerService.KEY_CONTEXT).getSharedPreferences(FILE_SHARED_PREFS, MODE_PRIVATE);
    }

    public String getActiveRegistrationToken() {
        return getSharedPrefs().getString(PREF_TOKEN_ACTIVE, null);
    }

    public byte[] getActiveRegistrationTokenBytes() {
        return DeviceConnectInformation.decodeToken(getActiveRegistrationToken());
    }

    public String getPassiveRegistrationToken() {
        return getSharedPrefs().getString(PREF_TOKEN_PASSIVE, null);
    }

    public byte[] getPassiveRegistrationTokenBytes() {
        return DeviceConnectInformation.decodeToken(getPassiveRegistrationToken());
    }

    public int getPort() {
        return getSharedPrefs().getInt(PREF_PORT, DEFAULT_LOCAL_PORT);
    }

    public String getHost() {
        return getSharedPrefs().getString(PREF_HOST, null);
    }

    public PrefEditor editPrefs() {
        return new PrefEditor(getSharedPrefs().edit());
    }

    public static class PrefEditor {
        private final SharedPreferences.Editor editor;

        public PrefEditor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        public PrefEditor setActiveRegistrationToken(String token) {
            editor.putString(PREF_TOKEN_ACTIVE, token);
            return this;
        }

        public PrefEditor setActiveRegistrationToken(byte[] token) {
            return setActiveRegistrationToken(DeviceConnectInformation.encodeToken(token));
        }

        public PrefEditor setPassiveRegistrationToken(String token) {
            editor.putString(PREF_TOKEN_PASSIVE, token);
            return this;
        }

        public PrefEditor setPassiveRegistrationToken(byte[] token) {
            return setPassiveRegistrationToken(DeviceConnectInformation.encodeToken(token));
        }

        public PrefEditor setPort(int port) {
            editor.putInt(PREF_PORT, port);
            return this;
        }

        public PrefEditor setHost(String host) {
            editor.putString(PREF_HOST, host);
            return this;
        }

        public boolean commit() {
            return editor.commit();
        }

        public void apply() {
            editor.apply();
        }
    }

    //Listeners/////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final List<ClientConnectionListener> listeners = new ArrayList<>();

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

    private void notifyClientConnecting() {
        for (ClientConnectionListener listener : listeners) {
            listener.onClientConnecting();
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

    void notifyClientRejected(String message) {
        for (ClientConnectionListener listener : listeners) {
            listener.onClientRejected(message);
        }
    }
}
