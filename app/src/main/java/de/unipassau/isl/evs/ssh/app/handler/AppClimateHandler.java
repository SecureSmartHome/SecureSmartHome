package de.unipassau.isl.evs.ssh.app.handler;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;

/**
 * AppLightHandler class handles message from and to the
 * {@link de.unipassau.isl.evs.ssh.app.activity.ClimateFragment ClimateFragment}
 *
 * @author bucher
 */
public class AppClimateHandler extends AbstractComponent implements MessageHandler {

    private double temp1 = 0.0;
    private double temp2 = 0.0;
    private double pressure = 0.0;
    private double altitude = 0.0;
    private double humidity = 0.0;
    private double uv = 0.0;
    private int visible = 0;
    private int ir = 0;

    public static final Key<AppClimateHandler> KEY = new Key<>(AppClimateHandler.class);

    private final List<ClimateHandlerListener> listeners = new ArrayList<>();
    private static final long REFRESH_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(200);


    public interface ClimateHandlerListener {
        void statusChanged(Module module);
    }


    //Lifecycle & Callbacks/////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Registers the {@link IncomingDispatcher} as an component.
     */
    @Override
    public void init(Container container) {
        super.init(container);
        requireComponent(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_LIGHT_UPDATE);
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (message.getPayload() instanceof ModulesPayload) {
            List<> modules = (List<Module>) message.getPayload();
            updateList(modules);
        } else {
            Log.e(this.getClass().getSimpleName(), "Error! Unknown message Payload");
        }
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    /**
     * Adds parameter handler to listeners.
     */
    public void addListener(ClimateHandlerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes parameter handler from listeners.
     */
    public void removeListener(AppClimateHandler.ClimateHandlerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Unregisters the {@link IncomingDispatcher} as an component.
     */
    @Override
    public void destroy() {
        requireComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_CLIMATE_UPDATE);
    }

}
