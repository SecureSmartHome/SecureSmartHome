package de.unipassau.isl.evs.ssh.app.handler;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GenerateNewRegisterTokenPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_USER_REGISTER;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_REGISTER;

/**
 * The AppRegisterNewDeviceHandler handles the messaging to register a new UserDevice.
 *
 * @author Wolfgang Popp
 * @author Leon Sell
 */
public class AppRegisterNewDeviceHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppRegisterNewDeviceHandler> KEY = new Key<>(AppRegisterNewDeviceHandler.class);

    private List<RegisterNewDeviceListener> listeners = new LinkedList<>();

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

    private void fireTokenResponse(DeviceConnectInformation info) {
        for (RegisterNewDeviceListener listener : listeners) {
            listener.tokenResponse(info);
        }
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_USER_REGISTER.matches(message)) {
            handleUserRegisterResponse(APP_USER_REGISTER.getPayload(message));
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{APP_USER_REGISTER};
    }

    private void handleUserRegisterResponse(GenerateNewRegisterTokenPayload generateNewRegisterTokenPayload) {
        final NamingManager namingManager = requireComponent(NamingManager.KEY);
        final Client client = requireComponent(Client.KEY);
        InetSocketAddress address = client.getAddress();
        if (address == null) {
            address = client.getConnectAddress();
        }
        if (address == null) {
            throw new IllegalStateException("Client not connected");
        }
        DeviceConnectInformation qrDevInfo = new DeviceConnectInformation(
                address.getAddress(),
                address.getPort(),
                namingManager.getMasterID(),
                generateNewRegisterTokenPayload.getToken()
        );
        fireTokenResponse(qrDevInfo);
    }

    /**
     * Sends a request message for a token to the master.
     *
     * @param user the user who is registered
     */
    public void requestToken(UserDevice user) {
        Message message = new Message(new GenerateNewRegisterTokenPayload(null, user));
        sendMessageToMaster(MASTER_USER_REGISTER, message);
    }

    /**
     * The listener to receive registration events
     */
    public interface RegisterNewDeviceListener {
        /**
         * Called when the token for the new UserDevice was received and the QR Code can be
         * displayed now.
         *
         * @param deviceConnectInformation the QR-Code information to display on the admin's screen
         */
        void tokenResponse(DeviceConnectInformation deviceConnectInformation);
    }
}
