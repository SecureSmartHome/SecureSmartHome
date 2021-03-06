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

package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

/**
 * This is a MessageHandler providing functionality all AppHandlers need. This will avoid needing to implement the
 * same functionality over and over again.
 *
 * @author Niko Fink
 */
public abstract class AbstractAppHandler extends AbstractMessageHandler {
    private final Map<Integer, Promise<?>> mappings = new HashMap<>();

    /**
     * Generate a new Future that will track the status of the request contained in the given sent AddressedMessage.
     * If the AddressedMessage couldn't be sent due to an IOException, the returned Future will also fail.
     * Otherwise, the returned Future will complete once {@link #handleResponse(Message.AddressedMessage)} with the
     * according response message is called.
     * <p/>
     * <i>The generic return type of this method T must match the payload of the response message.
     * If you are e.g. expecting a {@link de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys#MASTER_LIGHT_GET_REPLY}
     * response, the returned Future should be a Future<LightPayload>. If a error response with an {@link ErrorPayload}
     * is received, the Future will fail the cause being the received ErrorPayload.
     * If the request can have more than one successful response
     * (except for the ErrorPayload, which is always handled as described above and doesn't count here),
     * the most common supertype of both payloads (i.e. MessagePayload) must be declared as generic type for the Future.</i>
     */
    protected <T> Future<T> newResponseFuture(final Message.AddressedMessage message) {
        if (!message.getFromID().equals(requireComponent(NamingManager.KEY).getOwnID())) {
            throw new IllegalArgumentException("Can only track messages sent by me");
        }
        final Promise<T> promise = requireComponent(ExecutionServiceComponent.KEY).newPromise();
        final MessageMetrics metrics;
        if (CoreConstants.TRACK_STATISTICS) {
            metrics = new MessageMetrics(message);
            promise.addListener(new FutureListener<T>() {
                @Override
                public void operationComplete(Future<T> future) throws Exception {
                    metrics.finished(future);
                    Log.v(AbstractAppHandler.this.getClass().getSimpleName() + "-Metrics", metrics.toString());
                }
            });
        } else {
            metrics = null;
        }
        message.getSendFuture().addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if (!future.isSuccess()) {
                    promise.setFailure(future.cause());
                } else {
                    if (metrics != null) {
                        metrics.sent(future);
                    }
                }
            }
        });
        promise.setUncancellable();
        mappings.put(message.getSequenceNr(), promise);
        return promise;
    }

    /**
     * Generate a new FailedFuture for requests that couldn't even be sent.
     * {@link Future#isDone()} will return {@code true} and {@link Future#isSuccess()} {@code false},
     * {@link Future#cause()} is set to the given Throwable.
     */
    protected <T> Future<T> newFailedFuture(Throwable cause) {
        return requireComponent(ExecutionServiceComponent.KEY).newFailedFuture(cause);
    }

    /**
     * Try to find the request that caused the response contained in message and set the result of the Future that
     * belongs to the request according to the content of the response.
     *
     * @throws IllegalArgumentException if the original request that caused the given response message hasn't been registered
     *                                  using {@link #newResponseFuture(Message.AddressedMessage)}.
     * @throws IllegalStateException    if the status of the Future couldn't be set.
     */
    protected void handleResponse(Message.AddressedMessage message) {
        final Promise promise = getPromise(message);
        if (promise == null) {
            throw new IllegalArgumentException("Could not find Request Promise for Response " + message);
        } else if (promise.isDone()) {
            throw new IllegalStateException("Got already done Request Promise " + promise + " for Response " + message);
        } else {
            final boolean success = setPromiseResult(promise, message);
            if (!success) {
                throw new IllegalStateException("Could not set status of Request Promise " + promise + " from Response " + message);
            }
        }
    }

    /**
     * Try to find the request that caused the response contained in message and set the result of the Future that
     * belongs to the request according to the content of the response.
     *
     * @return {@code false} if the original request that caused the given response message hasn't been registered
     * using {@link #newResponseFuture(Message.AddressedMessage)} or if the status of the Future couldn't be set.
     * Otherwise, i.e. the status of the Future was successfully set, returns {@code true}.
     */
    protected boolean tryHandleResponse(Message.AddressedMessage message) {
        final Promise promise = getPromise(message);
        return promise != null && !promise.isDone() && setPromiseResult(promise, message);
    }

    private Promise<?> getPromise(Message.AddressedMessage message) {
        return mappings.remove(message.getHeader(Message.HEADER_REFERENCES_ID));
    }

    @SuppressWarnings("unchecked")
    private boolean setPromiseResult(Promise promise, Message.AddressedMessage message) {
        final MessagePayload payload = message.getPayloadChecked(MessagePayload.class);
        if (payload instanceof ErrorPayload) {
            return promise.tryFailure((ErrorPayload) payload);
        } else {
            return promise.trySuccess(payload);
        }
    }

    /**
     * Class for measuring Message Response timing without holding a strong, non garbage-collected reference on the
     * used Futures
     */
    private static class MessageMetrics {
        private final String key;
        private String status;
        private final long queued;
        private long sent;
        private long finished;

        public MessageMetrics(Message.AddressedMessage message) {
            this.key = message.getRoutingKey();
            queued = System.nanoTime();
            status = message.getSendFuture().toString();
        }

        public void sent(Future<?> future) {
            sent = System.nanoTime();
            status = future.toString();
        }

        public void finished(Future<?> future) {
            finished = System.nanoTime();
            status = future.toString();
        }

        @Override
        public String toString() {
            StringBuilder bob = new StringBuilder();
            bob.append(key).append(" ");
            if (sent > 0) {
                bob.append("sent after ").append(TimeUnit.NANOSECONDS.toMillis(sent - queued)).append("ms ");
                if (finished > 0) {
                    bob.append("and ");
                } else {
                    bob.append("and response still pending ");
                }
            } else {
                bob.append("not sent ");
            }
            if (finished > 0) {
                bob.append("finished after ").append(TimeUnit.NANOSECONDS.toMillis(finished - queued)).append("ms ");
            }
            bob.append("with Future ").append(status);
            return bob.toString();
        }
    }
}
