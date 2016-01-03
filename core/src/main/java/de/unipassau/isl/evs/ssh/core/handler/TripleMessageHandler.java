package de.unipassau.isl.evs.ssh.core.handler;

import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;

/**
 * TODO JavaDoc
 * @author Niko Fink
 */
public abstract class TripleMessageHandler<T, U, V> extends AbstractMessageHandler {
    protected final RoutingKey<T> routingKey1;
    protected final RoutingKey<U> routingKey2;
    protected final RoutingKey<V> routingKey3;

    public TripleMessageHandler(RoutingKey<T> routingKey1, RoutingKey<U> routingKey2, RoutingKey<V> routingKey3) {
        this.routingKey1 = routingKey1;
        this.routingKey2 = routingKey2;
        this.routingKey3 = routingKey3;
    }

    protected abstract void handleRouted1(Message.AddressedMessage message, T payload);

    protected abstract void handleRouted2(Message.AddressedMessage message, U payload);

    protected abstract void handleRouted3(Message.AddressedMessage message, V payload);

    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{routingKey1, routingKey2, routingKey3};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (routingKey1.matches(message)) {
            handleRouted1(message, routingKey1.getPayload(message));
        } else if (routingKey2.matches(message)) {
            handleRouted2(message, routingKey2.getPayload(message));
        } else if (routingKey3.matches(message)) {
            handleRouted3(message, routingKey3.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }
}
