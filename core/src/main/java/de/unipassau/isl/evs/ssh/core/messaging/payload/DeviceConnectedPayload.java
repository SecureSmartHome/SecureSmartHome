package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.channel.Channel;

/**
 * Payload which is used to signal (within the master) that a device has connected.
 *
 * @author Christoph Fraedrich
 */
public class DeviceConnectedPayload implements MessagePayload {
    private final DeviceID deviceID;
    private final Channel channel;
    private final boolean isLocal;

    public DeviceConnectedPayload(DeviceID deviceID, Channel channel, boolean isLocal) {
        this.deviceID = deviceID;
        this.channel = channel;
        this.isLocal = isLocal;
    }

    public DeviceID getDeviceID() {
        return deviceID;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isLocal() {
        return isLocal;
    }
}
