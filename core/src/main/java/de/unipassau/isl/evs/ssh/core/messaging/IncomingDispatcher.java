package de.unipassau.isl.evs.ssh.core.messaging;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.Set;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * Distributes incoming messages to the responsible handlers.
 */
public class IncomingDispatcher extends ChannelHandlerAdapter {
    public SetMultimap<String, MessageHandler> mappings = HashMultimap.create();

    public void registerHandler(MessageHandler handler, String... routingKeys) {
        for (String routingKey : routingKeys) {
            mappings.put(routingKey, handler);
        }
    }

    public void unregisterHandler(MessageHandler handler, String... routingKeys) {
        for (String routingKey : routingKeys) {
            mappings.remove(routingKey, handler);
        }
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object in) throws Exception {
        if (in instanceof Message.AddressedMessage) {
            Message.AddressedMessage msg = ((Message.AddressedMessage) in);
            if (dispatch(msg)) return;
        }
        super.channelRead(ctx, in);
    }

    /**
     * Dispatches an AddressedMessage to its target handler.
     * Do not call from outside the netty event thread!
     *
     * @param msg AddressedMessage to Dispatch.
     */
    private boolean dispatch(Message.AddressedMessage msg) {
        Set<MessageHandler> handlers = mappings.get(msg.getRoutingKey());
        for (MessageHandler handler : handlers) {
            handler.handle(msg);
        }
        return !handlers.isEmpty();
    }
}
