package de.unipassau.isl.evs.ssh.app.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.ModuleAccessPoint.WLANAccessPoint;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;

public class AppLightHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppLightHandler> KEY = new Key<>(AppLightHandler.class);

    private final List<HandlerUpdateListener> listeners = new ArrayList<>();

    private final Map<Module, Boolean> lightStatusMapping = new HashMap<>();

    private long timeStamp;

    private final int REFRESH_DELAY = 20000;

    public AppLightHandler() {
        timeStamp = System.currentTimeMillis() - REFRESH_DELAY;
    }

    private void updateList(List<Module> list) {
        lightStatusMapping.clear();
        for (Module m : list) {
            lightStatusMapping.put(m, false);
        }
    }

    public void switchLight(Module module, boolean status) {
        LightPayload lightPayload = new LightPayload(status, module);

        Message message;
        message = new Message(lightPayload);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        NamingManager namingManager = getContainer().require(NamingManager.KEY);
        router.sendMessage(namingManager.getMasterID(), CoreConstants.RoutingKeys.MASTER_LIGHT_SET, message);
    }

    public void toggleLight(Module module) {
        Map<Module, Boolean> map = getAllLightModuleStates();
        if (module != null && map != null) {
            switchLight(module, !map.get(module));
        }
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

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        NamingManager namingManager = getContainer().require(NamingManager.KEY);
        router.sendMessage(namingManager.getMasterID(), CoreConstants.RoutingKeys.MASTER_LIGHT_GET, message);
    }

    public Map<Module, Boolean> getAllLightModuleStates() {
        Map<Module, Boolean> copy = new HashMap<>();
        //fixme delete hardcoded object after tests
        Module m = new Module("TestPlugswitch", new DeviceID("1"), CoreConstants.ModuleType.LIGHT, new WLANAccessPoint());
        if (lightStatusMapping != null) {
            lightStatusMapping.put(m, true);
            for (Module module : lightStatusMapping.keySet()) {
                copy.put(module, lightStatusMapping.get(module));
            }
        }
        return copy;
    }

    private void updatePerformed() {
        for (HandlerUpdateListener listener : listeners) {
            listener.updatePerformed();
        }
    }

    public void addHandlerUpdateListener(HandlerUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeHandlerUpdateListener(HandlerUpdateListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        LightPayload lightPayload = (LightPayload) message.getPayload();
        lightStatusMapping.put(lightPayload.getModule(), lightPayload.getOn());
        updatePerformed();
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }

    @Override
    public void init(Container container) {
        super.init(container);
        requireComponent(IncomingDispatcher.KEY).registerHandler(this, "mzkey");
    }

    @Override
    public void destroy() {
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(this, "mzkey");
    }
}
