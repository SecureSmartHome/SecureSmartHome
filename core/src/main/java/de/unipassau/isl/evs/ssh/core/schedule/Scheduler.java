package de.unipassau.isl.evs.ssh.core.schedule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.Serializable;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

/**
 * Scheduler is a component that allows periodic or delayed execution of tasks.
 * The scheduler is based on Android AlarmManager class.
 * This class allows to schedule Callables and Runnables to be executed later.
 */
public class Scheduler extends AbstractComponent {
    public static final Key<Scheduler> KEY = new Key<>(Scheduler.class);

    public static final String ACTION_BROADCAST = ContainerService.class.getName() + ".Broadcast";
    public static final String EXTRA_BROADCAST_TARGET = "EXTRA_BROADCAST_TARGET";

    public void forwardIntent(Intent intent) {
        if (ACTION_BROADCAST.equals(intent.getAction())) {
            Serializable key = intent.getSerializableExtra(EXTRA_BROADCAST_TARGET);
            if (key instanceof Key) {
                Component target = getComponent(((Key) key));
                if (target instanceof ScheduledComponent) {
                    ((ScheduledComponent) target).onReceive(intent);
                }
            }
        }
    }

    public Intent getScheduleIntent(Key<?> to) {
        Intent intent = getContext().getStartIntent();
        if (intent == null) {
            throw new IllegalStateException("can't send Intents when not registered to a ContainerService");
        }
        intent.setAction(ACTION_BROADCAST);
        intent.putExtra(EXTRA_BROADCAST_TARGET, to);
        return intent;
    }

    public PendingIntent getSchedulePendingIntent(Key<?> to, Bundle extras, int flags) {
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

    public void set(int type, long triggerAtMillis, PendingIntent operation) {
        getAlarmManager().set(type, triggerAtMillis, operation);
    }

    public void setRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation) {
        getAlarmManager().setRepeating(type, triggerAtMillis, intervalMillis, operation);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, PendingIntent operation) {
        getAlarmManager().setWindow(type, windowStartMillis, windowLengthMillis, operation);
    }

    public void setExact(int type, long triggerAtMillis, PendingIntent operation) {
        getAlarmManager().setExact(type, triggerAtMillis, operation);
    }

    public void setInexactRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation) {
        getAlarmManager().setInexactRepeating(type, triggerAtMillis, intervalMillis, operation);
    }

    public void cancel(PendingIntent operation) {
        getAlarmManager().cancel(operation);
    }
}