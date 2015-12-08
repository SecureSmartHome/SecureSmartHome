package de.unipassau.isl.evs.ssh.core.network;

import android.util.Log;

import java.util.Set;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import io.netty.channel.ChannelHandler;

/**
 * Distributes incoming messages from the one single connection a client has to the responsible handlers.
 *
 * @author Niko Fink
 */
@ChannelHandler.Sharable
public class ClientIncomingDispatcher extends IncomingDispatcher {
    private static final String TAG = ClientIncomingDispatcher.class.getSimpleName();

    /**
     * Dispatches an AddressedMessage to its target handler using the EventExecutor of the Client.
     *
     * @param msg AddressedMessage to dispatch.
     * @return {@code true} if the Message was forwarded to at least one MessageHandler.
     */
    @Override
    public boolean dispatch(final Message.AddressedMessage msg) {
        Client client = getContainer().require(Client.KEY);
        if (!client.isExecutorAlive() || !client.isChannelOpen()) {
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
