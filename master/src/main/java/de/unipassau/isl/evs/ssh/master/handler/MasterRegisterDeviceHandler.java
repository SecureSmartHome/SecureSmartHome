package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Base64;
import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GenerateNewRegisterTokenPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.core.sec.KeyStoreController;
import de.unipassau.isl.evs.ssh.master.database.DatabaseControllerException;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.UnknownReferenceException;
import de.unipassau.isl.evs.ssh.master.database.UserManagementController;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_REGISTER;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_USER;

/**
 * Handles messages indicating that a device wants to register itself at the system and also generates
 * messages for each target that needs to know of this event and passes them to the OutgoingRouter.
 *
 * @author Leon Sell
 */
public class MasterRegisterDeviceHandler extends AbstractMasterHandler implements Component {
    public static final Key<MasterRegisterDeviceHandler> KEY = new Key<>(MasterRegisterDeviceHandler.class);

    //TODO Leon: move to CoreConstants and internationalize (Niko, 2016-01-05)
    public static final String FIRST_USER = "Admin";
    public static final String NO_GROUP = "No Group";
    private final Map<String, UserDevice> userDeviceForToken = new HashMap<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_USER_REGISTER};
    }


    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_USER_REGISTER.matches(message)) {
            handleInitRequest(message);
        } else {
            invalidMessage(message);
        }
    }

    /**
     * Generates a new token which devices can use to register at the system.
     *
     * @param device information which will be associated with the device using this token to register.
     * @return the generated token.
     */
    public byte[] generateNewRegisterToken(UserDevice device) {
        final byte[] token = DeviceConnectInformation.getRandomToken();
        userDeviceForToken.put(Base64.encodeToString(token, Base64.NO_WRAP), device);
        return token;
    }

    /**
     * Uses the information saved for the given token to register the new device.
     *
     * @return {@code true} if the registration was successful
     */
    public boolean registerDevice(X509Certificate certificate, byte[] token) {
        final String base64Token = Base64.encodeToString(token, Base64.NO_WRAP);
        final DeviceID deviceID = DeviceID.fromCertificate(certificate);
        if (userDeviceForToken.containsKey(base64Token)) {
            final UserDevice newDevice = userDeviceForToken.get(base64Token);
            newDevice.setUserDeviceID(deviceID);
            //Save certificate to KeyStore
            try {
                requireComponent(KeyStoreController.KEY).saveCertificate(certificate, deviceID.getIDString());
            } catch (GeneralSecurityException gse) {
                throw new RuntimeException("An error occurred while adding the certificate of the new device to"
                        + " the KeyStore.", gse);
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
            throw new RuntimeException("An error occurred while adding the new device to the database", dce);
        }
        //Add permissions. First device gets all permissions. Others get the permissions of the group they belong to.
        if (requireComponent(UserManagementController.KEY).getUserDevices().size() == 1) {
            final List<Permission> permissions = requireComponent(PermissionController.KEY).getPermissions();
            for (Permission permission : permissions) {
                try {
                    requireComponent(PermissionController.KEY).addUserPermission(
                            deviceID,
                            permission.getPermission(),
                            permission.getModuleName()
                    );
                } catch (UnknownReferenceException ure) {
                    throw new RuntimeException("There was a problem adding all permissions to the newly added user. " +
                            "Maybe a permission was deleted while adding permissions to the new user.", ure);
                }
            }
        } else {
            final String templateName = requireComponent(UserManagementController.KEY)
                    .getGroup(newDevice.getInGroup()).getTemplateName();
            final List<Permission> permissions = requireComponent(PermissionController.KEY)
                    .getPermissionsOfTemplate(templateName);
            for (Permission permission : permissions) {
                try {
                    requireComponent(PermissionController.KEY).addUserPermission(
                            deviceID,
                            permission.getPermission(),
                            permission.getModuleName()
                    );
                } catch (UnknownReferenceException ure) {
                    throw new RuntimeException("There was a problem adding all permissions to the newly added user. " +
                            "Maybe a permission was deleted while adding permissions to the new user.", ure);
                }
            }
        }
    }

    private void handleInitRequest(Message.AddressedMessage message) {
        if (hasPermission(message.getFromID(), ADD_USER)) {
            final GenerateNewRegisterTokenPayload generateNewRegisterTokenPayload =
                    MASTER_USER_REGISTER.getPayload(message);
            final byte[] newToken = generateNewRegisterToken(generateNewRegisterTokenPayload.getUserDevice());
            final String base64Token = Base64.encodeToString(newToken, Base64.NO_WRAP);
            userDeviceForToken.put(base64Token, generateNewRegisterTokenPayload.getUserDevice());
            final Message reply = new Message(new GenerateNewRegisterTokenPayload(
                    newToken,
                    generateNewRegisterTokenPayload.getUserDevice()
            ));
            sendReply(message, reply);
        } else {
            sendNoPermissionReply(message, ADD_USER);
        }
    }
}