package de.unipassau.isl.evs.ssh.master.network;

import java.io.IOException;
import java.util.Objects;

import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import io.netty.channel.Channel;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SucceededFuture;

/**
 * Receives messages from system components and decides how to route them to the targets.
 *
 * @author Niko Fink
 */
public class ServerOutgoingRouter extends OutgoingRouter {
    @Override
    protected Future<Void> doSendMessage(Message.AddressedMessage amsg) {
        if (Objects.equals(amsg.getToID(), getOwnID())) {
            //Send Local
            requireComponent(IncomingDispatcher.KEY).dispatch(amsg);
            return new SucceededFuture<>(requireComponent(Server.KEY).getExecutor().next(), null);
        } else {
            //Find client and send the message there
            Channel channel = requireComponent(Server.KEY).findChannel(amsg.getToID());
            if (channel == null || !channel.isOpen()) {
                Exception e = new IOException("Client " + amsg.getToID() + " is not connected");
                e.fillInStackTrace();
                return new FailedFuture<>(requireComponent(Server.KEY).getExecutor().next(), e);
                //in a future version, pending messages could be queued instead of failed directly
            } else {
                return channel.writeAndFlush(amsg);
            }
        }
    }
}
