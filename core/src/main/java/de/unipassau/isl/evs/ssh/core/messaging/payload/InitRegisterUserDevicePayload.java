package de.unipassau.isl.evs.ssh.core.messaging.payload;

public class InitRegisterUserDevicePayload implements MessagePayload {
    byte[] token;
    String groupName;

    public InitRegisterUserDevicePayload(byte[] token, String groupName) {
        this.token = token;
        this.groupName = groupName;
    }

    public byte[] getToken() {
        return token;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
