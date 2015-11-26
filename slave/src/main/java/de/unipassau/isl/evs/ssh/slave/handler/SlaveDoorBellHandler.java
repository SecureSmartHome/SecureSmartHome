package de.unipassau.isl.evs.ssh.slave.handler;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorBellPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import de.unipassau.isl.evs.ssh.drivers.lib.ButtonSensor;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;

import java.util.concurrent.TimeUnit;

/**
 * This handler receives an event when the door bell is rang, generates a message containing
 * information for this event and sends this information to the master.
 */
public class SlaveDoorBellHandler implements MessageHandler {

    private String modulName;
    private IncomingDispatcher dispatcher;

    @Override
    public void handle(Message.AddressedMessage message) {
        //TODO register listener
        //sendDoorBellInfo();
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
       /* this.dispatcher = dispatcher;
        dispatcher.getContainer().require(ExecutionServiceComponent.KEY).scheduleAtFixedRate(
                new DoorPollingRunnable(), 0, 200, TimeUnit.MILLISECONDS
        );*/
    }

    @Override
    public void handlerRemoved(String routingKey) {

    }



}