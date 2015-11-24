package de.unipassau.isl.evs.ssh.core.network;

import android.util.Log;

import java.util.Set;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Distributes incoming messages form the one single connection a client has to the responsible handlers.
 */
@ChannelHandler.Sharable
public class ClientIncomingDispatcher extends IncomingDispatcher {
    private static final String TAG = ClientIncomingDispatcher.class.getSimpleName();

    private final Client client;

    public ClientIncomingDispatcher(Client client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object in) throws Exception {
        if (in instanceof Message.AddressedMessage) {
            if (dispatch((Message.AddressedMessage) in)) return;
        }
        super.channelRead(ctx, in);
    }

    /**
     * Dispatches an AddressedMessage to its target handler using the EventExecutor of this Dispatcher.
     *
     * @param msg AddressedMessage to Dispatch.
     */
    @Override
    public boolean dispatch(final Message.AddressedMessage msg) {
        if (!client.isExecutorAlive() || !client.isTCPChannelOpen()) {
            Log.w(TAG, "Could not dispatch message as Executor was shut down");
            return false;
        }
        Set<MessageHandler> handlers = mappings.get(msg.getRoutingKey());
        for (final MessageHandler handler : handlers) {
            client.getExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    handler.handle(msg);
                }
            });
        }
        return !handlers.isEmpty();
    }
}
