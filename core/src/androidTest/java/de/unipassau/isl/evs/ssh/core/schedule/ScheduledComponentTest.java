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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

public class ScheduledComponentTest extends InstrumentationTestCase {
    public static final String EXTRA_DATA = "data";
    private Context context;
    private ServiceConnection conn;
    private SettableFuture<ContainerService.Binder> binding;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, ContainerService.class);
        //context.startService(intent);
        binding = SettableFuture.create();
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binding.set(((ContainerService.Binder) service));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("ScheduledComponentTest", "UNBIND");
            }
        };
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void tearDown() throws Exception {
        //context.stopService(intent);
        context.unbindService(conn);
        super.tearDown();
    }

    public void testSchedule1() throws InterruptedException, TimeoutException, ExecutionException {
        testSchedule(1000, 5000, 500);
    }

    public void testSchedule2() throws InterruptedException, TimeoutException, ExecutionException {
        testSchedule(5000, 10000, 1000);
    }

    public void testSchedule3() throws InterruptedException, TimeoutException, ExecutionException {
        testSchedule(10000, 20000, 1000);
    }

    private void testSchedule(int delay, int wait, int grace) throws InterruptedException, TimeoutException, ExecutionException {
        ContainerService.Binder container = binding.get(2, TimeUnit.SECONDS);
        final SettableFuture<String> result = SettableFuture.create();
        String expected = getName();

        Key<TestComponent> key = new Key<>(TestComponent.class, getName());
        container.register(key, new TestComponent() {
            @Override
            public void onReceive(Intent intent) {
                result.set(intent.getStringExtra(EXTRA_DATA));
            }
        });

        Scheduler scheduler = container.get(Scheduler.KEY);
        Bundle extras = new Bundle();
        extras.putString(EXTRA_DATA, expected);
        PendingIntent pendingIntent = scheduler.getPendingScheduleIntent(key, extras, PendingIntent.FLAG_CANCEL_CURRENT);

        long start = SystemClock.elapsedRealtime();
        scheduler.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                start + delay,
                pendingIntent);

        String actual = result.get(wait, TimeUnit.MILLISECONDS);
        long end = SystemClock.elapsedRealtime();

        assertEquals(expected, actual);
        assertThat("Scheduler is not in-time", (double) end - start, closeTo(delay, grace));
    }

    public void testScheduleCancel() throws Exception {
        ContainerService.Binder container = binding.get(2, TimeUnit.SECONDS);
        final SettableFuture<Boolean> result = SettableFuture.create();
        Scheduler scheduler = container.get(Scheduler.KEY);

        Key<TestComponent> key = new Key<>(TestComponent.class, getName());
        container.register(key, new TestComponent() {
            @Override
            public void onReceive(Intent intent) {
                result.set(true);
            }
        });

        PendingIntent pendingIntent = scheduler.getPendingScheduleIntent(key, null, PendingIntent.FLAG_CANCEL_CURRENT);
        scheduler.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000,
                pendingIntent);
        Thread.sleep(100);
        scheduler.cancel(pendingIntent);

        Thread.sleep(2000);

        if (result.isDone()) {
            fail("execution not cancelled");
        }
    }


    public void testScheduleRepeat() throws Exception {
        //test takes too long because repeating alarms can't be scheduled exactly

        //ContainerService.Binder container = binding.get(2, TimeUnit.SECONDS);
        //final AtomicInteger count = new AtomicInteger(0);
        //Scheduler scheduler = container.get(Scheduler.KEY);
        //
        //Key<Component> key = new Key<>(Component.class, getName());
        //container.register(key, new TestComponent() {
        //    @Override
        //    public void onReceive(Intent intent) {
        //        count.getAndIncrement();
        //    }
        //});
        //
        //PendingIntent pendingIntent = scheduler.getPendingScheduleIntent(key, null, PendingIntent.FLAG_CANCEL_CURRENT);
        //scheduler.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
        //        SystemClock.elapsedRealtime() + 10000, 10000,
        //        pendingIntent);
        //
        //Thread.sleep(45000);
        //scheduler.cancel(pendingIntent);
        //
        //Thread.sleep(20000);
        //
        //assertEquals("Repetition or cancelling did not work", 3, count.get());
    }

    private abstract class TestComponent extends AbstractComponent implements ScheduledComponent {
    }
}