/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.app.handler;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.GenerateNewRegisterTokenPayload;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_REGISTER;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_REGISTER_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_USER_REGISTER_REPLY;

/**
 * The AppRegisterNewDeviceHandler handles the messaging to register a new UserDevice.
 *
 * @author Wolfgang Popp
 * @author Leon Sell
 */
public class AppRegisterNewDeviceHandler extends AbstractAppHandler implements Component {
    public static final Key<AppRegisterNewDeviceHandler> KEY = new Key<>(AppRegisterNewDeviceHandler.class);

    private final List<RegisterNewDeviceListener> listeners = new LinkedList<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_USER_REGISTER_REPLY,
                MASTER_USER_REGISTER_ERROR
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (!tryHandleResponse(message)) {
            if (MASTER_USER_REGISTER_REPLY.matches(message)) {
                handleUserRegisterResponse(MASTER_USER_REGISTER_REPLY.getPayload(message));
            } else if (MASTER_USER_REGISTER_ERROR.matches(message)) {
                fireTokenError();
            } else {
                invalidMessage(message);
            }
        }
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

    private void fireTokenResponse(DeviceConnectInformation info) {
        for (RegisterNewDeviceListener listener : listeners) {
            listener.tokenResponse(info);
        }
    }

    private void fireTokenError() {
        for (RegisterNewDeviceListener listener : listeners) {
            listener.tokenError();
        }
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
     * Sends a request message for a token to the master. {@code requestToken()} does not return a Future like other
     * functions that are sending messages. Use the {@code RegisterNewDeviceListener} to get notified when a reply
     * message is handled by this handler.
     *
     * @param user the user who is registered
     */
    public void requestToken(UserDevice user) {
        Message message = new Message(new GenerateNewRegisterTokenPayload(null, user));
        final Future<GenerateNewRegisterTokenPayload> future = newResponseFuture(sendMessageToMaster(MASTER_USER_REGISTER, message));
        future.addListener(new FutureListener<GenerateNewRegisterTokenPayload>() {
            @Override
            public void operationComplete(Future<GenerateNewRegisterTokenPayload> future) throws Exception {
                if (future.isSuccess()) {
                    handleUserRegisterResponse(future.get());
                } else {
                    fireTokenError();
                }
            }
        });
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

        /**
         * Called when the request for a new token failed.
         */
        void tokenError();
    }
}
