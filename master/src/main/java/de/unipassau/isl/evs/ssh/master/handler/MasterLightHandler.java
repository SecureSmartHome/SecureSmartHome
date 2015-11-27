package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * Handles light messages, logs them for the holiday simulation and generates messages
 * for each target and passes them to the OutgoingRouter.
 * @author leon
 */
public class MasterLightHandler extends AbstractMasterHandler {

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);

        if (message.getPayload() instanceof LightPayload) {
            LightPayload lightPayload = (LightPayload) message.getPayload();
            //Response or request?
            if (message.getHeader(Message.HEADER_REFERENCES_ID) == null) {
                //Request
                Module atModule = incomingDispatcher.getContainer().require(SlaveController.KEY)
                        .getModule(lightPayload.getModule().getName());
                Message messageToSend = new Message(lightPayload);
                messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());

                //which functionality
                switch (message.getRoutingKey()) {
                    //Set Light
                    case CoreConstants.RoutingKeys.MASTER_LIGHT_SET:
                        if (incomingDispatcher.getContainer().require(PermissionController.KEY)
                                .hasPermission(message.getFromID(),
                                        new Permission(DatabaseContract.Permission.Values.SWITCH_LIGHT,
                                                atModule.getName()))) {
                            Message.AddressedMessage sendMessage = incomingDispatcher.getContainer()
                                    .require(OutgoingRouter.KEY).sendMessage(atModule.getAtSlave(),
                                            CoreConstants.RoutingKeys.SLAVE_LIGHT_SET, messageToSend);
                            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
                            if (lightPayload.getOn()) {
                                incomingDispatcher.getContainer().require(HolidayController.KEY)
                                        .addHolidayLogEntry(CoreConstants.LogActions.LIGHT_ON_ACTION);
                            } else {
                                incomingDispatcher.getContainer().require(HolidayController.KEY)
                                        .addHolidayLogEntry(CoreConstants.LogActions.LIGHT_OFF_ACTION);
                            }
                        } else {
                            //no permission
                            sendErrorMessage(message);
                        }
                        break;

                    //Get status
                    case CoreConstants.RoutingKeys.MASTER_LIGHT_GET:
                        if (incomingDispatcher.getContainer().require(PermissionController.KEY).
                                hasPermission(message.getFromID(),
                                        new Permission(DatabaseContract.Permission.Values.REQUEST_LIGHT_STATUS,
                                                atModule.getName()))) {
                            Message.AddressedMessage sendMessage = incomingDispatcher.getContainer()
                                    .require(OutgoingRouter.KEY).sendMessage(atModule.getAtSlave(),
                                            CoreConstants.RoutingKeys.SLAVE_LIGHT_GET, messageToSend);
                            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
                        } else {
                            //no permission
                            sendErrorMessage(message);
                        }
                        break;
                    default:
                        sendErrorMessage(message);
                        break;
                }
            } else {
                //Response
                Message.AddressedMessage correspondingMessage =
                        getMessageOnBehalfOfSequenceNr(message.getHeader(Message.HEADER_REFERENCES_ID));
                Message messageToSend = new Message(lightPayload);
                messageToSend.putHeader(Message.HEADER_REFERENCES_ID, correspondingMessage.getSequenceNr());

                //works for both, so no switch required
                incomingDispatcher.getContainer().require(OutgoingRouter.KEY)
                        .sendMessage(correspondingMessage.getFromID(),
                                correspondingMessage.getHeader(Message.HEADER_REPLY_TO_KEY), messageToSend);
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            MessageErrorPayload messageErrorPayload = (MessageErrorPayload) message.getPayload();
            if (message.getHeader(Message.HEADER_REFERENCES_ID) != null) {
                Message.AddressedMessage correspondingMessage = getMessageOnBehalfOfSequenceNr(
                        message.getHeader(Message.HEADER_REFERENCES_ID));
                incomingDispatcher.getContainer().require(OutgoingRouter.KEY).sendMessage(
                        correspondingMessage.getFromID(), correspondingMessage.getHeader(Message.HEADER_REPLY_TO_KEY),
                        new Message(message.getPayload()));
            } //else ignore
        } else {
            sendErrorMessage(message);
        }

    }
}