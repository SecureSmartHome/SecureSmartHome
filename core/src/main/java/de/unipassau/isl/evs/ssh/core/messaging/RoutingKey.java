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

package de.unipassau.isl.evs.ssh.core.messaging;

import android.support.annotation.NonNull;

import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;

/**
 * RoutingKeys are used by {@link de.unipassau.isl.evs.ssh.core.handler.MessageHandler}s to send {@link Message}s
 * to the correct destination Handlers and these destination Handlers to check, if they need to handle a received message.
 *
 * @see #matches(Message.AddressedMessage)
 * @author Niko Fink
 */
public class RoutingKey<T> {
    private static final String SUFFIX_REPLY = "/reply";
    private static final String SUFFIX_ERROR = "/error";
    private final Class<T> clazz;
    private final String key;

    public RoutingKey(String key, Class<T> clazz) {
        if (clazz == null) {
            throw new NullPointerException("class");
        }
        if (key == null) {
            throw new NullPointerException("key");
        }
        this.clazz = clazz;
        this.key = key;
    }

    /**
     * Get the RoutingKey for the giving message by getting the String {@link Message.AddressedMessage#getRoutingKey() routingKey}
     * stored in the Message and inferring the Class from the contained payload.
     */
    @NonNull
    public static RoutingKey forMessage(Message.AddressedMessage message) {
        final MessagePayload payload = message.getPayloadUnchecked();
        return new RoutingKey<>(message.getRoutingKey(), payload == null ? Void.class : payload.getClass());
    }

    @NonNull
    public Class<T> getPayloadClass() {
        return clazz;
    }

    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * Return a RoutingKey that identifies a response to messages identified by this RoutingKey with the given response payload.
     */
    @NonNull
    public <V> RoutingKey<V> getReply(Class<V> replyPayload) {
        return new RoutingKey<>(getReplyKey(key), replyPayload);
    }

    @NonNull
    public static String getReplyKey(String key) {
        return key + SUFFIX_REPLY;
    }

    public boolean isReply() {
        return key.endsWith(SUFFIX_REPLY);
    }

    public boolean isError() {
        return key.endsWith(SUFFIX_ERROR);
    }

    /**
     * @return {@code true} if {@link Message.AddressedMessage#getRoutingKey()} matched {@link #getKey()} and the payload of the
     * message is an instance of {@link #getPayloadClass()}.
     */
    public boolean matches(Message.AddressedMessage message) {
        return getKey().equals(message.getRoutingKey()) && payloadMatches(message);
    }

    /**
     * @return {@code true} if payload of the message is an instance of {@link #getPayloadClass()}.
     */
    public boolean payloadMatches(Message message) {
        return getPayloadClass().isInstance(message.getPayloadUnchecked()) ||
                (getPayloadClass() == Void.class && message.getPayloadUnchecked() == null);
    }

    /**
     * Cast the payload of the given message to {@link #getPayloadClass()}.
     * Use {@link Message#getPayloadChecked(Class)} if you are not sure about the RoutingKey of your message,
     * i.e. only one Payload Class but multiple String RoutingKeys are possible (e.g. GET_REPLY and SET_REPLY with the
     * same payload, but obviously different String keys).
     * As per {@link #payloadMatches(Message)} may only return null iff {@code T == Void.class}.
     *
     * @throws IllegalArgumentException if the message doesn't match this RoutingKey.
     * @see #matches(Message.AddressedMessage)
     */
    public T getPayload(Message message) {
        if (!payloadMatches(message)
                || (message instanceof Message.AddressedMessage && !matches((Message.AddressedMessage) message))) {
            throw new IllegalArgumentException("Message doesn't match RoutingKey " + this);
        }
        //noinspection ConstantConditions
        return message.getPayloadChecked(getPayloadClass());
    }

    @Override
    public String toString() {
        return getKey() + ":" + getPayloadClass().getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutingKey key = (RoutingKey) o;
        return clazz.equals(key.clazz) && this.key.equals(key.key);
    }

    @Override
    public int hashCode() {
        return 31 * clazz.hashCode() + key.hashCode();
    }
}
