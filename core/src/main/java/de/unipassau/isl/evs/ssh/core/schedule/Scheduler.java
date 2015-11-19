package de.unipassau.isl.evs.ssh.core.schedule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

/**
 * Scheduler is a component that allows periodic or delayed execution of tasks.
 * The scheduler is based on Android AlarmManager class.
 * This class allows to schedule Callables and Runnables to be executed later.
 */
public class Scheduler extends AbstractComponent {
    public static final Key<Scheduler> KEY = new Key<>(Scheduler.class);

    private static final String TAG = Scheduler.class.getSimpleName();
    private static final String INTENT_EXTRA_ID = "alarm-id";
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, AlarmTask> runnables = new ConcurrentHashMap<>();
    private String INTENT_ACTION;
    private AlarmManager alarmManager;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = Integer.parseInt(intent.getData().getSchemeSpecificPart().replaceAll("[^0-9]", ""));
            AlarmTask<?> task = runnables.get(id);
            Log.v(TAG, "Run " + task);
            if (task != null) {
                task.run();
            } else {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_NO_CREATE);
                Log.w(TAG, "Discarding unknown alarm #" + id + " and cancelling source PendingIntent " + pendingIntent);
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }
        }
    };

    @Override
    public void init(Container container) {
        super.init(container);
        Context context = getContext();
        alarmManager = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));

        INTENT_ACTION = context.getPackageName() + ".ScheduleManager.ALARM";
        IntentFilter filter = new IntentFilter(INTENT_ACTION);
        filter.addDataScheme(INTENT_EXTRA_ID);
        context.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void destroy() {
        getContext().unregisterReceiver(broadcastReceiver);
        for (AlarmTask task : runnables.values()) {
            task.cancel(true);
        }
        super.destroy();
    }

    private Context getContext() {
        return requireComponent(ContainerService.KEY_CONTEXT);
    }

    //---------------------------------SCHEDULING FUNCTIONS--------------------

    public <T> Future<T> scheduleRepeatedExecution(Callable<T> callable, long initialDelayMillis, long delayMillis, boolean wakeUp, long executionLatency) {
        return new AlarmTask<>(callable).scheduleRepeating(initialDelayMillis, delayMillis, wakeUp, executionLatency);
    }

    public <T> Future<T> scheduleExecution(Callable<T> callable, long delayMillis, boolean wakeUp, long executionLatency) {
        return new AlarmTask<>(callable).schedule(delayMillis, wakeUp, executionLatency);
    }

    public <T> Future<T> scheduleRepeatedExecution(Runnable runnable, T result, long initialDelayMillis, long delayMillis, boolean wakeUp, long executionLatency) {
        return new AlarmTask<>(runnable, result).scheduleRepeating(initialDelayMillis, delayMillis, wakeUp, executionLatency);
    }

    public <T> Future<T> scheduleExecution(Runnable runnable, T result, long delayMillis, boolean wakeUp, long executionLatency) {
        return new AlarmTask<>(runnable, result).schedule(delayMillis, wakeUp, executionLatency);
    }

    public Future<Void> scheduleRepeatedExecution(Runnable runnable, long initialDelayMillis, long delayMillis, boolean wakeUp, long executionLatency) {
        return new AlarmTask<Void>(runnable, null).scheduleRepeating(initialDelayMillis, delayMillis, wakeUp, executionLatency);
    }

    public Future<Void> scheduleExecution(Runnable runnable, long delayMillis, boolean wakeUp, long executionLatency) {
        return new AlarmTask<Void>(runnable, null).schedule(delayMillis, wakeUp, executionLatency);
    }

    //---------------------------------CLASSES---------------------------------

    private PendingIntent makePendingIntent(int id) {
        Intent intent = new Intent(INTENT_ACTION, Uri.fromParts(INTENT_EXTRA_ID, String.valueOf(id), null));
        //intent.putExtra("source-name", toString());
        //intent.putExtra("source-hash", String.valueOf(hashCode()));
        //intent.putExtra("source-time", System.currentTimeMillis());
        return PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private int getAlarmType(boolean wakeUp) {return wakeUp ? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;}

    private class AlarmTask<T> extends FutureTask<T> {
        private final int id;
        private final PendingIntent alarmIntent;
        private boolean repeat;

        public AlarmTask(Callable<T> callable) {
            super(callable);
            this.id = counter.getAndIncrement();
            this.alarmIntent = makePendingIntent(id);
        }

        public AlarmTask(Runnable runnable, T result) {
            super(runnable, result);
            this.id = counter.getAndIncrement();
            this.alarmIntent = makePendingIntent(id);
        }

        private AlarmTask<T> schedule(long delayMillis, boolean wakeUp, long executionLatency) {
            if (isDone() || runnables.containsKey(id)) {
                throw new IllegalStateException("Alarm with id " + id + " already scheduled");
            }
            runnables.put(id, this);
            repeat = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setWindow(getAlarmType(wakeUp), delayMillis, executionLatency, alarmIntent);
            } else {
                alarmManager.set(getAlarmType(wakeUp), delayMillis, alarmIntent);
            }
            return this;
        }

        private AlarmTask<T> scheduleRepeating(long initialDelayMillis, long delayMillis, boolean wakeUp, long executionLatency) {
            if (isDone() || runnables.containsKey(id)) {
                throw new IllegalStateException("Alarm with id " + id + " already scheduled");
            }
            runnables.put(id, this);
            repeat = true;
            alarmManager.setRepeating(getAlarmType(wakeUp), initialDelayMillis, delayMillis, alarmIntent);
            //TODO consider using repeated batch window scheduling as described in AlarmManager#setRepeating(...) to correctly use executionLatency
            return this;
        }

        @Override
        protected void done() {
            runnables.remove(id);
            alarmManager.cancel(alarmIntent);

        }

        @Override
        public void run() {
            if (repeat) {
                super.runAndReset();
            } else {
                super.run();
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("#").append(id);
            if (repeat) {
                sb.append(" (r)");
            }
            if (isDone()) {
                try {
                    T result = get();
                    sb.append(" [success]: ").append(result);
                } catch (InterruptedException e) {
                    sb.append(" [??]: ").append(e);
                } catch (CancellationException e) {
                    sb.append(" [cancelled]: ").append(e);
                } catch (ExecutionException e) {
                    sb.append(" [failed]: ").append(e);
                }
            } else if (runnables.containsKey(id)) {
                sb.append(" [scheduled]: ").append(alarmIntent);
            } else {
                sb.append(" [new]: ").append(alarmIntent);
            }
            return sb.toString();
        }
    }
}