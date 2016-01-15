package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ClimatePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;
import de.unipassau.isl.evs.ssh.core.database.UnknownReferenceException;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.LogActions.LIGHT_OFF_ACTION;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.LogActions.LIGHT_ON_ACTION;
import static de.unipassau.isl.evs.ssh.core.messaging.Message.HEADER_REFERENCES_ID;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_LIGHT_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DOOR_UNLATCH;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_LIGHT_SET_REPLY;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.REQUEST_LIGHT_STATUS;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.SWITCH_LIGHT;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.UNLATCH_DOOR;

/**
 * Handles light messages, logs them for the holiday simulation and generates messages
 * for each target and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterLightHandler extends AbstractMasterHandler {
    private static final String TAG = MasterLightHandler.class.getSimpleName();
    private static final int BRIGHTNESS_LOWER_THRESHOLD = 20;

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
        } else if (SLAVE_LIGHT_GET_ERROR.matches(message)) {
            handleGetError(message, SLAVE_LIGHT_GET_ERROR.getPayload(message));
        } else if (SLAVE_LIGHT_SET_ERROR.matches(message)) {
            handleSetError(message, SLAVE_LIGHT_SET_ERROR.getPayload(message));
        } else if (MASTER_DOOR_UNLATCH.matches(message)) {
            handleDoorUnlatched(message);
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
                SLAVE_LIGHT_GET_ERROR,
                SLAVE_LIGHT_SET_ERROR,
                MASTER_DOOR_UNLATCH};
    }

    private void handleSetError(Message.AddressedMessage message, ErrorPayload payload) {
        final Message.AddressedMessage correspondingMessage =
                takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        sendReply(correspondingMessage, new Message(payload));
    }

    private void handleGetError(Message.AddressedMessage message, ErrorPayload payload) {
        final Message.AddressedMessage correspondingMessage =
                takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        sendReply(correspondingMessage, new Message(payload));
    }

    private void handleSetRequest(Message.AddressedMessage message) {
        final LightPayload payload = MASTER_LIGHT_SET.getPayload(message);
        final Module atModule = payload.getModule();

        if (!hasPermission(message.getFromID(), SWITCH_LIGHT, atModule.getName())) {
            sendNoPermissionReply(message, SWITCH_LIGHT);
            return;
        }

        final Message messageToSend = new Message(payload);
        final Message.AddressedMessage sentMessage = sendMessage(atModule.getAtSlave(), SLAVE_LIGHT_SET, messageToSend);
        recordReceivedMessageProxy(message, sentMessage);
        try {
            if (payload.getOn()) {
                requireComponent(HolidayController.KEY).addHolidayLogEntryNow(LIGHT_ON_ACTION, atModule.getName());
            } else {
                requireComponent(HolidayController.KEY).addHolidayLogEntryNow(LIGHT_OFF_ACTION, atModule.getName());
            }
        } catch (UnknownReferenceException ure) {
            Log.i(TAG, "Can't created holiday log entry because the given module doesn't exist in the database.");
        }
    }

    private void handleGetRequest(Message.AddressedMessage message) {
        final LightPayload payload = MASTER_LIGHT_GET.getPayload(message);
        final Module atModule = payload.getModule();
        if (hasPermission(message.getFromID(), REQUEST_LIGHT_STATUS)) {
            final Message messageToSend = new Message(payload);
            final Message.AddressedMessage sendMessage =
                    sendMessage(atModule.getAtSlave(), SLAVE_LIGHT_GET, messageToSend);
            recordReceivedMessageProxy(message, sendMessage);
        } else {
            //no permission
            sendNoPermissionReply(message, REQUEST_LIGHT_STATUS);
        }
    }

    private void handleSetResponse(Message.AddressedMessage message) {
        final Message.AddressedMessage correspondingMessage =
                takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
        final LightPayload lightPayload = SLAVE_LIGHT_SET_REPLY.getPayload(message);
        final Message messageToSend = new Message(lightPayload);
        if (correspondingMessage != null) {
            sendReply(correspondingMessage, messageToSend);
        }
        sendMessageToAllDevicesWithPermission(messageToSend, REQUEST_LIGHT_STATUS, null, APP_LIGHT_UPDATE);
    }

    private void handleGetResponse(Message.AddressedMessage message) {
        final Message.AddressedMessage correspondingMessage = takeProxiedReceivedMessage(
                message.getHeader(Message.HEADER_REFERENCES_ID));

        final Message messageToSend = new Message(SLAVE_LIGHT_GET_REPLY.getPayload(message));
        sendReply(correspondingMessage, messageToSend);
    }

    private void handleDoorUnlatched(Message.AddressedMessage message) {
        final MasterUserLocationHandler masterUserLocationHandler = requireComponent(MasterUserLocationHandler.KEY);
        boolean switchedPosition = masterUserLocationHandler.switchedPositionToLocal(message.getFromID(), 2);
        boolean isExtern = !masterUserLocationHandler.isDeviceLocal(message.getFromID());

        if (hasPermission(message.getFromID(), UNLATCH_DOOR)) {
            if (switchedPosition && !isExtern) {
                final SlaveController slaveController = requireComponent(SlaveController.KEY);
                boolean tooDark = false;
                final List<Module> modulesByType = slaveController
                        .getModulesByType(CoreConstants.ModuleType.WeatherBoard);

                final MasterClimateHandler masterClimateHandler = requireComponent(MasterClimateHandler.KEY);
                final Map<Module, ClimatePayload> latestWeatherData = masterClimateHandler.getLatestWeatherData();
                for (Module module : modulesByType) {
                    if (latestWeatherData.get(module).getVisible() < BRIGHTNESS_LOWER_THRESHOLD) {
                        tooDark = true;
                        break;
                    }
                }

                if (tooDark) {
                    //Switch all lights on as user comes home and it is too dark
                    for (Module module : slaveController.getModulesByType(CoreConstants.ModuleType.Light)) {
                        try {
                            final HolidayController holidayController = requireComponent(HolidayController.KEY);
                            holidayController.addHolidayLogEntryNow(LIGHT_ON_ACTION, module.getName());
                        } catch (UnknownReferenceException e) {
                            Log.i(TAG, "Can't created holiday log entry because the given module doesn't exist "
                                    + "in the database.");
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