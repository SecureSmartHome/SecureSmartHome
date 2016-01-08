package de.unipassau.isl.evs.ssh.master.handler;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SystemHealthPayload;
import de.unipassau.isl.evs.ssh.master.network.NotificationBroadcaster;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_NOTIFICATION_SEND;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.SYSTEM_HEALTH_WARNING;

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
            // TODO: what happens when there's no error? (07.01.16, Leon)
            if (payload.getHasError()) {
                Serializable name = payload.getModule().getName();
                NotificationBroadcaster notificationBroadcaster = new NotificationBroadcaster();
                notificationBroadcaster.sendMessageToAllReceivers(NotificationPayload.NotificationType.SYSTEM_HEALTH_WARNING, name);
            }
            //TODO propagate message that shows that a modul is accessible again
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{MASTER_SYSTEM_HEALTH_CHECK};
    }
}