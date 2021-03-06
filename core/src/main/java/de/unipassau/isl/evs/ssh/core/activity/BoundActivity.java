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

package de.unipassau.isl.evs.ssh.core.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
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
    private Intent intent;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        this.intent = new Intent(this, this.serviceClass);
        super.onCreate(savedInstanceState);
    }

    /**
     * Calls {@link #startService(Intent)} and {@link #bindService(Intent, ServiceConnection, int)} in order
     * to connect to the {@link ContainerService}.
     */
    protected void doBind() {
        startService(intent);
        if (!serviceBound) {
            Log.v(TAG, "doBind not bound, binding");
            serviceBound = bindService(intent, serviceConn, BIND_AUTO_CREATE);
            Log.v(TAG, "doBind binding " + (serviceBound ? "successful" : "failed"));
        } else {
            Log.v(TAG, "doBind already bound");
        }
    }

    /**
     * Unbinds the Connection to the ContainerService.
     */
    protected void doUnbind() {
        if (serviceBound) {
            Log.v(TAG, "doUnbind bound, unbinding");
            unbindService(serviceConn);
            serviceBound = false;
        } else {
            Log.v(TAG, "doUnbind not bound, unbinding unnecessary");
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

    /**
     * Disconnects from the bound Service and forces it to stop.
     */
    protected void forceStopService() {
        doUnbind();
        stopService(intent);
    }

    /**
     * Forces the bound service to restart and rebinds.
     */
    protected void forceRestartService() {
        forceStopService();
        doBind();
    }
}
