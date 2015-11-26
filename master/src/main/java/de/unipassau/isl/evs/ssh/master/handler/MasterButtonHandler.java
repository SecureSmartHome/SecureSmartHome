package de.unipassau.isl.evs.ssh.master.handler;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;
import de.unipassau.isl.evs.ssh.master.database.DatabaseContract;
import de.unipassau.isl.evs.ssh.master.database.HolidayController;
import de.unipassau.isl.evs.ssh.master.database.PermissionController;
import de.unipassau.isl.evs.ssh.master.database.SlaveController;

/**
 * In case a hardware button on the Odroid is pressed, a message, of which the content depends on
 * what button pressed, is generated. The message is then passed to the OutgoingRouter.
 * <p/>
 * An example for such a scenario would be if the "Reset" button is pressed.
 * Then a message containing the reset command is generated an passed to the OutgoingRouter
 * and from there sent on to the target handler, which eventually,
 * will result in a reset of the whole system.
 * @author leon
 */
public class MasterButtonHandler extends AbstractMasterHandler {
}