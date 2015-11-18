package de.unipassau.isl.evs.ssh.slave;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

/**
 * A Client is a Component that has the Server as its only communication partner for
 * network communication and forwards messages to the IncomingDispatcher.
 */
public class Client extends AbstractComponent {
    public static final Key<Client> KEY = new Key<>(Client.class);

    private boolean connected;
    private DeviceID clientUID;

    /**
     * Send an AddressedMessage to it's destination (the master).
     *
     * @param addressedMessage AddressedMessage to send.
     */
    public void send(Message.AddressedMessage addressedMessage) {
        // TODO - implement Client.send
        throw new UnsupportedOperationException();
    }

}