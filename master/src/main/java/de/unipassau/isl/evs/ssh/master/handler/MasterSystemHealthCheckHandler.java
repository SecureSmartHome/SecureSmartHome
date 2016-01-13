package de.unipassau.isl.evs.ssh.master.handler;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SystemHealthPayload;
import de.unipassau.isl.evs.ssh.master.network.broadcast.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK;
import static de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload.NotificationType.SYSTEM_HEALTH_WARNING;

/**
 * Handler that periodically checks if hardware system components are still active and working properly.
 * In case of malfunctioning hardware notifications are issued.
 *
 * @author Christoph Fraedrich
 */
public class MasterSystemHealthCheckHandler extends AbstractMasterHandler {
    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_SYSTEM_HEALTH_CHECK.matches(message)) {
            final SystemHealthPayload payload = MASTER_SYSTEM_HEALTH_CHECK.getPayload(message);
            final NotificationBroadcaster notificationBroadcaster = requireComponent(NotificationBroadcaster.KEY);
            final Serializable name = payload.getModule().getName();

            if (payload.getHasError()) {
                notificationBroadcaster.sendMessageToAllReceivers(SYSTEM_HEALTH_WARNING, true, name);
            } else {
                notificationBroadcaster.sendMessageToAllReceivers(SYSTEM_HEALTH_WARNING, false, name);
            }
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_SYSTEM_HEALTH_CHECK};
    }
}