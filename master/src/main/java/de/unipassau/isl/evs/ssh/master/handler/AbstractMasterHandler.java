package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.Permission;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

import static de.unipassau.isl.evs.ssh.core.messaging.Message.HEADER_REFERENCES_ID;

/**
 * This is a MasterHandler providing functionality all MasterHandlers need. This will avoid needing to implement the
 * same functionality over and over again.
 *
 * @author Leon Sell
 */
public abstract class AbstractMasterHandler extends AbstractMessageHandler {
    private Map<Integer, Message.AddressedMessage> proxiedMessages = new HashMap<>();

    /**
     * Remember that I sent another message (the proxy message) in order to fulfill a received request.
     * The originally received message will be mapped to the sequence number of the proxy message,
     * so that it can be later retrieved using the {@link Message#HEADER_REFERENCES_ID} of the reply for the proxy message.
     *
     * @see #takeProxiedReceivedMessage(int)
     * @param receivedMessage the Message with a Request from the App or the Slave that was received.
     * @param proxyMessage    the Message that was sent by me (the Master) in order to get data required for responding
     *                        to the original request.
     */
    protected void recordReceivedMessageProxy(Message.AddressedMessage receivedMessage, Message.AddressedMessage proxyMessage) {
        final DeviceID masterID = requireComponent(NamingManager.KEY).getMasterID();
        if (!proxyMessage.getFromID().equals(masterID)) {
            Log.w(getClass().getSimpleName(), "Messages from other devices can't act as proxy: " + proxyMessage);
        }
        proxiedMessages.put(proxyMessage.getSequenceNr(), receivedMessage);
    }

    /**
     * Get the originally received message identified by the sequence number of the proxy message that was sent to
     * fulfill the original message.
     *
     * @param proxySequenceNumber the sequence number of the proxy message, usually obtained from the reply to the
     *                            proxy message by getting the {@link Message#HEADER_REFERENCES_ID} header field.
     * @return the message that I originally received.
     */
    protected Message.AddressedMessage takeProxiedReceivedMessage(int proxySequenceNumber) {
        return proxiedMessages.remove(proxySequenceNumber);
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

    protected boolean hasPermission(DeviceID userDeviceID, Permission permission, String moduleName) {
        return isMaster(userDeviceID) || requireComponent(PermissionController.KEY)
                .hasPermission(userDeviceID, permission, moduleName);
    }

    @Deprecated
    protected void handleErrorMessage(Message.AddressedMessage message) {
        if (message.getHeader(HEADER_REFERENCES_ID) != null) {
            final Message.AddressedMessage correspondingMessage = takeProxiedReceivedMessage(message.getHeader(HEADER_REFERENCES_ID));
            sendMessage(
                    correspondingMessage.getFromID(),
                    correspondingMessage.getHeader(Message.HEADER_REPLY_TO_KEY),
                    new Message(message.getPayload())
            );
        } //else ignore
    }

    protected void sendMessageToAllDevicesWithPermission(
            Message messageToSend,
            Permission permission,
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
