package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.sec.Permission;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.master.database.UnknownReferenceException;

import static de.unipassau.isl.evs.ssh.core.messaging.Message.HEADER_REFERENCES_ID;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_SET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET_REPLY;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT;

/**
 * Handles light messages, logs them for the holiday simulation and generates messages
 * for each target and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterLightHandler extends AbstractMasterHandler {
    private static final String TAG = MasterLightHandler.class.getSimpleName();
    private static final int BRIGHTNESS_LOWER_THRESHOLD = 20000; //TODO migrate to constants

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_LIGHT_SET.matches(message)) {
            handleSetRequest(message);
        } else if (MASTER_LIGHT_GET.matches(message)) {
            handleGetRequest(message);
        } else if (SLAVE_LIGHT_GET_REPLY.matches(message)) {
            handleGetResponse(message);
        } else if (SLAVE_LIGHT_SET_REPLY.matches(message)) {
            handleSetResponse(message);
        } else if (MASTER_DOOR_UNLATCH.matches(message)) {
            handleDoorUnlatched(message);
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_LIGHT_SET,
                MASTER_LIGHT_GET,
                SLAVE_LIGHT_SET_REPLY,
                SLAVE_LIGHT_GET_REPLY,
                MASTER_DOOR_UNLATCH};
    }

    private void handleSetRequest(Message.AddressedMessage message) {
        final LightPayload payload = MASTER_LIGHT_SET.getPayload(message);
        final Module atModule = payload.getModule();

        if (!hasPermission(message.getFromID(), SWITCH_LIGHT, atModule.getName())) {
            sendNoPermissionReply(message, SWITCH_LIGHT);
            return;
        }

        final Message messageToSend = new Message(payload);
        final Message.AddressedMessage sendMessage = sendMessage(
                atModule.getAtSlave(),
                SLAVE_LIGHT_SET,
                messageToSend
        );
        recordReceivedMessageProxy(message, sendMessage);
        try {
            if (payload.getOn()) {
                requireComponent(HolidayController.KEY).addHolidayLogEntryNow(
                        CoreConstants.LogActions.LIGHT_ON_ACTION,
                        atModule.getName()
                );
            } else {
                requireComponent(HolidayController.KEY).addHolidayLogEntryNow(
                        CoreConstants.LogActions.LIGHT_OFF_ACTION,
                        atModule.getName()
                );
            }
        } catch (UnknownReferenceException ure) {
            Log.i(TAG, "Can't created holiday log entry because the given module doesn't exist in the database.");
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
            final Message.AddressedMessage sendMessage = sendMessage(
                    atModule.getAtSlave(),
                    SLAVE_LIGHT_GET,
                    messageToSend
            );
            recordReceivedMessageProxy(message, sendMessage);
        } else {
            //no permission
            sendErrorMessage(message);
        }
    }

    private void handleSetResponse(Message.AddressedMessage message) {
        final Message.AddressedMessage correspondingMessage = takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        final LightPayload lightPayload = SLAVE_LIGHT_SET_REPLY.getPayload(message);
        final Message messageToSend = new Message(lightPayload);

        messageToSend.putHeader(Message.HEADER_REFERENCES_ID, correspondingMessage.getSequenceNr());

        sendMessageToAllDevicesWithPermission(
                messageToSend,
                Permission.REQUEST_LIGHT_STATUS,
                null,
                MASTER_LIGHT_SET_REPLY
        );

        //TODO Leon: broadcast lightstatus to all connected devices
    }

    private void handleGetResponse(Message.AddressedMessage message) {
        final Message.AddressedMessage correspondingMessage = takeProxiedReceivedMessage(
                message.getHeader(Message.HEADER_REFERENCES_ID));

        final Message messageToSend = new Message(SLAVE_LIGHT_GET_REPLY.getPayload(message));
        sendReply(correspondingMessage, messageToSend);
    }

    private void handleDoorUnlatched(Message.AddressedMessage message) {
        boolean switchedPosition = requireComponent(MasterUserLocationHandler.KEY).switchedPositionToLocal(message.getFromID(), 2);
        boolean isExtern = !requireComponent(MasterUserLocationHandler.KEY).isDeviceLocal(message.getFromID());

        if (hasPermission(message.getFromID(), Permission.UNLATCH_DOOR, null)) {
            if (switchedPosition || isExtern) {
                boolean tooDark = false;
                final List<Module> modulesByType = requireComponent(SlaveController.KEY).getModulesByType(CoreConstants.ModuleType.WeatherBoard);

                for (Module module : modulesByType) {
                    final Map<Module, ClimatePayload> latestWeatherData = requireComponent(MasterClimateHandler.KEY).getLatestWeatherData();

                    if (latestWeatherData.get(module).getVisible() < BRIGHTNESS_LOWER_THRESHOLD) {
                        tooDark = true;
                        break;
                    }
                }

                if (tooDark) {
                    //Switch all lights on as user comes home and it is too dark
                    for (Module module : requireComponent(SlaveController.KEY).getModulesByType(CoreConstants.ModuleType.Light)) {
                        try {
                            requireComponent(HolidayController.KEY).addHolidayLogEntryNow(
                                    CoreConstants.LogActions.LIGHT_ON_ACTION,
                                    module.getName()
                            );
                        } catch (UnknownReferenceException e) {
                            Log.i(TAG, "Can't created holiday log entry because the given module doesn't exist in the database.");
                        }

                        LightPayload payload = new LightPayload(true, module);
                        final Message messageToSend = new Message(payload);
                        sendMessage(module.getAtSlave(), SLAVE_LIGHT_SET, messageToSend);
                    }
                }
            }
        }
    }
}