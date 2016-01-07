package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.HolidayAction;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;
import de.unipassau.isl.evs.ssh.master.database.UnknownReferenceException;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET;

/**
 * Handles light messages, logs them for the holiday simulation and generates messages
 * for each target and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterLightHandler extends AbstractMasterHandler {
    private static final String TAG = MasterLightHandler.class.getSimpleName();

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);

        if (MASTER_LIGHT_SET.matches(message)) {
            handleSetRequest(message);
        } else if (MASTER_LIGHT_GET.matches(message)) {
            if (message.getHeader(Message.HEADER_REFERENCES_ID) == null) {
                handleGetRequest(message);
            } else {
                handleResponse(message);
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_LIGHT_SET, MASTER_LIGHT_GET};
    }

    private void handleSetRequest(Message.AddressedMessage message) {
        final LightPayload payload = MASTER_LIGHT_SET.getPayload(message);
        final Module atModule = payload.getModule();
        if (hasPermission(
                message.getFromID(),
                de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT,
                atModule.getName()
        )) {
            final Message messageToSend = new Message(payload);
            messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, MASTER_LIGHT_GET.getKey());
            final Message.AddressedMessage sendMessage = sendMessage(
                    atModule.getAtSlave(),
                    SLAVE_LIGHT_SET,
                    messageToSend
            );
            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
            try {
                if (payload.getOn()) {
                    requireComponent(HolidayController.KEY).addHolidayLogEntryNow(
                            CoreConstants.LogActions.LIGHT_ON_ACTION,
                            payload.getModule().getName()
                    );
                } else {
                    requireComponent(HolidayController.KEY).addHolidayLogEntryNow(
                            CoreConstants.LogActions.LIGHT_OFF_ACTION,
                            payload.getModule().getName()
                    );
                }
            } catch (UnknownReferenceException ure) {
                Log.i(TAG, "Can't created holiday log entry because the given module doesn't exist in the database.");
            }
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleGetRequest(Message.AddressedMessage message) {
        final LightPayload payload = MASTER_LIGHT_GET.getPayload(message);
        final Module atModule = payload.getModule();
        if (hasPermission(
                message.getFromID(),
                de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS,
                atModule.getName()
        )) {
            final Message messageToSend = new Message(payload);
            messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, MASTER_LIGHT_GET.getKey());
            final Message.AddressedMessage sendMessage = sendMessage(
                    atModule.getAtSlave(),
                    SLAVE_LIGHT_GET,
                    messageToSend
            );
            putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleResponse(Message.AddressedMessage message) {
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