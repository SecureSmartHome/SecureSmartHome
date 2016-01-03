package de.unipassau.isl.evs.ssh.master.network;

import android.support.annotation.NonNull;
import android.util.Log;

import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoop;

/**
 * Distributes incoming messages from one of the multiple connections a Server has to the responsible handlers.
 *
 * @author Niko Fink
 */
@ChannelHandler.Sharable
public class ServerIncomingDispatcher extends IncomingDispatcher {
    private static final String TAG = ServerIncomingDispatcher.class.getSimpleName();

    private EventLoop eventLoop;

    @NonNull
    protected EventLoop getExecutor() {
        if (eventLoop == null || eventLoop.isShuttingDown() || eventLoop.isShutdown()) {
            Log.v(TAG, "EventLoop unavailable (" + eventLoop + "), getting new one");
            Server server = getContainer().require(Server.KEY);
            if (!server.isExecutorAlive() || !server.isChannelOpen()) {
                Log.w(TAG, "Could not dispatch message as Executor was shut down");
                return null; //TODO Niko: handle exceptional state (Niko, 2015-01-03)
            }
            eventLoop = server.getExecutor().next();
        }
        return eventLoop;
    }
}
