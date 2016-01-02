package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.channel.Channel;

/**
 * @author Christoph Fraedrich
 */
public class DeviceConnectedPayload implements MessagePayload {
    public final DeviceID deviceID;
    public final Channel channel;
    public final boolean isLocal;

    public DeviceConnectedPayload(DeviceID deviceID, Channel channel, boolean isLocal) {
        this.deviceID = deviceID;
        this.channel = channel;
        this.isLocal = isLocal;
    }
}
