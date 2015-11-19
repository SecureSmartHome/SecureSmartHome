package de.unipassau.isl.evs.ssh.core.messaging;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.util.DeviceID;
import io.netty.channel.ChannelFuture;

public interface OutgoingRouter extends Component {
    Key<OutgoingRouter> KEY = new Key<>(OutgoingRouter.class);

    ChannelFuture sendMessageLocal(String routingKey, Message message);

    ChannelFuture sendMessage(DeviceID toID, String routingKey, Message msg);
}
