package de.unipassau.isl.evs.ssh.core.network;

import java.io.IOException;
import java.util.Objects;

import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.SucceededFuture;

/**
 * Receives messages from system components and decides how to route them to the targets.
 *
 * @author Niko Fink
 */
public class ClientOutgoingRouter extends OutgoingRouter {
    @Override
    protected Future<Void> doSendMessage(Message.AddressedMessage amsg) {
        final Client client = requireComponent(Client.KEY);
        if (Objects.equals(amsg.getToID(), getOwnID())) {
            //Send local
            requireComponent(IncomingDispatcher.KEY).dispatch(amsg);
            return new SucceededFuture<>(client.getAliveExecutor().next(), null);
        } else if (Objects.equals(amsg.getToID(), getMasterID())) {
            //Send to master
            if (client.isConnectionEstablished()) {
                //noinspection ConstantConditions
                return client.getChannel().writeAndFlush(amsg);
            } else {
                //in a future version, pending messages could be queued instead of failed directly
                Exception e = new IOException("Master " + amsg.getToID() + " is not connected");
                e.fillInStackTrace();
                return new FailedFuture<>(requireComponent(Client.KEY).getAliveExecutor().next(), e);
            }
        } else {
            //Can't send to other devices
            IllegalArgumentException e = new IllegalArgumentException(
                    "Client " + getOwnID() + " can't send message to other client " + amsg.getToID());
            e.fillInStackTrace();
            return new FailedFuture<>(client.getAliveExecutor().next(), e);
        }
    }
}
