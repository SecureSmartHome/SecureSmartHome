package de.unipassau.isl.evs.ssh.app.handler;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;

/**
 * @author Wolfgang Popp.
 */
public class AppRegisterNewDeviceHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppRegisterNewDeviceHandler> KEY = new Key<>(AppRegisterNewDeviceHandler.class);

    private List<RegisterNewDeviceListener> listeners = new LinkedList<>();

    public interface RegisterNewDeviceListener {
        void tokenResponse(QRDeviceInformation qrDeviceInformation);
    }

    public void addRegisterDeviceListener(RegisterNewDeviceListener listener) {
        listeners.add(listener);
    }

    public void removeRegisterDeviceListener(RegisterNewDeviceListener listener) {
        listeners.remove(listener);
    }

    private void fireTokenResponse(QRDeviceInformation info) {
        for (RegisterNewDeviceListener listener : listeners) {
            listener.tokenResponse(info);
        }
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        // TODO
        //...

        //fireTokenResponse(qrDevInfo);
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
        getComponent(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_USER_REGISTER);
    }

    @Override
    public void destroy() {
        getComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_USER_REGISTER);
        super.destroy();
    }

    public void requestToken(UserDevice user) {
        //TODO
    }
}
