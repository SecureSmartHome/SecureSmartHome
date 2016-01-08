package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

/**
 * A request sent locally by the Master on first boot or by an admin app.
 * Indicates to the MasterRegisterDeviceHandler to generate a new token used for registration.
 *
 * @author Leon Sell
 */
public class GenerateNewRegisterTokenPayload implements MessagePayload {
    private byte[] token;
    private UserDevice userDevice;

    public GenerateNewRegisterTokenPayload(byte[] token, UserDevice userDevice) {
        this.token = token;
        this.userDevice = userDevice;
    }

    public byte[] getToken() {
        return token;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

    public UserDevice getUserDevice() {
        return userDevice;
    }

    public void setUserDevice(UserDevice userDevice) {
        this.userDevice = userDevice;
    }
}
