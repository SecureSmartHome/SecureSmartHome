package de.unipassau.isl.evs.ssh.app.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.SimpleMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_LIGHT_UPDATE;

/**
 * AppLightHandler class handles message from and to the
 * {@link de.unipassau.isl.evs.ssh.app.activity.LightFragment LightFragment}
 *
 * @author Phil Werli
 */
public class AppLightHandler extends SimpleMessageHandler<LightPayload> implements Component {
    public static final Key<AppLightHandler> KEY = new Key<>(AppLightHandler.class);

    private static final String TAG = AppLightHandler.class.getSimpleName();

    private static final long REFRESH_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(2);
    private final List<LightHandlerListener> listeners = new ArrayList<>();
    private final Map<Module, LightStatus> lightStatusMapping = new HashMap<>();
    private AppModuleHandler.AppModuleListener listener = new AppModuleHandler.AppModuleListener() {
        @Override
        public void onModulesRefreshed() {
            update();
        }
    };

    public AppLightHandler() {
        super(APP_LIGHT_UPDATE);
    }

    @Override
    public void init(Container container) {
        super.init(container);
        requireComponent(AppModuleHandler.KEY).addAppModuleListener(listener);
    }

    @Override
    public void destroy() {
        super.destroy();
        requireComponent(AppModuleHandler.KEY).removeAppModuleListener(listener);
    }

    private void update() {
        lightStatusMapping.clear();
        List<Module> lights = requireComponent(AppModuleHandler.KEY).getLights();
        for (Module m : lights) {
            lightStatusMapping.put(m, new LightStatus(false));
            requestLightStatus(m);
        }
    }

    @Override
    protected void handleRouted(Message.AddressedMessage message, LightPayload payload) {
        setCachedStatus(payload.getModule(), payload.getOn());
    }

    /**
     * Changes the light status of a module.
     *
     * @param module The module which status is changed.
     */
    public void toggleLight(Module module) {
        setLight(module, !isLightOn(module));
    }

    private void setCachedStatus(Module module, boolean isOn) {
        LightStatus status = lightStatusMapping.get(module);
        if (status == null) {
            status = new LightStatus(isOn);
            lightStatusMapping.put(module, status);
        } else {
            status.setOn(isOn);
        }
        for (LightHandlerListener listener : listeners) {
            listener.statusChanged(module);
        }
    }

    /**
     * @param module The module which status will be returned.
     * @return The status of the parameter module.
     */
    public boolean isLightOn(Module module) {
        final LightStatus status = lightStatusMapping.get(module);
        if (status == null) {
            requestLightStatus(module);
        } else if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            status.updateTimeStamp();
            requestLightStatus(module);
        }
        return isLightOnCached(module);
    }

    /**
     * Return the light status if already cached.
     *
     * @param module the light module
     * @return <code>true</code> if a light-status is already cached and the light is turned on.
     */
    private boolean isLightOnCached(Module module) {
        final LightStatus status = lightStatusMapping.get(module);
        return status != null && status.isOn();
    }

    /**
     * @return All light modules with its status.
     */
    public Map<Module, LightStatus> getAllLightModuleStates() {
        return Collections.unmodifiableMap(lightStatusMapping);
    }


    /**
     * Sends a GET-request for the status of a module.
     */
    private void requestLightStatus(Module m) {
        LightPayload lightPayload = new LightPayload(false, m);

        Message message = new Message(lightPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_LIGHT_UPDATE.getKey());

        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(RoutingKeys.MASTER_LIGHT_GET, message);
    }

    /**
     * Sends a SET-request with the light-module and its status.
     *
     * @param module The light-module which status should be changed.
     * @param status The status of the module.
     */
    public void setLight(Module module, boolean status) {
        LightPayload lightPayload = new LightPayload(status, module);

        Message message;
        message = new Message(lightPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_LIGHT_UPDATE.getKey());

        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);
        router.sendMessageToMaster(RoutingKeys.MASTER_LIGHT_SET, message);
    }

    /**
     * Adds parameter handler to listeners.
     */
    public void addListener(LightHandlerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes parameter handler from listeners.
     */
    public void removeListener(LightHandlerListener listener) {
        listeners.remove(listener);
    }

    public interface LightHandlerListener {
        void statusChanged(Module module);
    }

    /**
     * Inner class used to save the status of a light.
     */
    public class LightStatus {
        private boolean isOn;
        private long timestamp;

        public LightStatus(boolean isOn) {
            setOn(isOn);
        }

        public boolean isOn() {
            return isOn;
        }

        void setOn(boolean isOn) {
            this.isOn = isOn;
            updateTimeStamp();
        }

        void updateTimeStamp() {
            timestamp = System.currentTimeMillis();
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
