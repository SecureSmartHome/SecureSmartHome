package de.unipassau.isl.evs.ssh.core.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

/**
 * TODO Niko add Javadoc for whole class. (Phil, 2016-01-09)
 *
 * @author Niko Fink
 */
public abstract class StartUpActivity extends BoundActivity {
    protected final String TAG = getClass().getSimpleName();
    private boolean switching = false;

    public StartUpActivity(Class<? extends ContainerService> serviceClass, boolean bindOnStart) {super(serviceClass, bindOnStart);}

    public StartUpActivity(Class<? extends ContainerService> serviceClass) {super(serviceClass);}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkSwitchActivity();
    }

    @Override
    public void onContainerConnected(Container container) {
        checkSwitchActivity();
    }

    protected boolean isSwitching() {
        return isFinishing() || switching;
    }

    protected void finishLater() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
        switching = true;
    }

    protected boolean doSwitch(final Class<? extends StartUpActivity> toClass, String reason) {
        return doSwitch(toClass, reason, new Runnable() {
            public void run() {
                startActivity(new Intent(StartUpActivity.this, toClass));
            }
        });
    }

    protected boolean doSwitch(final Class<? extends StartUpActivity> toClass, String reason, Runnable switchRunnable) {
        if (getClass().equals(toClass)) {
            Log.v(TAG, "Staying in " + toClass.getSimpleName() + " as " + reason);
            return false;
        } else {
            switchRunnable.run();
            finishLater();
            Log.i(TAG, "Switching to " + toClass.getSimpleName() + " as " + reason);
            return true;
        }
    }

    protected abstract boolean checkSwitchActivity();
}
