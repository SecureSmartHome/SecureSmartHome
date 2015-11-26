package de.unipassau.isl.evs.ssh.core.network;

import java.util.Objects;

import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import io.netty.channel.ChannelFuture;

public class ClientOutgoingRouter extends OutgoingRouter {

    @Override
    protected ChannelFuture doSendMessage(Message.AddressedMessage amsg) {
        if (Objects.equals(amsg.getToID(), getLocalID())) {
            //Send local
            requireComponent(IncomingDispatcher.KEY).dispatch(amsg);
            return requireComponent(Client.KEY).getChannel().newSucceededFuture();
        } else if (Objects.equals(amsg.getToID(), getMasterID())) {
            //Send to master
            return requireComponent(Client.KEY).getChannel().writeAndFlush(amsg);
        } else {
            //Can't send to other devices
            IllegalArgumentException e = new IllegalArgumentException(
                    "Client " + getLocalID() + " can't send message to other client " + amsg.getToID());
            e.fillInStackTrace();
            return requireComponent(Client.KEY).getChannel().newFailedFuture(e);
        }
    }
}
