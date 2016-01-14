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
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_LIGHT_UPDATE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_GET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_GET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_GET_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_SET;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_SET_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_LIGHT_SET_REPLY;

/**
 * AppLightHandler class handles message from and to the
 * {@link de.unipassau.isl.evs.ssh.app.activity.LightFragment LightFragment}
 *
 * @author Phil Werli
 */
public class AppLightHandler extends AbstractAppHandler implements Component {
    public static final Key<AppLightHandler> KEY = new Key<>(AppLightHandler.class);

    private static final long REFRESH_DELAY_MILLIS = TimeUnit.MINUTES.toMillis(2);
    private final List<LightHandlerListener> listeners = new ArrayList<>();
    private final Map<Module, LightStatus> lightStatusMapping = new HashMap<>();

    final private AppModuleHandler.AppModuleListener listener = new AppModuleHandler.AppModuleListener() {
        @Override
        public void onModulesRefreshed() {
            update();
        }
    };

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_LIGHT_SET_ERROR,
                MASTER_LIGHT_SET_REPLY,
                MASTER_LIGHT_GET_ERROR,
                MASTER_LIGHT_GET_REPLY,
                APP_LIGHT_UPDATE
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (!tryHandleResponse(message)) {
            if (MASTER_LIGHT_SET_REPLY.matches(message)) {
                LightPayload payload = MASTER_LIGHT_SET_REPLY.getPayload(message);
                setCachedStatus(payload.getModule(), payload.getOn());
                fireLightSetFinished(true);
            } else if (MASTER_LIGHT_GET_REPLY.matches(message)) {
                LightPayload payload = MASTER_LIGHT_GET_REPLY.getPayload(message);
                setCachedStatus(payload.getModule(), payload.getOn());
                fireLightGetFinished(true);
            } else if (MASTER_LIGHT_GET_ERROR.matches(message)) {
                fireLightGetFinished(false);
            } else if (MASTER_LIGHT_SET_ERROR.matches(message)) {
                fireLightSetFinished(false);
            } else if (APP_LIGHT_UPDATE.matches(message)) {
                LightPayload payload = APP_LIGHT_UPDATE.getPayload(message);
                final Module module = payload.getModule();
                setCachedStatus(module, payload.getOn());
                fireStatusChanged(module);
            } else {
                invalidMessage(message);
            }
        }
    }

    @Override
    public void init(Container container) {
        super.init(container);
        requireComponent(AppModuleHandler.KEY).addAppModuleListener(listener);
    }

    @Override
    public void destroy() {
        requireComponent(AppModuleHandler.KEY).removeAppModuleListener(listener);
        super.destroy();
    }

    private void update() {
        lightStatusMapping.clear();
        List<Module> lights = requireComponent(AppModuleHandler.KEY).getLights();
        for (Module m : lights) {
            lightStatusMapping.put(m, new LightStatus(false));
            requestLightStatus(m);
        }
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
        final Future<LightPayload> future = newResponseFuture(sendMessageToMaster(MASTER_LIGHT_GET, message));
        future.addListener(new FutureListener<LightPayload>() {
            @Override
            public void operationComplete(Future<LightPayload> future) throws Exception {
                boolean wasSuccess = future.isSuccess();
                if (wasSuccess) {
                    LightPayload payload = future.get();
                    setCachedStatus(payload.getModule(), payload.getOn());
                }
                fireLightGetFinished(wasSuccess);
            }
        });
    }

    /**
     * Sends a SET-request with the light-module and its status.
     *
     * @param module The light-module which status should be changed.
     * @param status The status of the module.
     */
    private void setLight(Module module, boolean status) {
        LightPayload lightPayload = new LightPayload(status, module);
        Message message = new Message(lightPayload);
        final Future<LightPayload> future = newResponseFuture(sendMessageToMaster(MASTER_LIGHT_SET, message));
        future.addListener(new FutureListener<LightPayload>() {
            @Override
            public void operationComplete(Future<LightPayload> future) throws Exception {
                boolean wasSuccess = future.isSuccess();
                if (wasSuccess) {
                    LightPayload payload = future.get();
                    setCachedStatus(payload.getModule(), payload.getOn());
                }
                fireLightSetFinished(wasSuccess);
            }
        });
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

    private void fireStatusChanged(Module module) {
        for (LightHandlerListener listener : listeners) {
            listener.statusChanged(module);
        }
    }

    private void fireLightSetFinished(boolean wasSuccessful) {
        for (LightHandlerListener listener : listeners) {
            listener.onLightSetFinished(wasSuccessful);
        }
    }

    private void fireLightGetFinished(boolean wasSuccessful) {
        for (LightHandlerListener listener : listeners) {
            listener.onLightGetFinished(wasSuccessful);
        }
    }

    public interface LightHandlerListener {
        void statusChanged(Module module);

        void onLightSetFinished(boolean wasSuccess);

        void onLightGetFinished(boolean wasSuccess);
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
