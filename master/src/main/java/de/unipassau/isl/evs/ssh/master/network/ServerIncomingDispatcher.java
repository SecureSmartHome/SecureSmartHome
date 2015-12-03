package de.unipassau.isl.evs.ssh.master.network;

import android.util.Log;

import java.util.Set;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import io.netty.channel.ChannelHandler;

/**
 * Distributes incoming messages from one of the multiple connections a Server has to the responsible handlers.
 *
 * @author Niko Fink
 */
@ChannelHandler.Sharable
public class ServerIncomingDispatcher extends IncomingDispatcher {
    private static final String TAG = ServerIncomingDispatcher.class.getSimpleName();

    /**
     * Dispatches an AddressedMessage to its target handler using the EventExecutor of the Server.
     *
     * @param msg AddressedMessage to dispatch.
     * @return {@code true} if the Message was forwarded to at least one MessageHandler.
     */
    @Override
    public boolean dispatch(final Message.AddressedMessage msg) {
        Server server = getContainer().require(Server.KEY);
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
