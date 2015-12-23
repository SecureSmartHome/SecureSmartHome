package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import io.netty.channel.Channel;

/**
 * Payload which is used to signal (within the master) that a device has connected.
 *
 * @author Christoph Fraedrich
 */
public class DeviceConnectedPayload implements MessagePayload {

    public final DeviceID deviceID;
    public final Channel channel;

    public DeviceConnectedPayload(DeviceID deviceID, Channel channel) {
        this.deviceID = deviceID;
        this.channel = channel;
    }
}
