package de.unipassau.isl.evs.ssh.core.messaging.payload;

public class DoorStatusPayload implements MessagePayload {
    boolean closed;
    String moduleName;

    public DoorStatusPayload(boolean closed, String moduleName) {
        this.closed = closed;
        this.moduleName = moduleName;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}