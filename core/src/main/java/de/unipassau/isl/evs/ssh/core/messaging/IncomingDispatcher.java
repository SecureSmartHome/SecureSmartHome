package de.unipassau.isl.evs.ssh.core.messaging;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;

import java.security.SignatureException;
import java.util.Objects;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.NettyConstants.ATTR_PEER_ID;

/**
 * Distributes incoming messages to their target MessageHandlers.
 *
 * @author Niko Fink
 */
@ChannelHandler.Sharable
public class IncomingDispatcher extends ChannelHandlerAdapter implements Component {
    private static final String TAG = IncomingDispatcher.class.getSimpleName();
    public static final Key<IncomingDispatcher> KEY = new Key<>(IncomingDispatcher.class);

    protected final SetMultimap<RoutingKey, MessageHandler> mappings = HashMultimap.create();
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
            if (dispatch(msg)) return; //if no Handler can handle the Message, forward it in the pipeline
        }
        super.channelRead(ctx, in);
    }

    /**
     * Dispatches an AddressedMessage to its target handler using an EventExecutor.
     *
     * @param msg AddressedMessage to dispatch.
     * @return {@code true} if the Message was forwarded to at least one MessageHandler.
     */
    public boolean dispatch(final Message.AddressedMessage msg) {
        Set<MessageHandler> handlers = mappings.get(RoutingKey.forMessage(msg));
        final EventLoop executor = getEventLoop();
        Log.v(TAG, "DISPATCH " + msg + " to " + handlers + " using " + executor);
        for (final MessageHandler handler : handlers) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        handler.handle(msg);
                    } catch (Exception e) {
                        Log.e(TAG, "Handler " + handler + " crashed while handling message " + msg
                                + " with Exception " + Log.getStackTraceString(e));
                    }
                }
            });
        }
        return !handlers.isEmpty();
    }

    private EventLoop eventLoop;

    @NonNull
    private EventLoop getEventLoop() {
        if (eventLoop == null || eventLoop.isShuttingDown() || eventLoop.isShutdown()) {
            Log.v(TAG, "EventLoop unavailable (" + eventLoop + "), getting new one");
            eventLoop = getContainer().require(ExecutionServiceComponent.KEY).next();
        }
        if (eventLoop == null) {
            throw new IllegalStateException("Could not dispatch message as Executor was shut down");
        }
        return eventLoop;
    }

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
    public void registerHandler(MessageHandler handler, RoutingKey... routingKeys) {
        for (RoutingKey routingKey : routingKeys) {
            mappings.put(routingKey, handler);
            handler.handlerAdded(this, routingKey);
        }
    }

    /**
     * Unregister the handler so that it no longer receives any messages sent to one of the given routingKeys.
     */
    public void unregisterHandler(MessageHandler handler, RoutingKey... routingKeys) {
        for (RoutingKey routingKey : routingKeys) {
            handler.handlerRemoved(routingKey);
            mappings.remove(routingKey, handler);
        }
    }

    public Container getContainer() {
        return container;
    }

    @Override
    public String toString() {
        StringBuilder bob = new StringBuilder();
        bob.append(getClass().getSimpleName()).append(" [");
        final Multiset<RoutingKey> keys = mappings.keys();
        if (!keys.isEmpty()) {
            bob.append("\n");
            for (RoutingKey key : keys) {
                bob.append('\t').append(key).append(" => ").append(mappings.get(key)).append('\n');
            }
        }
        bob.append("]");
        return bob.toString();
    }
}