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

package de.unipassau.isl.evs.ssh.app.activity;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

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

    /**
     * Run the given runnable on the UI Thread, but only if the fragment is still attached and the Activity is available.
     * @return {@code true} if the fragment was still attached and the Runnable will be run
     */
    protected boolean maybeRunOnUiThread(Runnable runnable) {
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(runnable);
            return true;
        }
        return false;
    }

    /**
     * Shows {@link Toast} with given String resource.
     *
     * @param resId Resources ID of the given String resource.
     */
    protected void showToast(int resId) {
        Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
    }
}
