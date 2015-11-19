package de.unipassau.isl.evs.ssh.master.network;

import android.util.Log;

import java.util.Set;

import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.MessageHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Distributes incoming messages from one of the multiple connections a master has to the responsible handlers.
 */
@ChannelHandler.Sharable
public class ServerIncomingDispatcher extends IncomingDispatcher {
    private static final String TAG = ServerIncomingDispatcher.class.getSimpleName();

    private final Server server;

    public ServerIncomingDispatcher(Server server) {
        this.server = server;
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
        if (!server.isExecutorAlive() || !server.isChannelOpen()) {
            Log.w(TAG, "Could not dispatch message as Executor was shut down");
            return false;
        }
        Set<MessageHandler> handlers = mappings.get(msg.getRoutingKey());
        for (final MessageHandler handler : handlers) {
            server.getExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    handler.handle(msg);
                }
            });
        }
        return !handlers.isEmpty();
    }
}
