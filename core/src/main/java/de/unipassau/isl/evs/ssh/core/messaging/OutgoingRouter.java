package de.unipassau.isl.evs.ssh.core.messaging;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import io.netty.channel.ChannelFuture;

public abstract class OutgoingRouter extends AbstractComponent {
    public static final Key<OutgoingRouter> KEY = new Key<>(OutgoingRouter.class);

    public abstract ChannelFuture sendMessage(DeviceID toID, String routingKey, Message msg);

    public ChannelFuture sendMessageLocal(String routingKey, Message message) {
        return sendMessage(getLocalID(), routingKey, message);
    }

    public ChannelFuture sendMessageToMaster(String routingKey, Message message) {
        return sendMessage(getMasterID(), routingKey, message);
    }

    protected DeviceID getMasterID() {
        return requireComponent(NamingManager.KEY).getMasterID();
    }

    protected DeviceID getLocalID() {
        return requireComponent(NamingManager.KEY).getLocalDeviceId();
    }
}
