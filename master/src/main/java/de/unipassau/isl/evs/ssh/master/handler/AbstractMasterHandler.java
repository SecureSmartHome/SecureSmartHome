package de.unipassau.isl.evs.ssh.master.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * This is a MasterHandler providing functionality all MasterHandlers need. This will avoid needing to implement the
 * same functionality over and over again.
 *
 * @author Leon Sell
 */
public abstract class AbstractMasterHandler extends AbstractMessageHandler {
    private Map<Integer, Message.AddressedMessage> inbox = new HashMap<>();
    private Map<Integer, Integer> onBehalfOfMessage = new HashMap<>();

    /**
     * Save a Message to be able to respond to Messages from the app after requesting information from the slave.
     *
     * @param message Message to save.
     */
    protected void saveMessage(Message.AddressedMessage message) {
        //TODO clear sometime.
        inbox.put(message.getSequenceNr(), message);
    }

    /**
     * Get a saved Message.
     *
     * @param sequenceNr Sequence number of the requested Message.
     * @return Requested message.
     */
    protected Message.AddressedMessage getMessage(int sequenceNr) {
        return inbox.get(sequenceNr);
    }

    /**
     * Get saved Message by sequence number of the Message send on behalf of the saved Message.
     *
     * @param sequenceNr Sequence number of the Message send on behalf of the saved Message.
     * @return Requested Message.
     */
    protected Message.AddressedMessage getMessageOnBehalfOfSequenceNr(int sequenceNr) {
        return getMessage(getSequenceNrOnBehalfOfSequenceNr(sequenceNr));
    }

    /**
     * Make a connection between a received Message sequence number and the Message sequence number of the Message send
     * on behalf of that Message.
     *
     * @param newMessageSequenceNr        Message sequence number of the Message send on behalf of the Message.
     * @param onBehalfOfMessageSequenceNr Message sequence number of the original Message.
     */
    protected void putOnBehalfOf(int newMessageSequenceNr, int onBehalfOfMessageSequenceNr) {
        onBehalfOfMessage.put(newMessageSequenceNr, onBehalfOfMessageSequenceNr);

    }

    /**
     * Request the Message sequence number of a previously made connection between Message sequence numbers.
     *
     * @param sequenceNr The Message sequence number of the Message send on behalf of the requested Message.
     * @return Requested Message's sequence number..
     */
    protected Integer getSequenceNrOnBehalfOfSequenceNr(int sequenceNr) {
        return onBehalfOfMessage.get(sequenceNr);
    }

    /**
     * Returns whether the device with the given DeviceID is a Slave.
     *
     * @param deviceID DeviceID for the device to check for whether it a Slave or not.
     * @return Whether or not the device with given DeviceID is a Slave.
     */
    public boolean isSlave(DeviceID deviceID) {
        return requireComponent(SlaveController.KEY).getSlave(deviceID) != null;
    }

    /**
     * Returns whether the device with the given DeviceID is a Master.
     *
     * @param userDeviceID DeviceID for the device to check for whether it a Master or not.
     * @return Whether or not the device with given DeviceID is a Master.
     */
    public boolean isMaster(DeviceID userDeviceID) {
        return userDeviceID.equals(requireComponent(NamingManager.KEY).getMasterID());
    }

    protected boolean hasPermission(DeviceID userDeviceID, de.unipassau.isl.evs.ssh.core.sec.Permission permission,
                                    String moduleName) {
        return isMaster(userDeviceID)
                || requireComponent(PermissionController.KEY).hasPermission(userDeviceID, permission, moduleName);
    }

    @Deprecated
    protected void handleErrorMessage(Message.AddressedMessage message) {
        if (message.getHeader(Message.HEADER_REFERENCES_ID) != null) {
            final Message.AddressedMessage correspondingMessage = getMessageOnBehalfOfSequenceNr(
                    message.getHeader(Message.HEADER_REFERENCES_ID));
            sendMessage(
                    correspondingMessage.getFromID(),
                    correspondingMessage.getHeader(Message.HEADER_REPLY_TO_KEY),
                    new Message(message.getPayload())
            );
        } //else ignore
    }

    protected void sendMessageToAllDevicesWithPermission(
            Message messageToSend,
            de.unipassau.isl.evs.ssh.core.sec.Permission permission,
            String moduleName,
            RoutingKey routingKey
    ) {
        final List<UserDevice> allUserDevicesWithPermission = requireComponent(PermissionController.KEY)
                .getAllUserDevicesWithPermission(permission, moduleName);
        for (UserDevice userDevice : allUserDevicesWithPermission) {
            sendMessage(userDevice.getUserDeviceID(), routingKey, messageToSend);
        }
    }
}
