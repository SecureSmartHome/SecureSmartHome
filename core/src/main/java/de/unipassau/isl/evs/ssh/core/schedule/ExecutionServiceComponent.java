package de.unipassau.isl.evs.ssh.core.schedule;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;

/**
 * An ExecutorService that can schedule commands to run after a given
 * delay, or to execute periodically.
 *
 * @author Christoph Fraedrich
 */
public class ExecutionServiceComponent extends AbstractComponent implements ScheduledExecutorService {
    public static final Key<ExecutionServiceComponent> KEY = new Key<>(ExecutionServiceComponent.class);
    private ScheduledExecutorService service;

    @Override
    public void init(Container container) {
        super.init(container);

        service = Executors.newScheduledThreadPool(2);
    }

    @Override
    public void destroy() {
        service.shutdown();
        super.destroy();
    }

    @NonNull
    @Override
    public ScheduledFuture<?> schedule(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
        return service.schedule(command, delay, unit);
    }

    @NonNull
    @Override
    public <V> ScheduledFuture<V> schedule(@NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit) {
        return service.schedule(callable, delay, unit);
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) {
        return service.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @NonNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Runnable command, long initialDelay, long delay, @NonNull TimeUnit unit) {
        return service.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        service.shutdown();
    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        return service.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return service.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return service.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return service.awaitTermination(timeout, unit);
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        return service.submit(task);
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        return service.submit(task, result);
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable task) {
        return service.submit(task);
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return service.invokeAll(tasks);
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return service.invokeAll(tasks, timeout, unit);
    }

    @NonNull
    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return service.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return service.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        service.execute(command);
    }
}
