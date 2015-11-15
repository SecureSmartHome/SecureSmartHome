package de.unipassau.isl.evs.ssh.slave.handler;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Handles messages indicating that information of a device needs to be updated and writes these changes to the routing table.
 */
public class SlaveRoutingTableHandler implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}