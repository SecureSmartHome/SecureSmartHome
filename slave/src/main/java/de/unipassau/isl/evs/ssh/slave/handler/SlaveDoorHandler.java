package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.drivers.lib.DoorBuzzer;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;

/**
 * Handles door messages and makes API calls accordingly.
 * @author bucher
 */
public class SlaveDoorHandler implements MessageHandler {

    private String routingKey;
    private IncomingDispatcher dispatcher;
    private DoorBuzzer doorBuzzer;

    @Override
    public void handle(Message.AddressedMessage message) {
        //TODO Benötigt keine Payload, da nur eine Funktion öffnen
        //TODO Antwort an Master zurückschicken, dass Tür betätigt wurde
        try {
            doorBuzzer.unlock(3000);
            //reply Message
        } catch (EvsIoException e) {
            Log.e(this.getClass().getSimpleName(), "Cannot unlock Door", e);
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        this.routingKey = routingKey;
        this.dispatcher = dispatcher;

        //get the following values via dispatcher
        int IoAdress = 0;

        doorBuzzer = new DoorBuzzer(IoAdress);
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }
}