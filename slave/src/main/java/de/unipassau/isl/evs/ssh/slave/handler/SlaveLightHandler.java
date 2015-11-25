package de.unipassau.isl.evs.ssh.slave.handler;

import android.util.Log;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.drivers.lib.EdimaxPlugSwitch;
import de.unipassau.isl.evs.ssh.drivers.lib.EvsIoException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Handles light messages and makes API calls accordingly.
 */
public class SlaveLightHandler implements MessageHandler {

    private EdimaxPlugSwitch plugSwitch;
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
            LightPayload payload = (LightPayload) message.getPayload();

            String moduleName = payload.getModuleName();

            if (message.getRoutingKey().equals(CoreConstants.RoutingKeys.SLAVE_LIGHT_SET)) {
                switchLight(payload, fromID, plugSwitch);
                replyStatus(fromID,moduleName);
            } else if (message.getRoutingKey().equals(CoreConstants.RoutingKeys.SLAVE_LIGHT_GET)) {
                replyStatus(fromID,moduleName);
            }
        } else {
            //TODO check Routing key
            String routingKey = CoreConstants.RoutingKeys.MASTER_LIGHT_GET;

            Message reply = new Message(new MessageErrorPayload(routingKey, message.getPayload()));
            dispatcher.getContainer().require(OutgoingRouter.KEY).sendMessage(fromID, routingKey, reply);
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        this.dispatcher = dispatcher;

        //TODO get the following values via dispatcher

        String lampAddress = "192.168.0.20";
        String lampUser = "admin";
        String lampPassword = "1234";
        int lampPort = 10000;


        //new Key<>(EdimaxPlugSwitch.class, moduleName);

        plugSwitch = new EdimaxPlugSwitch(lampAddress, lampPort, lampUser, lampPassword);
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }

    /**
     * Method the hides switching the light on and off
     *
     * @param payload which contains the info if the light should be switched on and off
     * @param fromID DeviceID which requested switching the light (needed for possible responses)
     * @param plugSwitch representing the driver of the lamp which is to be switched
     */
    private void switchLight(LightPayload payload, DeviceID fromID, EdimaxPlugSwitch plugSwitch) {
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
            String routingKey = CoreConstants.RoutingKeys.MASTER_LIGHT_SET;

            Log.e(this.getClass().getSimpleName(), "Cannot switch lamp due to error", e);
            Message reply = new Message(new MessageErrorPayload(routingKey, payload));
            OutgoingRouter router = dispatcher.getContainer().require(OutgoingRouter.KEY);
            router.sendMessage(fromID, routingKey, reply);
        }
    }

    /**
     * Sends a reply containing an info whether the light is on or off
     *
     * @param fromID DeviceID of the device that requested the light status
     * @param moduleName of the module which is to be checked
     */
    private void replyStatus(DeviceID fromID, String moduleName) {
        Message reply;
        try {
            reply = new Message(new LightPayload(plugSwitch.isOn(), moduleName));
            OutgoingRouter router = dispatcher.getContainer().require(OutgoingRouter.KEY);
            router.sendMessage(fromID, CoreConstants.RoutingKeys.MASTER_LIGHT_GET, reply);
        } catch (IOException | ParserConfigurationException | EvsIoException | SAXException e) {
            String routingKey = CoreConstants.RoutingKeys.MASTER_LIGHT_GET;

            Log.e(this.getClass().getSimpleName(), "Cannot retrieve lamp status due to error", e);
            reply = new Message(new MessageErrorPayload(routingKey, null));
            dispatcher.getContainer().require(OutgoingRouter.KEY).sendMessage(fromID, routingKey,
                    reply);
        }
    }
}