package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * In case a hardware button on the Odroid is pressed, a message, of which the content depends on
 * what button pressed, is generated. The message is then passed to the OutgoingRouter.
 * <p/>
 * An example for such a scenario would be if the "Reset" button is pressed.
 * Then a message containing the reset command is generated an passed to the OutgoingRouter
 * and from there sent on to the target handler, which eventually,
 * will result in a reset of the whole system.
 */
public class MasterButtonHandler implements Handler {
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }
}