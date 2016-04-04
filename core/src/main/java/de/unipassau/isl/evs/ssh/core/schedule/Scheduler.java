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

package de.unipassau.isl.evs.ssh.core.schedule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.Serializable;
import java.util.Objects;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

/**
 * Scheduler is a component that allows periodic or delayed execution of tasks, similar to the Android {@link AlarmManager} class.
 * <p/>
 * To receive alarms, let the Component that should receive the alarms extend the {@link ScheduledComponent} interface.
 * Afterwards, generate a PendingIntent for your Component using {@link #getPendingScheduleIntent(Key, Bundle, int)} and register
 * it to the AlarmManager using one of the {@link #set(int, long, PendingIntent)} methods.
 * <p/>
 * <i>Note that this Class only works when the Container is managed by a {@link ContainerService}, as the generated Intents
 * are targeted at that Service, which also forwards the received Intents from
 * {@link ContainerService#onStartCommand(Intent, int, int)} to {@link #forwardIntent(Intent)}.</i>
 * <p/>
 * Example Usage:
 * <pre>
 * bindService(getContext(), ContainerService.class); //container must be managed by a ContainerService
 * //...
 * container.register(MyComponent.KEY, new MyComponent()); //MyComponent must extend ScheduledComponent
 * //...
 * Scheduler scheduler = container.get(Scheduler.KEY);
 * Bundle extras = new Bundle();
 * extras.putString("data", "hello world");
 * PendingIntent pendingIntent = scheduler.getPendingScheduleIntent(key, extras, PendingIntent.FLAG_CANCEL_CURRENT);
 *
 * scheduler.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
 *      SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(5),
 *      pendingIntent);
 * </pre>
 *
 * @author Niko Fink
 * @see AlarmManager
 * @see ContainerService
 */
public class Scheduler extends AbstractComponent {
    public static final Key<Scheduler> KEY = new Key<>(Scheduler.class);

    private static final String ACTION_BROADCAST = ContainerService.class.getName() + ".Broadcast";
    private static final String EXTRA_BROADCAST_TARGET = "EXTRA_BROADCAST_TARGET";

    /**
     * Forward the received Intent to the targeted {@link ScheduledComponent}.
     * Should only be called from {@link ContainerService#onStartCommand(Intent, int, int)}.
     *
     * @param intent the Intent received by {@link ContainerService#onStartCommand(Intent, int, int)}
     */
    public void forwardIntent(Intent intent) {
        if (Objects.equals(ACTION_BROADCAST, intent.getAction())) {
            Serializable key = intent.getSerializableExtra(EXTRA_BROADCAST_TARGET);
            if (key instanceof Key) {
                Component target = getComponent(((Key) key));
                if (target instanceof ScheduledComponent) {
                    ((ScheduledComponent) target).onReceive(intent);
                }
            }
        }
    }

    /**
     * Generate a new generic Intent for sending an alarm to the Component added for the given key.
     *
     * @param to the Key of the Component that should receive the alarm
     * @return the Intent that can be wrapped in a {@link PendingIntent} and scheduled using the {@link AlarmManager}
     * @see #getPendingScheduleIntent(Key, Bundle, int) getPendingScheduleIntent can be used for wrapping this Intent in a PendingIntent
     */
    public Intent getScheduleIntent(Key<? extends ScheduledComponent> to) {
        Intent intent = getContext().getStartIntent();
        if (intent == null) {
            throw new IllegalStateException("can't send Intents when not registered to a ContainerService");
        }
        intent.setAction(ACTION_BROADCAST);
        intent.putExtra(EXTRA_BROADCAST_TARGET, to);
        return intent;
    }

    /**
     * Generate a new PendingIntent that can be used for sending an alarm to the Component added for the given key.
     *
     * @param to     the Key of the Component that should receive the alarm
     * @param extras the Extras that should be added to the Intent and thereby be delivered to the receiving {@link ScheduledComponent}.
     *               See {@link Intent#putExtras(Bundle)}.
     * @param flags  the flags passed to {@link PendingIntent#getService(Context, int, Intent, int)}.
     *               May be {@link PendingIntent#FLAG_ONE_SHOT}, {@link PendingIntent#FLAG_NO_CREATE},
     *               {@link PendingIntent#FLAG_CANCEL_CURRENT}, {@link PendingIntent#FLAG_UPDATE_CURRENT},
     *               or any of the flags as supported by
     *               {@link Intent#fillIn Intent.fillIn()} to control which unspecified parts
     *               of the intent that can be supplied when the actual send happens.
     * @return the Intent that can be scheduled using the {@link AlarmManager} or one of the {@link #set(int, long, PendingIntent)} methods
     * @see #set(int, long, PendingIntent)
     */
    public PendingIntent getPendingScheduleIntent(Key<? extends ScheduledComponent> to, Bundle extras, int flags) {
        Intent intent = getScheduleIntent(to);
        if (extras != null) {
            intent.putExtras(extras);
        }
        return PendingIntent.getService(getContext(), 0, intent, flags);
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) (getContext().getSystemService(Context.ALARM_SERVICE));
    }

    private ContainerService.ContextComponent getContext() {
        return requireComponent(ContainerService.KEY_CONTEXT);
    }

    /**
     * Shorthand method delegating to the Android System {@link AlarmManager}.
     *
     * @see AlarmManager#set(int, long, PendingIntent)
     */
    public void set(int type, long triggerAtMillis, PendingIntent operation) {
        getAlarmManager().set(type, triggerAtMillis, operation);
    }

    /**
     * Shorthand method delegating to the Android System {@link AlarmManager}.
     *
     * @see AlarmManager#setRepeating(int, long, long, PendingIntent)
     */
    public void setRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation) {
        getAlarmManager().setRepeating(type, triggerAtMillis, intervalMillis, operation);
    }

    /**
     * Shorthand method delegating to the Android System {@link AlarmManager}.
     *
     * @see AlarmManager#setWindow(int, long, long, PendingIntent)
     */
    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, PendingIntent operation) {
        getAlarmManager().setWindow(type, windowStartMillis, windowLengthMillis, operation);
    }

    /**
     * Shorthand method delegating to the Android System {@link AlarmManager}.
     *
     * @see AlarmManager#setExact(int, long, PendingIntent)
     */
    public void setExact(int type, long triggerAtMillis, PendingIntent operation) {
        getAlarmManager().setExact(type, triggerAtMillis, operation);
    }

    /**
     * Shorthand method delegating to the Android System {@link AlarmManager}.
     *
     * @see AlarmManager#setInexactRepeating(int, long, long, PendingIntent)
     */
    public void setInexactRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation) {
        getAlarmManager().setInexactRepeating(type, triggerAtMillis, intervalMillis, operation);
    }

    /**
     * Shorthand method delegating to the Android System {@link AlarmManager}.
     *
     * @see AlarmManager#cancel(PendingIntent)
     */
    public void cancel(PendingIntent operation) {
        getAlarmManager().cancel(operation);
    }
}