package de.unipassau.isl.evs.ssh.core.handler;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * Abstract base implementation for {@link MessageHandler}s providing many convenience functions for accessing the
 * IncomingDispatcher and the respective Container this Object is registered to.
 * If a subclass also implements {@link Component} also provides implementations of Methods in that interface,
 * automatically registering the Handler to the IncomingDispatcher once the Handler Component is registered to the Container.
 *
 * @author Team
 */
public abstract class AbstractMessageHandler implements MessageHandler {
    private Container container;
    private IncomingDispatcher dispatcher;
    private final Set<RoutingKey> registeredKeys = new HashSet<>(getRoutingKeys().length);

    /**
     * @return all the RoutingKeys this MessageHandler can handle
     */
    public abstract RoutingKey[] getRoutingKeys();

    /**
     * Provides a standard implementation of {@link Component#init(Container)} if a child class implements {@link Component}
     * and is used as such.
     * Once registered to the Container, the handler is also registered to the {@link IncomingDispatcher} to handle all
     * RoutingKeys returned by {@link #getRoutingKeys()}.
     */
    public void init(Container container) {
        this.container = container;
        container.require(IncomingDispatcher.KEY).registerHandler(this, getRoutingKeys());
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, RoutingKey routingKey) {
        registeredKeys.add(routingKey);
        this.dispatcher = dispatcher;
    }

    @Override
    public void handlerRemoved(RoutingKey routingKey) {
        registeredKeys.remove(routingKey);
        if (registeredKeys.isEmpty()) {
            dispatcher = null;
        }
    }

    /**
     * Provides a standard implementation of {@link Component#destroy()} if a child class implements {@link Component}
     * and is used as such.
     * Before being removed from the Container, the handler is also unregistered from the {@link IncomingDispatcher}.
     */
    public void destroy() {
        final IncomingDispatcher dispatcher = getDispatcher();
        if (dispatcher != null) {
            dispatcher.unregisterHandler(this, getRoutingKeys());
        }
        this.container = null;
    }

    @Nullable
    protected IncomingDispatcher getDispatcher() {
        return dispatcher == null ? container.get(IncomingDispatcher.KEY) : dispatcher;
    }

    @Nullable
    protected Container getContainer() {
        return dispatcher == null ? container : dispatcher.getContainer();
    }

    /**
     * @return {@code true} if this Handler is used as Component and is registered to a Container.
     */
    protected boolean isActive() {
        return container != null;
    }

    /**
     * @return {@code true} if this Handler is registered to an IncomingDispatcher.
     */
    protected boolean isRegistered() {
        return dispatcher != null;
    }

    /**
     * Fetch the Component from the Container or return {@code null} if the Component or the Container itself are not available.
     *
     * @see Container#get(Key)
     */
    @Nullable
    protected <T extends Component> T getComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.get(key);
        } else {
            return null;
        }
    }

    /**
     * Fetch the Component from the Container or throw an {@link IllegalStateException} if the Component or the
     * Container itself are not available.
     *
     * @see Container#require(Key)
     */
    @NonNull
    protected <T extends Component> T requireComponent(Key<T> key) {
        Container container = getContainer();
        if (container != null) {
            return container.require(key);
        } else {
            throw new IllegalStateException("Handler not registered to a IncomingDispatcher");
        }
    }

    /**
     * Convenience Method delegating to {@link OutgoingRouter#sendMessage(DeviceID, RoutingKey, Message)} of the current Container.
     */
    protected Message.AddressedMessage sendMessage(DeviceID toID, String routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessage(toID, routingKey, msg);
    }

    /**
     * Convenience Method delegating to {@link OutgoingRouter#sendMessage(DeviceID, RoutingKey, Message)} of the current Container.
     */
    protected Message.AddressedMessage sendMessageLocal(String routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessageLocal(routingKey, msg);
    }

    /**
     * Convenience Method delegating to {@link OutgoingRouter#sendMessage(DeviceID, RoutingKey, Message)} of the current Container.
     */
    protected Message.AddressedMessage sendMessageToMaster(String routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessageToMaster(routingKey, msg);
    }

    /**
     * Convenience Method delegating to {@link OutgoingRouter#sendMessage(DeviceID, RoutingKey, Message)} of the current Container.
     */
    protected Message.AddressedMessage sendMessage(DeviceID toID, RoutingKey routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessage(toID, routingKey, msg);
    }

    /**
     * Convenience Method delegating to {@link OutgoingRouter#sendMessage(DeviceID, RoutingKey, Message)} of the current Container.
     */
    protected Message.AddressedMessage sendMessageLocal(RoutingKey routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessageLocal(routingKey, msg);
    }

    /**
     * Convenience Method delegating to {@link OutgoingRouter#sendMessage(DeviceID, RoutingKey, Message)} of the current Container.
     */
    protected Message.AddressedMessage sendMessageToMaster(RoutingKey routingKey, Message msg) {
        return requireComponent(OutgoingRouter.KEY).sendMessageToMaster(routingKey, msg);
    }

    protected Message.AddressedMessage sendReply(Message.AddressedMessage original, Message reply) {
        return requireComponent(OutgoingRouter.KEY).sendReply(original, reply);
    }

    /**
     * Called if this Handler received a Message it can't handle.
     */
    protected void invalidMessage(Message.AddressedMessage message) {
        for (RoutingKey routingKey : registeredKeys) {
            if (routingKey.matches(message)) {
                throw new IllegalStateException("Handler did not accept message for RoutingKey " + routingKey
                        + " even though being registered for handling it. The message was " + message);
            }
        }
        throw new IllegalArgumentException("Handler is not registered for Handling message " + message);
    }

    @Override
    public String toString() {
        String name = getClass().getSimpleName();
        if (name.isEmpty()) {
            name = getClass().getName();
            int index = name.lastIndexOf(".");
            if (index >= 0 && index + 1 < name.length()) {
                name = name.substring(index + 1);
            }
        }
        return name;
    }
}
