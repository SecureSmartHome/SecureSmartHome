package de.unipassau.isl.evs.ssh.core.handler;

import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;

/**
 * @author Niko Fink
 */
public abstract class SimpleMessageHandler<T> extends AbstractMessageHandler {
    protected final RoutingKey<T> routingKey;

    protected SimpleMessageHandler(RoutingKey<T> routingKey) {
        this.routingKey = routingKey;
    }

    protected abstract void handleRouted(Message.AddressedMessage message, T payload);

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{routingKey};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (routingKey.matches(message)) {
            handleRouted(message, routingKey.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }
}
