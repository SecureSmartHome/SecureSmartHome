package de.unipassau.isl.evs.ssh.core.messaging;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.security.SignatureException;
import java.util.Objects;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_PEER_ID;

/**
 * Distributes incoming messages to their target MessageHandlers.
 *
 * @author Niko Fink
 */
public abstract class IncomingDispatcher extends ChannelHandlerAdapter implements Component {
    public static final Key<IncomingDispatcher> KEY = new Key<>(IncomingDispatcher.class);

    public SetMultimap<String, MessageHandler> mappings = HashMultimap.create();
    private Container container;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object in) throws Exception {
        if (in instanceof Message.AddressedMessage) {
            final DeviceID peerID = ctx.attr(ATTR_PEER_ID).get();
            if (peerID == null) {
                ctx.close();
                throw new IllegalStateException("Unauthenticated peer");
            }
            final Message.AddressedMessage msg = (Message.AddressedMessage) in;
            if (!Objects.equals(msg.getFromID(), peerID)) { //signature is already verified by SignatureChecker
                ctx.close();
                throw new SignatureException("Connected to Device with ID " + peerID + " but received message " +
                        "seemingly from " + msg.getFromID());
            }
            if (dispatch(msg)) return;
        }
        super.channelRead(ctx, in);
    }

    /**
     * Dispatches an AddressedMessage to its target handler using an EventExecutor.
     *
     * @param msg AddressedMessage to dispatch.
     * @return {@code true} if the Message was forwarded to at least one MessageHandler.
     */
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