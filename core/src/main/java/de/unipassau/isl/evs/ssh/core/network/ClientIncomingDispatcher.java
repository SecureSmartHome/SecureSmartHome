package de.unipassau.isl.evs.ssh.core.network;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoop;

/**
 * Distributes incoming messages from the one single connection a client has to the responsible handlers.
 *
 * @author Niko Fink
 */
@ChannelHandler.Sharable
public class ClientIncomingDispatcher extends IncomingDispatcher {
    private static final String TAG = ClientIncomingDispatcher.class.getSimpleName();

    private EventLoop eventLoop;

    protected EventLoop getExecutor() {
        if (eventLoop == null || eventLoop.isShuttingDown() || eventLoop.isShutdown()) {
            Log.v(TAG, "EventLoop unavailable (" + eventLoop + "), getting new one");
            Client client = getContainer().require(Client.KEY);
            if (!client.isExecutorAlive() || !client.isChannelOpen()) {
                Log.w(TAG, "Could not dispatch message as Executor was shut down");
                return null;
            }
            eventLoop = client.getExecutor().next();
        }
        return eventLoop;
    }
}
