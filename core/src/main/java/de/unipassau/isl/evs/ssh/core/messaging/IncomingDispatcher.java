package de.unipassau.isl.evs.ssh.core.messaging;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.Set;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

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
            Set<MessageHandler> handlers = mappings.get(msg.getRoutingKey());
            for (MessageHandler handler : handlers) {
                handler.handle(msg);
            }
            if (!handlers.isEmpty()) {
                return;
            }
        }
        super.channelRead(ctx, in);
    }
}
