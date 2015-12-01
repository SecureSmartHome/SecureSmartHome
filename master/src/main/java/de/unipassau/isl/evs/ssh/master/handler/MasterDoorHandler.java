package de.unipassau.isl.evs.ssh.master.handler;

import java.util.Map;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorLockPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DoorUnlatchPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * Handles door messages and generates messages for each target and passes them to the OutgoingRouter.
 */
public class MasterDoorHandler extends AbstractMasterHandler {
    //Todo: formulate messages
    public static final String DOOR_UNLATCHED_MESSAGE = "Door unlatched";
    public static final String DOOR_UNLOCKED_MESSAGE = "Door unlocked";
    public static final String DOOR_LOCKED_MESSAGE = "Door locked";
    private Map<Integer, Boolean> lockedFor;

    private synchronized void setLocked(String moduleName, boolean locked) {
        //Todo: null check
        lockedFor.put(requireComponent(SlaveController.KEY).getModuleID(moduleName), locked);
    }

    private synchronized boolean getLocked(String moduleName) {
        return lockedFor.get(requireComponent(SlaveController.KEY).getModuleID(moduleName));
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        saveMessage(message);

        if (message.getPayload() instanceof DoorUnlatchPayload) {
            DoorUnlatchPayload doorUnlatchPayload = (DoorUnlatchPayload) message.getPayload();
            //Response or request?
            if (message.getHeader(Message.HEADER_REFERENCES_ID) == null) {
                //Request
                Module atModule = requireComponent(SlaveController.KEY)
                        .getModule(doorUnlatchPayload.getModuleName());
                Message messageToSend = new Message(doorUnlatchPayload);
                messageToSend.putHeader(Message.HEADER_REPLY_TO_KEY, message.getRoutingKey());

                //which functionality
                switch (message.getRoutingKey()) {
                    //Unlatch door
                    case CoreConstants.RoutingKeys.MASTER_DOOR_UNLATCH:
                        if (requireComponent(PermissionController.KEY)
                                .hasPermission(message.getFromID(), new Permission(
                                        DatabaseContract.Permission.Values.UNLATCH_DOOR, atModule.getName()))) {

                            if (!getLocked(atModule.getName())) {
                                Message.AddressedMessage sendMessage = requireComponent(OutgoingRouter.KEY)
                                        .sendMessage(atModule.getAtSlave(),
                                                CoreConstants.RoutingKeys.SLAVE_LIGHT_SET, messageToSend);
                                putOnBehalfOf(sendMessage.getSequenceNr(), message.getSequenceNr());
                            } else {
                                //locked
                                sendErrorMessage(message);
                            }
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
                Message messageToSend = new Message(new NotificationPayload(
                        CoreConstants.NotificationTypes.DOOR_UNLATCHED, DOOR_UNLATCHED_MESSAGE));
                //TODO: do i want to do this?!
                messageToSend.putHeader(Message.HEADER_REFERENCES_ID, correspondingMessage.getSequenceNr());

                requireComponent(OutgoingRouter.KEY)
                        .sendMessageLocal(CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, messageToSend);
            }
        } else if (message.getPayload() instanceof DoorLockPayload) {
            DoorLockPayload doorLockPayload = (DoorLockPayload) message.getPayload();
            //Request
            Module atModule = requireComponent(SlaveController.KEY)
                    .getModule(doorLockPayload.getModuleName());

            //which functionality
            switch (message.getRoutingKey()) {
                //(Un)Lock door
                case CoreConstants.RoutingKeys.MASTER_DOOR_LOCK_SET:
                    if (requireComponent(PermissionController.KEY)
                            .hasPermission(message.getFromID(), new Permission(
                                    DatabaseContract.Permission.Values.LOCK_DOOR, atModule.getName()))) {

                        setLocked(atModule.getName(), !doorLockPayload.isUnlock());

                        //Send notification
                        if (doorLockPayload.isUnlock()) {
                            requireComponent(OutgoingRouter.KEY).sendMessageLocal(
                                    CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, new Message(
                                            new NotificationPayload(CoreConstants.NotificationTypes.DOOR_UNLOCKED,
                                                    DOOR_UNLOCKED_MESSAGE)));
                        } else {
                            requireComponent(OutgoingRouter.KEY).sendMessageLocal(
                                    CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND, new Message(
                                            new NotificationPayload(CoreConstants.NotificationTypes.DOOR_LOCKED,
                                                    DOOR_LOCKED_MESSAGE)));

                        }
                    } else {
                        //no permission
                        sendErrorMessage(message);
                    }
                    break;
                //Get door lock state
                case CoreConstants.RoutingKeys.MASTER_DOOR_LOCK_GET:
                    if (requireComponent(PermissionController.KEY)
                            .hasPermission(message.getFromID(), new Permission(
                                    DatabaseContract.Permission.Values.REQUEST_DOOR_STATUS, atModule.getName()))) {


                        Message messageToSend = new Message(new DoorLockPayload(getLocked(atModule.getName()),
                                atModule.getName()));
                        messageToSend.putHeader(Message.HEADER_REFERENCES_ID, message.getSequenceNr());

                        requireComponent(OutgoingRouter.KEY).sendMessage(
                                message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), message);

                    } else {
                        //no permission
                        sendErrorMessage(message);
                    }
                    break;
                default:
                    sendErrorMessage(message);
                    break;
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            //Todo: handle error
        } else {
            sendErrorMessage(message);
        }
    }
}