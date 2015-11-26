package de.unipassau.isl.evs.ssh.app.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;

public class AppLightHandler extends AbstractComponent implements MessageHandler {
    private Container container;
    private IncomingDispatcher dispatcher;
    /**
     *
     */
    private Map<Module, Boolean> lightStatusMapping;
    /**
     *
     */
    private long timeStamp;
    /**
     *
     */
    private final int REFRESH_DELAY = 20000;

    public AppLightHandler(List<Module> list) {
        lightStatusMapping = new HashMap<Module, Boolean>();
        for (Module m : list) {
            lightStatusMapping.put(m, false);
        }
        timeStamp = System.currentTimeMillis() - REFRESH_DELAY;
    }

    public boolean isModuleOn(Module m) {
        if (System.currentTimeMillis() - timeStamp >= REFRESH_DELAY) {
            for (Module module : lightStatusMapping.keySet()) {
                requestLightStatus(module);
            }
            timeStamp = System.currentTimeMillis();
        }
        return lightStatusMapping.get(m);
    }

    private void requestLightStatus(Module m) {
        LightPayload lightPayload = new LightPayload(false, m);

        Message message;
        message = new Message(lightPayload);

        OutgoingRouter router = dispatcher.getContainer().require(OutgoingRouter.KEY);
        NamingManager namingManager = dispatcher.getContainer().require(NamingManager.KEY);
        router.sendMessage(namingManager.getMasterID(), CoreConstants.RoutingKeys.MASTER_LIGHT_GET, message);
    }

    public Map<Module, Boolean> getAllLightModuleStates() {
        Map copy = new HashMap<Module, Boolean>();
        for (Module module : lightStatusMapping.keySet()) {
            copy.put(module, lightStatusMapping.get(module));
        }
        return copy;
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        LightPayload lightPayload = (LightPayload) message.getPayload();
        lightStatusMapping.put(lightPayload.getModule(), lightPayload.getOn());
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        this.dispatcher = dispatcher;

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    @Override
    public void init(Container container) {
        super.init(container);
        this.container = container;
    }

    @Override
    public void destroy() {

    }
}
