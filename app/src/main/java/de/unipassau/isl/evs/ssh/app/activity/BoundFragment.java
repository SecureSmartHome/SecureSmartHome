package de.unipassau.isl.evs.ssh.app.activity;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * A Fragment which can be attached to a {@link BoundActivity} and has utility methods for accessing the
 * {@link de.unipassau.isl.evs.ssh.core.container.ContainerService Container(Service)} the Activity is bound to.
 *
 * @author Niko Fink
 * @see BoundActivity
 */
public class BoundFragment extends Fragment {
    /**
     * {@inheritDoc}
     * <p/>
     * Also redelivers {@link #onContainerConnected(Container)} if the Activity is already connected to the container.
     */
    @Override
    public void onStart() {
        super.onStart();
        final Container container = getContainer();
        if (container != null) {
            onContainerConnected(container);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Also calls {@link #onContainerDisconnected()} as the Container is no longer available.
     */
    @Override
    public void onStop() {
        final Container container = getContainer();
        if (container != null) {
            onContainerDisconnected();
        }
        super.onStop();
    }

    /**
     * Overwrite this method if you want to receive a callback as soon as the Container is connected and available,
     * i.e. this Fragment is attached to a BoundActivity, which is bound to a ContainerService.
     *
     * @param container the Container the attached Activity is bound to
     * @see BoundActivity#onContainerConnected(Container)
     */
    public void onContainerConnected(Container container) {
    }

    /**
     * Overwrite this method if you want to receive a callback as soon as the Container is no longer available,
     * i.e. because this Fragment is no longer attached to a BoundActivity or the BoundActivity is not bound to
     * a ContainerService.
     *
     * @see BoundActivity#onContainerDisconnected()
     */
    public void onContainerDisconnected() {
    }

    /**
     * Returns the Container of the BoundActivity this Fragment is attached to, which must itself be bound to a ContainerService,
     * or {@code null} if this Fragment is not attached to an Activity or the attached Activity isn't currently bound.
     *
     * @see BoundActivity#getContainer()
     */
    @Nullable
    public Container getContainer() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof BoundActivity) {
            return ((BoundActivity) activity).getContainer();
        } else {
            return null;
        }
    }

    /**
     * Fetch the Component from the Container or return {@code null} if the Component or the Container itself are not available.
     *
     * @see BoundActivity#getComponent(Key)
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
     * @return {@code true} if the container is connected, i.e. {@link #getContainer()} returns a valid container
     */
    public boolean isContainerConnected() {
        return getContainer() != null;
    }

    /**
     * Wraps and returns a Listener that will, as soon as its operationComplete is called, call the operationComplete
     * of the given listener, but on the UI Thread.
     */
    protected <T extends Future<?>> GenericFutureListener<T> listenerOnUiThread(final GenericFutureListener<T> listener) {
        final String tag = BoundFragment.this.getClass().getSimpleName();
        return new GenericFutureListener<T>() {
            @Override
            public void operationComplete(final T future) throws Exception {
                final FragmentActivity activity = getActivity();
                if (activity == null) {
                    Log.i(tag, "Not calling listener " + listener + " of future " + future + " as fragment is no longer attached.");
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                listener.operationComplete(future);
                            } catch (Exception e) {
                                Log.w(tag, "Listener for Future " + future + " threw an Exception", e);
                            }
                        }
                    });
                }
            }
        };
    }

    protected boolean maybeRunOnUiThread(Runnable runnable) {
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(runnable);
            return true;
        }
        return false;
    }
}
