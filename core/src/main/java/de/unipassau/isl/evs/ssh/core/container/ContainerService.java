package de.unipassau.isl.evs.ssh.core.container;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;

import java.util.Collection;

import de.ncoder.typedmap.Key;
import de.ncoder.typedmap.TypedMap;

public class ContainerService extends Service implements Container {
    public static final Key<ContextComponent> KEY_CONTEXT = new Key<>(ContextComponent.class, "ContainerContext");
    private final Container container = new SimpleContainer();
    private final Binder theBinder = new Binder();

    @Override
    public void onCreate() {
        super.onCreate();
        container.register(KEY_CONTEXT, new ContextComponent(this));
    }

    @Override
    public void onDestroy() {
        container.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return theBinder;
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

    @Override
    public <T extends Component> T require(Key<T> key) {
        return container.require(key);
    }

    @Override
    public boolean isRegistered(Key<?> key) {
        return container.isRegistered(key);
    }

    @Override
    public TypedMap<? extends Component> getData() {
        return container.getData();
    }

    @Override
    public void shutdown() {
        container.shutdown();
    }

    public static class ContextComponent extends ContextWrapper implements Component {
        public ContextComponent(Context base) {
            super(base);
        }

        @Override
        public void init(Container container) {
        }

        @Override
        public void destroy() {
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    public class Binder extends android.os.Binder implements Container {
        public <T extends Component, V extends T> void register(Key<T> key, V component) {
            container.register(key, component);
        }

        public <T extends Component> T get(Key<T> key) {
            return container.get(key);
        }

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

        public TypedMap<? extends Component> getData() {
            return container.getData();
        }
    }
}