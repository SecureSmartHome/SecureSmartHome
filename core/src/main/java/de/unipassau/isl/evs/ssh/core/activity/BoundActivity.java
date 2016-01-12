package de.unipassau.isl.evs.ssh.core.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

/**
 * Activity that automatically binds itself to the {@link ContainerService} to enable Activities to access the Container.
 *
 * @author Niko Fink
 */
@SuppressLint("Registered")
public class BoundActivity extends AppCompatActivity {
    private final String TAG = getClass().getSimpleName() + "(BndAct)";

    /**
     * The class of the ContainerService that this Activity should bind to.
     */
    private final Class<? extends ContainerService> serviceClass;
    /**
     * {@code true}, if {@link Context#bindService(Intent, ServiceConnection, int)} has successfully been called and
     * {@link Context#unbindService(ServiceConnection)} hasn't been called yet.
     */
    private boolean serviceBound;
    /**
     * The ContainerService this Activity is bound to.
     */
    private Container serviceContainer;
    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service " + name + " connected: " + service);
            serviceContainer = ((Container) service);
            onContainerConnected(serviceContainer);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service " + name + " disconnected");
            if (serviceContainer instanceof IBinder && !((IBinder) serviceContainer).isBinderAlive()) {
                serviceContainer = null; //can't use container any more if the binder is dead
            }
            onContainerDisconnected();
            serviceContainer = null;
        }
    };
    private final boolean bindOnStart;

    /**
     * Constructor for the BoundActivity
     *
     * @param serviceClass class of the ContainerService to start, used for generating the Intent with {@link Intent#Intent(Context, Class)}
     */
    public BoundActivity(Class<? extends ContainerService> serviceClass) {
        this(serviceClass, true);
    }


    /**
     * Constructor for the BoundActivity
     *
     * @param serviceClass class of the ContainerService to start, used for generating the Intent with {@link Intent#Intent(Context, Class)}
     * @param bindOnStart  whether doBind should be automatically in onStart
     */
    public BoundActivity(Class<? extends ContainerService> serviceClass, boolean bindOnStart) {
        this.serviceClass = serviceClass;
        this.bindOnStart = bindOnStart;
    }

    /**
     * Calls {@link #startService(Intent)} and {@link #bindService(Intent, ServiceConnection, int)} in order
     * to connect to the {@link ContainerService}.
     */
    protected void doBind() {
        Intent intent = new Intent(this, serviceClass);
        startService(intent);
        if (!serviceBound) {
            Log.v(TAG, "onStart not bound, binding");
            serviceBound = bindService(intent, serviceConn, BIND_AUTO_CREATE);
            Log.v(TAG, "onStart binding " + (serviceBound ? "successful" : "failed"));
        } else {
            Log.v(TAG, "onStart already bound");
        }
    }

    /**
     * Unbinds the Connection to the ContainerService.
     */
    protected void doUnbind() {
        if (serviceBound) {
            Log.v(TAG, "onStop bound, unbinding");
            unbindService(serviceConn);
            serviceBound = false;
        } else {
            Log.v(TAG, "onStop not bound, unbinding unnecessary");
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     *
     * @see #doBind()
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (bindOnStart) {
            doBind();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     *
     * @see #doUnbind()
     */
    @Override
    protected void onStop() {
        if (bindOnStart) {
            doUnbind();
        }
        super.onStop();
    }

    /**
     * Overwrite this method if you want to receive a callback as soon as the Container is connected and available,
     * i.e. this BoundActivity is bound to a ContainerService.
     *
     * @param container the Container of the ContainerService this Activity is bound to
     * @see #getContainer()
     * @see ServiceConnection#onServiceConnected(ComponentName, IBinder) called from
     * ServiceConnection.onServiceConnected(ComponentName, IBinder)
     */
    public void onContainerConnected(Container container) {
    }

    /**
     * Overwrite this method if you want to receive a callback as soon as the Container is no longer connected and available,
     * i.e. this BoundActivity is no longer bound to a ContainerService.
     *
     * @see ServiceConnection#onServiceDisconnected(ComponentName) called from
     * ServiceConnection.onServiceDisconnected(ComponentName)
     */
    public void onContainerDisconnected() {
    }

    protected void shutdownService() {
        final Intent intent = new Intent(this, serviceClass);
        stopService(intent);
    }

    /**
     * @return the Container of the ContainerService this Activity is bound to, or {@code null} if this Activity
     * isn't currently bound
     */
    @Nullable
    public Container getContainer() {
        return serviceContainer;
    }

    /**
     * Fetch the Component from the Container or return {@code null} if the Component or the Container itself are not available.
     *
     * @see Container#get(Key)
     */
    @Nullable
    protected <T extends Component> T getComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.get(key);
        } else {
            return null;
        }
    }

    /**
     * Fetch the Component from the Container or throw an {@link IllegalStateException} if the Component or the
     * Container itself are not available.
     *
     * @see Container#require(Key)
     * @deprecated will throw an IllegalStateException and crash the App if the container is not connected
     */
    @NonNull
    @Deprecated
    protected <T extends Component> T requireComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.require(key);
        } else {
            throw new IllegalStateException("Activity not bound to ContainerService");
        }
    }
}
