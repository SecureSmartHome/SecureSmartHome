package de.unipassau.isl.evs.ssh.core.schedule;

import android.test.InstrumentationTestCase;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;


public class SchedulerTest extends InstrumentationTestCase {
    private SimpleContainer container;
    private Scheduler scheduler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));
        container.register(Scheduler.KEY, new Scheduler());
        scheduler = container.get(Scheduler.KEY);
    }

    @Override
    protected void tearDown() throws Exception {
        container.shutdown();
        super.tearDown();
    }

    public void testSchedule1() throws Exception {
        final String expected = "hallo";
        long start = System.currentTimeMillis();
        Future<String> future = scheduler.scheduleExecution(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return expected;
            }
        }, 500, true, 0);
        final String actual = future.get(600, TimeUnit.MILLISECONDS);
        long end = System.currentTimeMillis();

        assertEquals(expected, actual);
        assertThat("Scheduler is not in-time", (double) end - start, closeTo(500, 100));
    }

    public void testSchedule2() throws Exception {
        final String expected = "hallo";
        long start = System.currentTimeMillis();
        Future<String> future = scheduler.scheduleExecution(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return expected;
            }
        }, 1000, true, 0);
        final String actual = future.get(1100, TimeUnit.MILLISECONDS);
        long end = System.currentTimeMillis();

        assertEquals(expected, actual);
        assertThat("Scheduler is not in-time", (double) end - start, closeTo(1000, 100));
    }

    public void testSchedule3() throws Exception {
        final String expected = "hallo";
        long start = System.currentTimeMillis();
        Future<String> future = scheduler.scheduleExecution(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return expected;
            }
        }, 500, true, 100);
        final String actual = future.get(700, TimeUnit.MILLISECONDS);
        long end = System.currentTimeMillis();

        assertEquals(expected, actual);
        assertThat("Scheduler is not in-time", (double) end - start, closeTo(500, 200));
    }

    public void testScheduleCancel() throws Exception {
        final AtomicBoolean called = new AtomicBoolean(false);

        final String expected = "hallo";
        long start = System.currentTimeMillis();
        Future<String> future = scheduler.scheduleExecution(new Callable<String>() {
            @Override
            public String call() throws Exception {
                called.set(true);
                return expected;
            }
        }, 500, true, 0);

        Thread.sleep(100);
        future.cancel(false);

        try {
            future.get(500, TimeUnit.MILLISECONDS);
            fail("future not cancelled");
        } catch (CancellationException ignore) {
        }
        long end = System.currentTimeMillis();

        Thread.sleep(500);

        assertFalse("Cancel did not work", called.get());
        assertThat("Cancel did not happen in-time", (double) end - start, closeTo(100, 50));
    }
}