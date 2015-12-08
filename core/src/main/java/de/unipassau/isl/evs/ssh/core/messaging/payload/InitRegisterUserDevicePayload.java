package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

public class InitRegisterUserDevicePayload implements MessagePayload {
    byte[] token;
    UserDevice userDevice;

    public InitRegisterUserDevicePayload(byte[] token, UserDevice userDevice) {
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
