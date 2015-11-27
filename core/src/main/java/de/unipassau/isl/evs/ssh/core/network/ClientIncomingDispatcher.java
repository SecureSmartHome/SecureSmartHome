package de.unipassau.isl.evs.ssh.core.network;

import android.util.Log;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.Set;

/**
 * Distributes incoming messages form the one single connection a client has to the responsible handlers.
 *
 * @author Niko
 */
@ChannelHandler.Sharable
public class ClientIncomingDispatcher extends IncomingDispatcher {
    private static final String TAG = ClientIncomingDispatcher.class.getSimpleName();

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
