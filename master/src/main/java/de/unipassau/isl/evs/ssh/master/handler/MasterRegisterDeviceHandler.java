package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Base64;
import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GenerateNewRegisterTokenPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_USER_REGISTER;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterRegisterDeviceHandler extends AbstractMasterHandler implements Component {
    public static final Key<MasterRegisterDeviceHandler> KEY = new Key<>(MasterRegisterDeviceHandler.class);

    //Todo: close qr code after first register.
    //TODO: move to?
    public static final String FIRST_USER = "Admin";
    public static final String NO_GROUP = "No Group";
    private Map<String, UserDevice> userDeviceForToken = new HashMap<>();

    @Override
    public void init(Container container) {
        container.require(IncomingDispatcher.KEY).registerHandler(this, MASTER_USER_REGISTER);
    }

    @Override
    public void destroy() {
        Container container = getContainer();
        if (container != null) {
            container.require(IncomingDispatcher.KEY).unregisterHandler(this, MASTER_USER_REGISTER);
        }
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof GenerateNewRegisterTokenPayload) {
            if (hasPermission(message.getFromID(), new Permission(CoreConstants.Permission.BinaryPermission.ADD_USER.toString()))) {
                //which functionality
                switch (message.getRoutingKey()) {
                    //Add new register token
                    case MASTER_USER_REGISTER:
                        handleInitRequest(message);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey()
                                + " for GenerateNewRegisterTokenPayload");
                }
            }
        } else if (message.getPayload() instanceof MessageErrorPayload) {
            handleErrorMessage(message);
        } else {
            sendErrorMessage(message);
        }
    }

    public byte[] generateNewRegisterToken(UserDevice device) {
        byte[] token = QRDeviceInformation.getRandomToken();
        userDeviceForToken.put(Base64.encodeToString(token, Base64.NO_WRAP), device);
        return token;
    }

    /**
     * @return {@code true} if the registration was successful
     */
    public boolean registerDevice(X509Certificate certificate, byte[] token) {
        String base64Token = Base64.encodeToString(token, Base64.NO_WRAP);
        DeviceID deviceID;
        deviceID = DeviceID.fromCertificate(certificate);
        if (userDeviceForToken.containsKey(base64Token)) {
            UserDevice newDevice = userDeviceForToken.get(base64Token);
            newDevice.setUserDeviceID(deviceID);
            //Save certificate to KeyStore
            try {
                requireComponent(KeyStoreController.KEY).saveCertificate(certificate, deviceID.getIDString());
            } catch (GeneralSecurityException gse) {
                throw new IllegalArgumentException("An error occurred while adding the certificate of the new device to"
                        + " the KeyStore.", gse); //FIXME
            }
            addUserDeviceToDatabase(deviceID, newDevice);
            userDeviceForToken.remove(base64Token);
            return true;
        } else {
            Log.w(getClass().getSimpleName(), "Some tried using an unknown token to register. Token: " + base64Token
                    + ". Certificate: " + certificate);
            return false;
        }
    }

    private void addUserDeviceToDatabase(DeviceID deviceID, UserDevice newDevice) {
        try {
            requireComponent(UserManagementController.KEY).addUserDevice(newDevice);
        } catch (DatabaseControllerException dce) {
            throw new IllegalArgumentException("An error occurred while adding the new device to the database", dce);
        }
        //Add permissions
        if (requireComponent(UserManagementController.KEY).getUserDevices().size() == 1) {
            List<Permission> permissions = requireComponent(PermissionController.KEY).getPermissions();
            for (Permission permission : permissions) {
                try {
                    requireComponent(PermissionController.KEY).addUserPermission(deviceID, permission);
                } catch (UnknownReferenceException ure) {
                    throw new IllegalArgumentException("There was a problem adding all permissions to the newly added user. " +
                            "Maybe a permission was deleted while adding permissions to the new user.", ure);
                }
            }
        } else {
            String templateName = requireComponent(UserManagementController.KEY)
                    .getGroup(newDevice.getInGroup()).getTemplateName();
            List<Permission> permissions = requireComponent(PermissionController.KEY)
                    .getPermissionsOfTemplate(templateName);
            for (Permission permission : permissions) {
                try {
                    requireComponent(PermissionController.KEY).addUserPermission(deviceID, permission);
                } catch (UnknownReferenceException ure) {
                    throw new IllegalArgumentException("There was a problem adding all permissions to the newly added user. " +
                            "Maybe a permission was deleted while adding permissions to the new user.", ure);
                }
            }
        }
    }

    private void handleInitRequest(Message.AddressedMessage message) {
        GenerateNewRegisterTokenPayload generateNewRegisterTokenPayload =
                ((GenerateNewRegisterTokenPayload) message.getPayload());
        byte[] newToken = generateNewRegisterToken(generateNewRegisterTokenPayload.getUserDevice());
        String base64Token = Base64.encodeToString(newToken, Base64.NO_WRAP);
        userDeviceForToken.put(base64Token, generateNewRegisterTokenPayload.getUserDevice());
        Message reply = new Message(new GenerateNewRegisterTokenPayload(newToken,
                generateNewRegisterTokenPayload.getUserDevice()));
        sendMessage(message.getFromID(), message.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
    }
}