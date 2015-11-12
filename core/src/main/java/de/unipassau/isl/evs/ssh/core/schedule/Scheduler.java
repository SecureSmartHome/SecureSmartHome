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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

public class Scheduler extends AbstractComponent {
    private static final String TAG = Scheduler.class.getSimpleName();
    private String INTENT_ACTION;
    private static final String INTENT_EXTRA_ID = "alarm-id";

    private AlarmManager alarmManager;

    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, Runnable> runnables = new ConcurrentHashMap<>();

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
        for (Integer id : runnables.keySet()) {
            alarmManager.cancel(makePendingIntent(id));
        }
        super.destroy();
    }

    private Context getContext() {
        return requireComponent(ContainerService.KEY_CONTEXT);
    }

    private PendingIntent makePendingIntent(int id) {
        Intent intent = new Intent(INTENT_ACTION, Uri.fromParts(INTENT_EXTRA_ID, String.valueOf(id), null));
        //intent.putExtra("source-name", toString());
        //intent.putExtra("source-hash", String.valueOf(hashCode()));
        //intent.putExtra("source-time", System.currentTimeMillis());
        return PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id = Integer.parseInt(intent.getData().getSchemeSpecificPart().replaceAll("[^0-9]", ""));
            Runnable runnable = runnables.get(id);
            //Log.v(TAG, "Run #" + id + ": " + runnable + " from " + intent);
            if (runnable != null) {
                runnable.run();
            } else {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, intent, PendingIntent.FLAG_NO_CREATE);
                Log.w(TAG, "Discarding unknown alarm #" + id + " and cancelling source PendingIntent " + pendingIntent);
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                }
            }
        }
    };

    //---------------------------------SCHEDULING FUNCTIONS--------------------

    public Future<?> scheduleRepeatedExecution(Runnable runnable, long initialDelayMillis, long delayMillis, boolean wakeUp, long executionLatency) {
        final int id = counter.getAndIncrement();

        runnables.put(id, runnable);

        int alarmType = wakeUp ? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;
        PendingIntent alarmIntent = makePendingIntent(id);
        //TODO consider using repeated batch window scheduling as described in AlarmManager#setRepeating(...) to correctly use executionLatency
        alarmManager.setRepeating(alarmType, initialDelayMillis, delayMillis, alarmIntent);

        return new AlarmFuture(id, alarmIntent);
    }

    public Future<?> scheduleExecution(Runnable runnable, long delayMillis, boolean wakeUp, long executionLatency) {
        final int id = counter.getAndIncrement();
        runnables.put(id, new OneTimeRunnable(id, runnable));

        int alarmType = wakeUp ? AlarmManager.ELAPSED_REALTIME_WAKEUP : AlarmManager.ELAPSED_REALTIME;
        PendingIntent alarmIntent = makePendingIntent(id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setWindow(alarmType, delayMillis, executionLatency, alarmIntent);
        } else {
            alarmManager.set(alarmType, delayMillis, alarmIntent);
        }

        return new AlarmFuture(id, alarmIntent);
    }

    //---------------------------------CLASSES---------------------------------

    private class OneTimeRunnable implements Runnable {
        private final int id;
        private final Runnable runnable;

        private OneTimeRunnable(int id, Runnable runnable) {
            this.id = id;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnables.remove(id);
            runnable.run();
        }
    }

    private class AlarmFuture implements Future<Void> {
        private final int id;
        private final PendingIntent alarmIntent;
        private boolean cancelled;

        public AlarmFuture(int id, PendingIntent alarmIntent) {
            this.id = id;
            this.alarmIntent = alarmIntent;
            cancelled = false;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!cancelled && !isDone()) {
                alarmManager.cancel(alarmIntent);
                runnables.remove(id);
                cancelled = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return !runnables.containsKey(id);
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            if (isCancelled()) {
                throw new CancellationException();
            }
            //TODO block
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (isCancelled()) {
                throw new CancellationException();
            }
            //TODO block
            return null;
        }
    }
}