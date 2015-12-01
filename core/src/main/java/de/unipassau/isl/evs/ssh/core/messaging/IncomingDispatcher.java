package de.unipassau.isl.evs.ssh.core.messaging;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import io.netty.channel.ChannelHandlerAdapter;

/**
 * Abstract class providing base methods for IncomingDispatchers.
 * IncomingDispatchers distribute incoming messages to the specific handler.
 *
 * @author Niko
 */
public abstract class IncomingDispatcher extends ChannelHandlerAdapter implements Component {
    public static final Key<IncomingDispatcher> KEY = new Key<>(IncomingDispatcher.class);

    public SetMultimap<String, MessageHandler> mappings = HashMultimap.create();
    private Container container;

    public abstract boolean dispatch(Message.AddressedMessage msg);

    @Override
    public void init(Container container) {
        this.container = container;
    }

    @Override
    public void destroy() {
        this.container = null;
    }

    /**
     * Register the handler to receive all messages sent to one of the given routingKeys.
     */
    public void registerHandler(MessageHandler handler, String... routingKeys) {
        for (String routingKey : routingKeys) {
            mappings.put(routingKey, handler);
            handler.handlerAdded(this, routingKey);
        }
    }

    /**
     * Unregister the handler so that it no longer receives any messages sent to one of the given routingKeys.
     */
    public void unregisterHandler(MessageHandler handler, String... routingKeys) {
        for (String routingKey : routingKeys) {
            handler.handlerRemoved(routingKey);
            mappings.remove(routingKey, handler);
        }
    }

    public Container getContainer() {
        return container;
    }
}