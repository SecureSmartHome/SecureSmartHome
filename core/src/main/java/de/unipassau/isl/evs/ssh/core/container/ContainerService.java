package de.unipassau.isl.evs.ssh.core.container;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;
import de.unipassau.isl.evs.ssh.core.schedule.Scheduler;

/**
 * An Android {@link Service} that manages a {@link SimpleContainer} and its {@link Component}s.
 * Android Activity can bind to this Service and communicate with the {@link Container}.
 *
 * @author Niko
 */
public class ContainerService extends Service implements Container {
    public static final Key<ContextComponent> KEY_CONTEXT = new Key<>(ContextComponent.class, "ContainerContext");
    private static final String TAG = ContainerService.class.getSimpleName();
    private final Container container = new SimpleContainer();
    private final Binder theBinder = new Binder();

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate:called");
        super.onCreate();
        container.register(KEY_CONTEXT, new ContextComponent(this, getStartIntent()));
        container.register(Scheduler.KEY, new Scheduler());
        init();
        Log.d(TAG, "onCreate:finished");
    }

    protected void init() {
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy:called");
        container.shutdown();
        super.onDestroy();
        Log.d(TAG, "onDestroy:finished");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return theBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Scheduler scheduler = get(Scheduler.KEY);
        if (scheduler != null && intent != null) {
            scheduler.forwardIntent(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public Container getContainer() {
        return container;
    }

    @Override
    public <T extends Component, V extends T> void register(Key<T> key, V component) {
        container.register(key, component);
    }

    @Override
    public void unregister(Key<?> key) {
        container.unregister(key);
    }

    @Override
    public void unregister(Component component) {
        container.unregister(component);
    }

    @Override
    public <T extends Component> T get(Key<T> key) {
        return container.get(key);
    }

    @NonNull
    @Override
    public <T extends Component> T require(Key<T> key) {
        return container.require(key);
    }

    @Override
    public boolean isRegistered(Key<?> key) {
        return container.isRegistered(key);
    }

    @NonNull
    @Override
    public TypedMap<? extends Component> getData() {
        return container.getData();
    }

    @Override
    public void shutdown() {
        container.shutdown();
    }

    /**
     * @return an Intent that can be used to address (e.g. start or bind) this ContainerService
     */
    public Intent getStartIntent() {
        return new Intent(this, getClass());
    }

    /**
     * A Component that holds a to the {@link Context} of the ContainerService that manages the Container.
     */
    public static class ContextComponent extends ContextWrapper implements Component {
        private final Intent intent;

        public ContextComponent(Context base) {
            this(base, null);
        }

        public ContextComponent(Context base, Intent intent) {
            super(base);
            this.intent = intent;
        }

        @Override
        public void init(Container container) {
            Log.d(TAG, getClass().getSimpleName() + " initialized");
        }

        @Override
        public void destroy() {
            Log.d(TAG, getClass().getSimpleName() + " destroyed");
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }

        public Intent getStartIntent() {
            if (intent == null) {
                return null;
            } else {
                return new Intent(intent);
            }
        }
    }

    public class Binder extends android.os.Binder implements Container {
        public <T extends Component, V extends T> void register(Key<T> key, V component) {
            container.register(key, component);
        }

        public <T extends Component> T get(Key<T> key) {
            return container.get(key);
        }

        @NonNull
        @Override
        public <T extends Component> T require(Key<T> key) {
            return container.require(key);
        }

        public void unregister(Key<?> key) {
            container.unregister(key);
        }

        @Override
        public void unregister(Component component) {
            container.unregister(component);
        }

        public boolean isRegistered(Key<?> key) {
            return container.isRegistered(key);
        }

        public void shutdown() {
            container.shutdown();
            stopSelf();
        }

        @NonNull
        public TypedMap<? extends Component> getData() {
            return container.getData();
        }

        public Intent getStartIntent() {
            return ContainerService.this.getStartIntent();
        }
    }
}