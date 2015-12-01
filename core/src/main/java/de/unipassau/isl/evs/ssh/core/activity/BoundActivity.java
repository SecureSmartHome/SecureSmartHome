package de.unipassau.isl.evs.ssh.core.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

/**
 * Activity that binds itself to the container service to enable activities to access the container.
 *
 * @author Niko
 */
public class BoundActivity extends AppCompatActivity {
    private static final String TAG = BoundActivity.class.getSimpleName();

    private final Class<? extends ContainerService> serviceClass;
    private boolean serviceBound;
    private Container serviceContainer;
    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceContainer = ((Container) service);
            Log.d(TAG, "Service " + name + " connected: " + service);
            onContainerConnected(serviceContainer);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceContainer = null;
            Log.d(TAG, "Service " + name + " disconnected");
            onContainerDisconnected();
        }
    };

    /**
     * Constructor for the BoundActivity
     *
     * @param serviceClass representing the ContainerService
     */
    public BoundActivity(Class<? extends ContainerService> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Override
    protected void onStart() {
        super.onStart();
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

    @Override
    protected void onStop() {
        if (serviceBound) {
            Log.v(TAG, "onStop bound, unbinding");
            unbindService(serviceConn);
            serviceContainer = null;
            serviceBound = false;
        } else {
            Log.v(TAG, "onStop not bound, unbinding unnecessary");
        }
        super.onStop();
    }

    public void onContainerConnected(Container container) {
    }

    public void onContainerDisconnected() {
    }

    public Container getContainer() {
        return serviceContainer;
    }

    protected <T extends Component> T getComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.get(key);
        } else {
            return null;
        }
    }

    protected <T extends Component> T requireComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.require(key);
        } else {
            throw new IllegalStateException("Activity not bound to ContainerService");
        }
    }
}
