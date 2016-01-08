package de.unipassau.isl.evs.ssh.master.handler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    private final Map<DeviceID, LinkedList<Boolean>> positionMap = new HashMap<>();

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
                positionMap.put(payload.deviceID, new LinkedList<Boolean>());
            }

            List<Boolean> list = positionMap.get(payload.deviceID);
            list.add(0, payload.isLocal);

            if (list.size() > MAX_POSITION_COUNT) {
                list.remove(MAX_POSITION_COUNT);
            }
        }
    }

    public boolean switchedPositionToLocal(DeviceID deviceID) {
        return (positionMap.get(deviceID).get(0) && !positionMap.get(deviceID).get(1));
    }
}
