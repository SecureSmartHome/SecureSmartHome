package de.unipassau.isl.evs.ssh.core.schedule;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.concurrent.Promise;

/**
 * An ExecutorService that can schedule commands to run after a given
 * delay, or to execute periodically.
 *
 * @author Christoph Fraedrich
 */
public abstract class ExecutionServiceComponent extends AbstractComponent implements EventLoopGroup {
    public static final Key<ExecutionServiceComponent> KEY = new Key<>(ExecutionServiceComponent.class);
    private EventLoopGroup eventLoopGroup;

    protected abstract EventLoopGroup createEventLoopGroup();

    @Override
    public void init(Container container) {
        super.init(container);
        eventLoopGroup = createEventLoopGroup();
    }

    @Override
    public void destroy() {
        eventLoopGroup.shutdownGracefully();
        super.destroy();
    }

    // UTILS////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Return a new {@link Promise}.
     *
     * @see #next()
     * @see EventLoop#newPromise()
     */
    public <V> Promise<V> newPromise() {
        return next().newPromise();
    }

    /**
     * Create a new {@link ProgressivePromise}.
     */
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return next().newProgressivePromise();
    }

    /**
     * Create a new {@link io.netty.util.concurrent.Future} which is marked as succeeded already. So {@link io.netty.util.concurrent.Future#isSuccess()}
     * will return {@code true}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */
    public <V> io.netty.util.concurrent.Future<V> newSucceededFuture(@Nullable V result) {
        return next().newSucceededFuture(result);
    }

    /**
     * Create a new {@link io.netty.util.concurrent.Future} which is marked as failed already. So {@link io.netty.util.concurrent.Future#isSuccess()}
     * will return {@code false}. All {@link FutureListener} added to it will be notified directly. Also
     * every call of blocking methods will just return without blocking.
     */
    public <V> io.netty.util.concurrent.Future<V> newFailedFuture(Throwable cause) {
        return next().newFailedFuture(cause);
    }

    // DELEGATES ///////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public EventLoop next() {
        return eventLoopGroup.next();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return eventLoopGroup.register(channel);
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
        return eventLoopGroup.register(channel, promise);
    }

    @Override
    public boolean isShuttingDown() {
        return eventLoopGroup.isShuttingDown();
    }

    @Override
    public io.netty.util.concurrent.Future<?> shutdownGracefully() {
        return eventLoopGroup.shutdownGracefully();
    }

    @Override
    public io.netty.util.concurrent.Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        return eventLoopGroup.shutdownGracefully(quietPeriod, timeout, unit);
    }

    @Override
    public io.netty.util.concurrent.Future<?> terminationFuture() {
        return eventLoopGroup.terminationFuture();
    }

    @Override
    @Deprecated
    public void shutdown() {
        eventLoopGroup.shutdown();
    }

    @NonNull
    @Override
    @Deprecated
    public List<Runnable> shutdownNow() {
        return eventLoopGroup.shutdownNow();
    }

    @Override
    public <E extends EventExecutor> Set<E> children() {
        return eventLoopGroup.children();
    }

    @NonNull
    @Override
    public io.netty.util.concurrent.Future<?> submit(Runnable task) {
        return eventLoopGroup.submit(task);
    }

    @NonNull
    @Override
    public <T> io.netty.util.concurrent.Future<T> submit(Runnable task, T result) {
        return eventLoopGroup.submit(task, result);
    }

    @NonNull
    @Override
    public <T> io.netty.util.concurrent.Future<T> submit(Callable<T> task) {
        return eventLoopGroup.submit(task);
    }

    @NonNull
    @Override
    public io.netty.util.concurrent.ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return eventLoopGroup.schedule(command, delay, unit);
    }

    @NonNull
    @Override
    public <V> io.netty.util.concurrent.ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return eventLoopGroup.schedule(callable, delay, unit);
    }

    @NonNull
    @Override
    public io.netty.util.concurrent.ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return eventLoopGroup.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @NonNull
    @Override
    public io.netty.util.concurrent.ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return eventLoopGroup.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public boolean isShutdown() {
        return eventLoopGroup.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return eventLoopGroup.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return eventLoopGroup.awaitTermination(timeout, unit);
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return eventLoopGroup.invokeAll(tasks);
    }

    @NonNull
    @Override
    public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return eventLoopGroup.invokeAll(tasks, timeout, unit);
    }

    @NonNull
    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return eventLoopGroup.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return eventLoopGroup.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        eventLoopGroup.execute(command);
    }

    @Override
    public void close() throws Exception {
        eventLoopGroup.close();
    }
}
