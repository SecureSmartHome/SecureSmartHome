package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.NotificationPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.SystemHealthPayload;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.Permission.BinaryPermission.SYSTEM_HEALTH_WARNING;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_NOTIFICATION_SEND;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_SYSTEM_HEALTH_CHECK;

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
            if (((SystemHealthPayload) message.getPayload()).getHasError()) {
                String name = ((SystemHealthPayload) message.getPayload()).getModule().getName();
                MessagePayload payload = new NotificationPayload(SYSTEM_HEALTH_WARNING.toString(), "Error at module: " + name);
                sendMessageLocal(MASTER_NOTIFICATION_SEND, new Message(payload));
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