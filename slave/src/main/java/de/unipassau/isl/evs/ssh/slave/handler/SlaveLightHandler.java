package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;

import org.xml.sax.SAXException;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;

/**
 * Handles light messages and makes API calls accordingly.
 */
public class SlaveLightHandler implements MessageHandler {

    private IncomingDispatcher dispatcher;

    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(Message.AddressedMessage message) {
        DeviceID fromID = message.getFromID();

        if (message.getPayload() instanceof LightPayload) {
            Key<EdimaxPlugSwitch> key = new Key<>(EdimaxPlugSwitch.class, ((LightPayload) message.getPayload())
                    .getModule().getName());
            EdimaxPlugSwitch plugSwitch = dispatcher.getContainer().require(key);

            if (message.getRoutingKey().equals(CoreConstants.RoutingKeys.SLAVE_LIGHT_SET)) {

                switchLight(message, plugSwitch);
                replyStatus(message, plugSwitch);
            } else if (message.getRoutingKey().equals(CoreConstants.RoutingKeys.SLAVE_LIGHT_GET)) {
                replyStatus(message, plugSwitch);
            }
        } else {
            //TODO check Routing key
            String routingKey = CoreConstants.RoutingKeys.MASTER_LIGHT_GET;

            Message reply = new Message(new MessageErrorPayload(message.getPayload()));
            dispatcher.getContainer().require(OutgoingRouter.KEY).sendMessage(fromID, routingKey, reply);
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }

    /**
     * Method the hides switching the light on and off
     *
     * @param original   message that should get a reply
     * @param plugSwitch representing the driver of the lamp which is to be switched
     */
    private void switchLight(Message.AddressedMessage original, EdimaxPlugSwitch plugSwitch) {
        LightPayload payload = (LightPayload) original.getPayload();
        try {
            if (payload.getOn()) {
                if (!plugSwitch.isOn()) {
                    plugSwitch.switchOn();
                }
            } else {
                if (plugSwitch.isOn()) {
                    plugSwitch.switchOff();
                }
            }
        } catch (EvsIoException | SAXException | IOException | ParserConfigurationException e) {
            Log.e(this.getClass().getSimpleName(), "Cannot switch lamp due to error", e);
            sendErrorMessage(original);
        }
    }

    /**
     * Sends a reply containing an info whether the light is on or off
     *
     * @param original message that should get a reply
     */
    private void replyStatus(Message.AddressedMessage original, EdimaxPlugSwitch plugSwitch) {
        LightPayload payload = (LightPayload) original.getPayload();
        Module module = payload.getModule();

        Message reply;
        try {
            reply = new Message(new LightPayload(plugSwitch.isOn(), module));
            reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr()); //TODO: getSequenzeNumber
            reply.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());

            OutgoingRouter router = dispatcher.getContainer().require(OutgoingRouter.KEY);
            router.sendMessage(original.getFromID(), original.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
        } catch (IOException | ParserConfigurationException | EvsIoException | SAXException e) {
            Log.e(this.getClass().getSimpleName(), "Cannot retrieve lamp status due to error", e);
            sendErrorMessage(original);
        }
    }

    private void sendErrorMessage(Message.AddressedMessage original) {
        Message reply;

        String routingKey = original.getHeader(Message.HEADER_REPLY_TO_KEY);
        reply = new Message(new MessageErrorPayload(null));
        reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr()); //TODO: getSequenzeNumber
        reply.putHeader(Message.HEADER_TIMESTAMP, System.currentTimeMillis());

        dispatcher.getContainer().require(OutgoingRouter.KEY).sendMessage(original.getFromID(), routingKey, reply);
    }
}