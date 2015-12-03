package de.unipassau.isl.evs.ssh.master.handler;

import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.RegisterUserDevicePayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 */
public class MasterRegisterDeviceHandler extends AbstractMasterHandler {
    //TODO: update database to initially include these. Also move to?
    public static final String NOBODY = "Nobody";
    public static final String NOGROUP = "No Group";
    private Map<byte[], DeviceID> allowRegistrationFor;

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof RegisterUserDevicePayload) {
            //works for every functionality, because sender has to either be master or have ADD_USER permission
            if (hasPermission(message.getFromID(), new Permission(DatabaseContract.Permission.Values.ADD_USER))) {
                //which functionality
                switch (message.getRoutingKey()) {
                    //Add new token deviceID combination
                    case CoreConstants.RoutingKeys.MASTER_REGISTER_INIT:
                        handleInitRequest(message);
                        break;
                    //User one of the token deviceID combinations to register a new device
                    case CoreConstants.RoutingKeys.MASTER_REGISTER_FINALIZE:
                        handleFinalizeRequest(message);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey());
                }
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            sendErrorMessage(message);
        }
    }

    private void handleFinalizeRequest(Message.AddressedMessage message) {
        RegisterUserDevicePayload registerUserDevicePayload = ((RegisterUserDevicePayload) message.getPayload());
        if (allowRegistrationFor.containsKey(registerUserDevicePayload.getToken())
                && allowRegistrationFor.get(registerUserDevicePayload.getToken())
                .equals(registerUserDevicePayload.getUserDeviceID())) {
            try {
                requireComponent(KeyStoreController.KEY).saveCertificate(
                        registerUserDevicePayload.getCertificate(),
                        registerUserDevicePayload.getUserDeviceID().getIDString()
                );
            } catch (GeneralSecurityException gse) {
                //Todo: yeah, what do?
            }
            try {
                requireComponent(UserManagementController.KEY).addUserDevice(new UserDevice(
                        NOBODY,NOGROUP, registerUserDevicePayload.getUserDeviceID()));
            } catch (DatabaseControllerException e) {
                //Todo: yeah, what do?
            }
            allowRegistrationFor.remove(registerUserDevicePayload.getToken());
            System.out.println("Yeahalsdjflkasdjflkjaksdfjlaksjdflkjasdlkfjöalsdkjf");
        } //Todo: else error, ignore?
    }

    private void handleInitRequest(Message.AddressedMessage message) {
        RegisterUserDevicePayload registerUserDevicePayload = ((RegisterUserDevicePayload) message.getPayload());
        allowRegistrationFor.put(registerUserDevicePayload.getToken(), registerUserDevicePayload.getUserDeviceID());
    }
}