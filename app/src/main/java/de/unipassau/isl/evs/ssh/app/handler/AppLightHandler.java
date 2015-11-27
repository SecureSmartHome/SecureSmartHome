package de.unipassau.isl.evs.ssh.app.handler;

import java.util.ArrayList;
import java.util.Collections;
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

/**
 * AppLightHandler class handles message from and to the
 * {@link de.unipassau.isl.evs.ssh.app.activity.LightFragment LightFragment}
 *
 * @author Phil
 */
public class AppLightHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppLightHandler> KEY = new Key<>(AppLightHandler.class);

    private final List<HandlerUpdateListener> listeners = new ArrayList<>();

    private final Map<Module, Boolean> lightStatusMapping = new HashMap<>();

    private long timeStamp;

    private final int REFRESH_DELAY = 20000;

    /**
     * Public constructor which sets the timestamp with the current time.
     */
    public AppLightHandler() {
        timeStamp = System.currentTimeMillis() - REFRESH_DELAY;
        Module m = new Module("TestPlugswitch", new DeviceID("H5f4ahpVmoVL6GKAYqZY7m73k9i9nDCnsiJLbw+0n3E="), CoreConstants.ModuleType.LIGHT, new WLANAccessPoint()); //FIXME resolve DeviceID
        lightStatusMapping.put(m, false);
    }

    private void updateList(List<Module> list) {
        lightStatusMapping.clear();
        for (Module m : list) {
            lightStatusMapping.put(m, false);
        }
    }

    /**
     * Sends a SET-request with the light-module and it's status.
     *
     * @param module The light-module which status should be changed.
     * @param status The status of the module.
     */
    public void switchLight(Module module, boolean status) {
        LightPayload lightPayload = new LightPayload(status, module);

        Message message;
        message = new Message(lightPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_LIGHT_UPDATE);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        NamingManager namingManager = getContainer().require(NamingManager.KEY);
        router.sendMessage(namingManager.getMasterID(), CoreConstants.RoutingKeys.MASTER_LIGHT_SET, message);
    }

    /**
     * Changes the light-status of a module.
     *
     * @param module The module which status is changed.
     */
    public void toggleLight(Module module) {
        if (module != null && lightStatusMapping != null) {
            switchLight(module, !lightStatusMapping.get(module));
        }
    }

    /**
     * @param module The module which status will be returned.
     * @return The status of the parameter module.
     */
    public boolean isModuleOn(Module module) {
        if (System.currentTimeMillis() - timeStamp >= REFRESH_DELAY) {
            for (Module m : lightStatusMapping.keySet()) {
                requestLightStatus(m);
            }
            timeStamp = System.currentTimeMillis();
        }

        return lightStatusMapping.get(module);
    }

    /**
     * Sends a request for the status of a module.
     */
    private void requestLightStatus(Module m) {
        LightPayload lightPayload = new LightPayload(false, m);

        Message message;
        message = new Message(lightPayload);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        NamingManager namingManager = getContainer().require(NamingManager.KEY);
        router.sendMessage(namingManager.getMasterID(), CoreConstants.RoutingKeys.MASTER_LIGHT_GET, message);
    }

    /**
     * @return All light modules with it's statuses.
     */
    public Map<Module, Boolean> getAllLightModuleStates() {
        return Collections.unmodifiableMap(lightStatusMapping);
    }

    /**
     * Updates all registered handlers.
     */
    private void updatePerformed() {
        for (HandlerUpdateListener listener : listeners) {
            listener.updatePerformed();
        }
    }

    /**
     * Adds parameter handler to listeners.
     */
    public void addHandlerUpdateListener(HandlerUpdateListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes parameter handler from listeners.
     */
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

    /**
     * Registers the {@link IncomingDispatcher} as an component.
     */
    @Override
    public void init(Container container) {
        super.init(container);
        requireComponent(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_LIGHT_UPDATE);
    }

    /**
     * Unregisters the {@link IncomingDispatcher} as an component.
     */
    @Override
    public void destroy() {
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_LIGHT_UPDATE);
    }
}
