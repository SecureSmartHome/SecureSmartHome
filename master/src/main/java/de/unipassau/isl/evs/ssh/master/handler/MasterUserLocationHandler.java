package de.unipassau.isl.evs.ssh.master.handler;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeviceConnectedPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DEVICE_CONNECTED;

/**
 * Handler that keeps information on the position of a User Device (home network or extern)
 * and provides this information to other components.
 *
 * @author Christoph Frädrich
 */
public class MasterUserLocationHandler extends AbstractMasterHandler implements Component {

    public static final Key<MasterUserLocationHandler> KEY = new Key<>(MasterUserLocationHandler.class);

    //We only save the 10 last positions after a connect of a device
    private static final int MAX_POSITION_COUNT = 10;

    //Contains a mapping from boolean to a queue which contains information whether the device
    //connected to local
    private final Map<DeviceID, LinkedList<Message.AddressedMessage>> positionMap = new HashMap<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[] {
                MASTER_DEVICE_CONNECTED
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (RoutingKeys.MASTER_DEVICE_CONNECTED.matches(message)) {
            DeviceConnectedPayload payload = RoutingKeys.MASTER_DEVICE_CONNECTED.getPayload(message);

            if (!positionMap.containsKey(payload.deviceID)) {
                positionMap.put(payload.deviceID, new LinkedList<Message.AddressedMessage>());
            }

            List<Message.AddressedMessage> list = positionMap.get(payload.deviceID);
            list.add(0, message);

            if (list.size() > MAX_POSITION_COUNT) {
                list.remove(MAX_POSITION_COUNT);
            }
        }
    }

    /**
     * Checks if the given Device switched from extern to local within the last 'intervall' minutes
     *
     * @param deviceID of the device to be checked
     * @param intervall in which the change has happened in min
     *
     * @return true if change to local happened in the last 'intervall' min.
     */
    public boolean switchedPositionToLocal(DeviceID deviceID, int intervall) {
        if (positionMap.get(deviceID) == null || positionMap.get(deviceID).size() >= 2) {
            return false;
        }

        Message.AddressedMessage lastMessage = positionMap.get(deviceID).get(0);
        Message.AddressedMessage preLastMessage = positionMap.get(deviceID).get(1);

        //Is last location local and was last outside of home network and was that change in the last 2 min?
        if (MASTER_DEVICE_CONNECTED.getPayload(lastMessage).isLocal
                && !MASTER_DEVICE_CONNECTED.getPayload(preLastMessage).isLocal
                && lastMessage.getHeaders().get(Message.HEADER_TIMESTAMP) - System.currentTimeMillis() > TimeUnit.MINUTES.toMillis(intervall)) {
            return true;
        }

        return false;
    }

    /**
     *
     * Returns if a device is currently in local network
     *
     * @param deviceID of the device that is to be checked
     * @return true if device is in local network
     */
    public boolean isDeviceLocal(DeviceID deviceID) {
        return MASTER_DEVICE_CONNECTED.getPayload(positionMap.get(deviceID).get(0)).isLocal;
    }
}
