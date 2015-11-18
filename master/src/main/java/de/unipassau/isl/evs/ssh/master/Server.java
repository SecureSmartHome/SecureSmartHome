package de.unipassau.isl.evs.ssh.master;

import java.util.Collection;
import java.util.Map;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.channel.ChannelPipeline;

/**
 * A Server is a Component that manages ChannelPipelines.
 * It uses those to send and receive messages over the network to and from Clients.
 * Incoming messages are forwarded to the IncomingDispatcher.
 */
public class Server extends AbstractComponent {
    public static final Key<Server> KEY = new Key<>(Server.class);

    private Map<DeviceID, ChannelPipeline> registered;
    private Collection<ChannelPipeline> openConnections;

    /**
     * Send an AddressedMessage to the ChannelPipeline associated with the given DeviceID.
     *
     * @param addressedMessage AddressedMessage to send.
     * @param deviceID         DeviceID whose associated ChannelPipeline the AddressedMessage is to be send to.
     */
    public void send(Message.AddressedMessage addressedMessage, DeviceID deviceID) {
        // TODO - implement Server.send
        throw new UnsupportedOperationException();
    }

}