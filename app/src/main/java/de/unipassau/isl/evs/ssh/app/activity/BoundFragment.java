package de.unipassau.isl.evs.ssh.app.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;

/**
 * @author Niko Fink
 */
public class BoundFragment extends Fragment {
    public void onContainerConnected(Container container) {
    }

    public void onContainerDisconnected() {
    }

    public Container getContainer() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof BoundActivity) {
            return ((BoundActivity) activity).getContainer();
        } else {
            return null;
        }
    }

    protected <T extends Component> T getComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.get(key);
        } else {
            return null;
        }
    }
}
