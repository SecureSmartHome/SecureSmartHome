package de.unipassau.isl.evs.ssh.app.handler;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GenerateNewRegisterTokenPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.APP_USER_REGISTER;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.RoutingKeys.MASTER_USER_REGISTER;

/**
 * The AppRegisterNewDeviceHandler handles the messaging to register a new UserDevice.
 *
 * @author Wolfgang Popp
 * @author Leon Sell
 */
public class AppRegisterNewDeviceHandler extends AbstractComponent implements MessageHandler {
    public static final Key<AppRegisterNewDeviceHandler> KEY = new Key<>(AppRegisterNewDeviceHandler.class);

    private List<RegisterNewDeviceListener> listeners = new LinkedList<>();

    /**
     * The listener to receive registration events
     */
    public interface RegisterNewDeviceListener {
        /**
         * Called when the token for the new UserDevice was received and the QR Code can be
         * displayed now.
         *
         * @param qrDeviceInformation the QR-Code information to display on the admin's screen
         */
        void tokenResponse(QRDeviceInformation qrDeviceInformation);
    }

    /**
     * Adds the given listener to this handler.
     *
     * @param listener the listener to add
     */
    public void addRegisterDeviceListener(RegisterNewDeviceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given listener from this handler.
     *
     * @param listener the listener to remove
     */
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
        if (message.getPayload() instanceof GenerateNewRegisterTokenPayload) {
            switch (message.getRoutingKey()) {
                case APP_USER_REGISTER:
                    handleUserRegisterResponse(message);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported routing key: " + message.getRoutingKey()
                            + " for GenerateNewRegisterTokenPayload");
            }
        }
    }

    private void handleUserRegisterResponse(Message.AddressedMessage message) {
        GenerateNewRegisterTokenPayload generateNewRegisterTokenPayload =
                (GenerateNewRegisterTokenPayload) message.getPayload();
        final SharedPreferences prefs = getContainer().require(ContainerService.KEY_CONTEXT)
                .getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, Context.MODE_PRIVATE);
        final NamingManager namingManager = getComponent(NamingManager.KEY);

        String host = prefs.getString(CoreConstants.NettyConstants.PREF_HOST, null);
        int port = prefs.getInt(CoreConstants.NettyConstants.PREF_PORT, 0);
        Inet4Address address;
        try {
            address = ((Inet4Address) InetAddress.getByName(host));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to convert host address from shared prefs to "
                    + "an Inet4Address", e);
        }
        QRDeviceInformation qrDevInfo = new QRDeviceInformation(address, port, namingManager.getMasterID(),
                generateNewRegisterTokenPayload.getToken());
        fireTokenResponse(qrDevInfo);
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
        getComponent(IncomingDispatcher.KEY).registerHandler(this, APP_USER_REGISTER);
    }

    @Override
    public void destroy() {
        getComponent(IncomingDispatcher.KEY).unregisterHandler(this, APP_USER_REGISTER);
        super.destroy();
    }

    /**
     * Sends a request message for a token to the master.
     *
     * @param user
     */
    public void requestToken(UserDevice user) {
        Message message = new Message(new GenerateNewRegisterTokenPayload(null, user));
        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_USER_REGISTER);
        final OutgoingRouter outgoingRouter = getComponent(OutgoingRouter.KEY);
        if (outgoingRouter != null) {
            outgoingRouter.sendMessageToMaster(MASTER_USER_REGISTER, message);
        } //Todo: what do when no container?
    }
}
