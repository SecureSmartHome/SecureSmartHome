package de.unipassau.isl.evs.ssh.slave.handler;

import android.test.InstrumentationTestCase;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.container.SimpleContainer;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.mock.network.ClientMock;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.network.ClientIncomingDispatcher;

/**
 * Testclass for SlaveLightHandlerTest
 *
 * @author Chris
 */
public class SlaveLightHandlerTest extends InstrumentationTestCase {

    public void testHandleOn() {
        SimpleContainer container = new SimpleContainer();
        container.register(ContainerService.KEY_CONTEXT,
                new ContainerService.ContextComponent(getInstrumentation().getTargetContext()));

        IncomingDispatcher dispatcher = new ClientIncomingDispatcher(new ClientMock());

        container.register(dispatcher.KEY, dispatcher);

        SlaveLightHandler handler = new SlaveLightHandler();
        dispatcher.registerHandler(handler, "SlaveLightHandler");

        MessagePayload pld = new LightPayload(true);
        Message msg = new Message(pld);

        Message.AddressedMessage aMsg = msg.setDestination(new DeviceID("1"),
                new DeviceID("2"), "SlaveLightHandler");
        handler.handle(aMsg);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        pld = new LightPayload(false);
        msg = new Message(pld);

        aMsg = msg.setDestination(new DeviceID("1"),
                new DeviceID("2"), "SlaveLightHandler");
        handler.handle(aMsg);
    }
}