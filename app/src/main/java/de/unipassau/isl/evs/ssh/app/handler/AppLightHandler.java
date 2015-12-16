package de.unipassau.isl.evs.ssh.app.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.LightPayload;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.APP_LIGHT_UPDATE;

/**
 * AppLightHandler class handles message from and to the
 * {@link de.unipassau.isl.evs.ssh.app.activity.LightFragment LightFragment}
 *
 * @author Phil Werli
 */
public class AppLightHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppLightHandler> KEY = new Key<>(AppLightHandler.class);

    private static final long REFRESH_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(20);
    private final List<LightHandlerListener> listeners = new ArrayList<>();
    private final Map<Module, LightStatus> lightStatusMapping = new HashMap<>();

    /**
     * Changes the light status of a module.
     *
     * @param module The module which status is changed.
     */
    public void toggleLight(Module module) {
        setLight(module, !isLightOnCached(module));
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
        if (System.currentTimeMillis() - status.getTimestamp() >= REFRESH_DELAY_MILLIS) {
            requestLightStatus(module);
        }
        return isLightOnCached(module);
    }

    /**
     * Return if a light-status is already cached.
     *
     * @param module the light module
     * @return if a light-status is already cached.
     */
    public boolean isLightOnCached(Module module) {
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
     * Sends a request for the status of a module.
     */
    private void requestLightStatus(Module m) {
        LightPayload lightPayload = new LightPayload(false, m);

        Message message = new Message(lightPayload);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_LIGHT_GET, message);
    }

    /**
     * Sends a SET-request with the light-module and it's status.
     *
     * @param module The light-module which status should be changed.
     * @param status The status of the module.
     */
    public void setLight(Module module, boolean status) {
        LightPayload lightPayload = new LightPayload(status, module);

        Message message;
        message = new Message(lightPayload);
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_LIGHT_UPDATE);

        OutgoingRouter router = getContainer().require(OutgoingRouter.KEY);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_LIGHT_SET, message);
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{APP_LIGHT_UPDATE};
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_LIGHT_UPDATE.matches(message)) {
            LightPayload lightPayload = (LightPayload) message.getPayload();
            setCachedStatus(lightPayload.getModule(), lightPayload.getOn());
        } else {
            invalidMessage(message);
        }
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
            timestamp = System.currentTimeMillis();
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
