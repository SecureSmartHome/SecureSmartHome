/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.master.handler;

import android.util.Log;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeviceConnectedPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.master.network.Server;
import io.netty.channel.Channel;

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
    private final ListMultimap<DeviceID, Record> positionMap = ArrayListMultimap.create();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_DEVICE_CONNECTED
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (RoutingKeys.MASTER_DEVICE_CONNECTED.matches(message)) {
            final DeviceConnectedPayload payload = RoutingKeys.MASTER_DEVICE_CONNECTED.getPayload(message);

            final List<Record> list = positionMap.get(payload.getDeviceID());
            if (list.size() <= 1 || list.get(0).isLocal != payload.isLocal()) {
                list.add(0, new Record(payload.isLocal()));
            }
            while (list.size() > MAX_POSITION_COUNT) {
                list.remove(list.size() - 1);
            }
        }
    }

    /**
     * Checks if the given Device switched from extern to local within the last 'interval' minutes
     *
     * @param deviceID of the device to be checked
     * @param interval in which the change has happened in min
     * @return true if change to local happened in the last 'interval' min.
     */
    public boolean switchedPositionToLocal(DeviceID deviceID, int interval) {
        if (positionMap.get(deviceID).size() < 2) {
            return false;
        }

        final Record lastMessage = positionMap.get(deviceID).get(0);
        final Record preLastMessage = positionMap.get(deviceID).get(1);

        //Is last location local and was last outside of home network and was that change in the last 2 min?
        boolean result = lastMessage.isLocal && !preLastMessage.isLocal
                && System.currentTimeMillis() - lastMessage.timestamp < TimeUnit.MINUTES.toMillis(interval);

        Log.v(getClass().getSimpleName(), "switchedPositionToLocal=" + result + "\n\t last Message was " + lastMessage
                + "\n\t pre last Message was " + preLastMessage + "\n\t at timestamp " + System.currentTimeMillis());
        return result;

    }

    /**
     * Returns if a device is currently in local network
     *
     * @param deviceID of the device that is to be checked
     * @return true if device is in local network
     */
    public boolean isDeviceLocal(DeviceID deviceID) {
        final Channel channel = requireComponent(Server.KEY).findChannel(deviceID);
        if (channel != null) {
            return channel.isOpen() && channel.attr(CoreConstants.NettyConstants.ATTR_LOCAL_CONNECTION).get();
        } else {
            return false;
        }
    }

    private static class Record {
        private final boolean isLocal;
        private final long timestamp = System.currentTimeMillis();

        private Record(boolean isLocal) {
            this.isLocal = isLocal;
        }

        @Override
        public String toString() {
            return "Record{" +
                    "isLocal=" + isLocal +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}