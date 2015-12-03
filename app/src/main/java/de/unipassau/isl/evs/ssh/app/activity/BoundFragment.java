package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;

/**
 * A Fragment which can be attached to a {@link BoundActivity} and has utitlity methods for accessing the
 * Container(Service) the Activity is bound to.
 *
 * @author Niko Fink
 */
public class BoundFragment extends Fragment {
    /**
     * {@inheritDoc}
     * <p/>
     * Also redelivers {@link #onContainerConnected(Container)} if the Activity is already connected to the container.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
    public void onDetach() {
        onContainerDisconnected();
        super.onDetach();
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
     * Returns the Container of the BoundActivity this Fragment is attached to, which must itself be bound to a ContainerService.
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
}
