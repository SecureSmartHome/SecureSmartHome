package de.unipassau.isl.evs.ssh.core.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

/**
 * A BoundActivity that provides utilities for switching between Activities if one or more of them are currently not
 * available.
 *
 * @author Niko Fink
 */
@SuppressLint("Registered")
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

    /**
     * @return {@code true}, if the current Activity is currently finishing and/or being replaced by another Activity.
     */
    protected boolean isSwitching() {
        return isFinishing() || switching;
    }

    /**
     * Finish this activity as soon as it has been fully started.
     */
    protected void finishLater() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
        switching = true;
    }

    /**
     * Switch to the given StartUpActivity due to the given reason String.
     *
     * @param toClass the class of the StartUpActivity to switch to
     * @param reason  the reason for switching, only used for logging
     * @return {@code true}, if the Activity will be switched. {@code false}, if the current Activity stays open
     */
    protected boolean doSwitch(final Class<? extends StartUpActivity> toClass, String reason) {
        return doSwitch(toClass, reason, new Runnable() {
            public void run() {
                startActivity(new Intent(StartUpActivity.this, toClass));
            }
        });
    }

    /**
     * Switch to the given StartUpActivity due to the given reason String by executing the given runnable.
     *
     * @param toClass        the class of the StartUpActivity to switch to
     * @param reason         the reason for switching, only used for logging
     * @param switchRunnable the runnable that should be called to switch the Activity
     * @return {@code true}, if the Activity will be switched. {@code false}, if the current Activity stays open
     */
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

    /**
     * Check if a switch to another Activity is required and call doSwitch if that is the case.
     * Returns {@code true} if the Activity will be switched.s
     */
    protected abstract boolean checkSwitchActivity();
}
