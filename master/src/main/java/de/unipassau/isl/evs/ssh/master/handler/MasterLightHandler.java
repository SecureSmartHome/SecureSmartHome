package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;

/**
 * Handles light messages, logs them for the holiday simulation and generates messages
 * for each target and passes them to the OutgoingRouter.
 *
 * @author leon
 */
public class MasterLightHandler extends AbstractMasterHandler {
    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);
        if (message.getPayload() instanceof LightPayload) {
            //Response or request?
            if (message.getHeader(Message.HEADER_REFERENCES_ID) == null) {
                //which functionality
                switch (message.getRoutingKey()) {
                    //Set Light
                    case CoreConstants.RoutingKeys.MASTER_LIGHT_SET:
                        handleSetRequest(message);
                        break;
                    case CoreConstants.RoutingKeys.MASTER_LIGHT_GET:
                        handleGetRequest(message);
                        break;
                    default:
                        sendErrorMessage(message);
                        break;
                }
            } else {
                handleResponse(message);
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            sendErrorMessage(message);
        }
    }

    private void handleSetRequest(Message.AddressedMessage message) {
        final Module atModule = ((LightPayload) message.getPayload()).getModule();
        if (hasPermission(
                message.getFromID(),
                new Permission(
                        DatabaseContract.Permission.Values.SWITCH_LIGHT,
                        atModule.getName()
                )
        )) {
            final Message messageToSend = new Message(message.getPayload());
            messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());
            final Message.AddressedMessage sendMessage = sendMessage(
                    atModule.getAtSlave(),
                    CoreConstants.RoutingKeys.SLAVE_LIGHT_SET,
                    messageToSend
            );
            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
            if (((LightPayload) message.getPayload()).getOn()) {
                requireComponent(HolidayController.KEY).addHolidayLogEntry(CoreConstants.LogActions.LIGHT_ON_ACTION);
            } else {
                requireComponent(HolidayController.KEY).addHolidayLogEntry(CoreConstants.LogActions.LIGHT_OFF_ACTION);
            }
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleGetRequest(Message.AddressedMessage message) {
        final Module atModule = ((LightPayload) message.getPayload()).getModule();
        //Get status
        if (hasPermission(
                message.getFromID(),
                new Permission(
                        DatabaseContract.Permission.Values.REQUEST_LIGHT_STATUS,
                        atModule.getName()
                )
        )) {
            final Message messageToSend = new Message(message.getPayload());
            messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());
            final Message.AddressedMessage sendMessage = sendMessage(
                    atModule.getAtSlave(),
                    CoreConstants.RoutingKeys.SLAVE_LIGHT_GET,
                    messageToSend
            );
            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleResponse(Message.AddressedMessage message) {
        //Response
        final Message.AddressedMessage correspondingMessage =
                getMessageOnBehalfOfSequenceNr(message.getHeader(Message.HEADER_REFERENCES_ID));
        final Message messageToSend = new Message(message.getPayload());
        messageToSend.putHeader(Message.HEADER_REFERENCES_ID, correspondingMessage.getSequenceNr());

        //works for get and set, so no switch required
        sendMessage(
                correspondingMessage.getFromID(),
                correspondingMessage.getHeader(Message.HEADER_REPLY_TO_KEY),
                messageToSend
        );
    }
}