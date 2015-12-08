package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Base64;
import android.util.Log;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.InitRegisterUserDevicePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.FinalizeRegisterUserDevicePayload;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 */
public class MasterRegisterDeviceHandler extends AbstractMasterHandler {
    //TODO: update database to initially include these. Also move to?
    public static final String FIRST_USER = "Admin";
    public static final String NO_GROUP = "No Group";
    private Map<String, UserDevice> groupForToken = new HashMap<>();

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof InitRegisterUserDevicePayload) {
            if (hasPermission(message.getFromID(), new Permission(DatabaseContract.Permission.Values.ADD_USER))) {
                //which functionality
                switch (message.getRoutingKey()) {
                    //Add new register token
                    case CoreConstants.RoutingKeys.MASTER_REGISTER_INIT:
                        handleInitRequest(message);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey()
                            + " for InitRegisterUserDevicePayload");
                }
            }
        } else if (message.getPayload() instanceof FinalizeRegisterUserDevicePayload) {
            //which functionality
            switch (message.getRoutingKey()) {
                //Use one of the tokens to register a new device
                case CoreConstants.RoutingKeys.MASTER_REGISTER_FINALIZE:
                    handleFinalizeRequest(message);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey()
                        + " for FinalizeRegisterUserDevicePayload");
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            sendErrorMessage(message);
        }
    }

    private void handleFinalizeRequest(Message.AddressedMessage message) {
        FinalizeRegisterUserDevicePayload finalizeRegisterUserDevicePayload = ((FinalizeRegisterUserDevicePayload) message.getPayload());
        String base64Token = Base64.encodeToString(finalizeRegisterUserDevicePayload.getToken(), Base64.NO_WRAP);
        if (groupForToken.containsKey(base64Token)) {
            UserDevice newDevice = groupForToken.get(finalizeRegisterUserDevicePayload.getToken());
            newDevice.setUserDeviceID(finalizeRegisterUserDevicePayload.getUserDeviceID());
            try {
                requireComponent(KeyStoreController.KEY).saveCertificate(
                        finalizeRegisterUserDevicePayload.getCertificate(),
                        finalizeRegisterUserDevicePayload.getUserDeviceID().getIDString()
                );
            } catch (GeneralSecurityException gse) {
                throw new IllegalArgumentException("An error occurred while adding the certificate of the new device to"
                        + " the KeyStore.", gse);
            }
            try {
                requireComponent(UserManagementController.KEY).addUserDevice(newDevice);
            } catch (DatabaseControllerException dce) {
                throw new IllegalArgumentException("An error occurred while adding the new device to the database",
                        dce);
            }
            //Add permissions
            String templateName = requireComponent(UserManagementController.KEY)
                    .getGroup(newDevice.getInGroup()).getTemplateName();
            List<Permission> permissions = requireComponent(PermissionController.KEY)
                    .getPermissionsOfTemplate(templateName);
            for (Permission permission : permissions) {
                try {
                    requireComponent(PermissionController.KEY).addUserPermission(message.getFromID(), permission);
                } catch (UnknownReferenceException ure) {
                    throw new IllegalArgumentException("There was a problem adding the all permissions to the newly"
                            + "added user. Maybe a permission was deleted while adding permissions to the new user.",
                            ure);
                }
            }

            groupForToken.remove(base64Token);
        } else {
            Log.v(getClass().getSimpleName(), "Some tried using an unknown token to register. Token: " + base64Token
                    + ". Certificate: " + finalizeRegisterUserDevicePayload.getCertificate());
        }
    }

    private void handleInitRequest(Message.AddressedMessage message) {
        InitRegisterUserDevicePayload initRegisterUserDevicePayload = ((InitRegisterUserDevicePayload) message.getPayload());
        if (isMaster(message.getFromID())) {
            String base64Token = Base64.encodeToString(initRegisterUserDevicePayload.getToken(), Base64.NO_WRAP);
            groupForToken.put(base64Token, initRegisterUserDevicePayload.getUserDevice());
        } else {
            byte[] newToken = QRDeviceInformation.getRandomToken();
            String base64Token = Base64.encodeToString(newToken, Base64.NO_WRAP);
            groupForToken.put(base64Token, initRegisterUserDevicePayload.getUserDevice());
            Message reply = new Message(new InitRegisterUserDevicePayload(newToken,
                    initRegisterUserDevicePayload.getUserDevice()));
            sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
        }
    }
}