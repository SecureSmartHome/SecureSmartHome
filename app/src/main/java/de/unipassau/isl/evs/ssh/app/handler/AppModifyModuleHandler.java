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

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModifyModulePayload;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE_REPLY;

/**
 * The AppModifyModuleHandler handles the messaging needed to register and remove a ElectronicModule.
 *
 * @author Wolfgang Popp
 */
public class AppModifyModuleHandler extends AbstractAppHandler implements Component {
    public static final Key<AppModifyModuleHandler> KEY = new Key<>(AppModifyModuleHandler.class);

    private final List<NewModuleListener> listeners = new LinkedList<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_MODULE_ADD_REPLY,
                MASTER_MODULE_ADD_ERROR,
                MASTER_MODULE_REMOVE_REPLY,
                MASTER_MODULE_REMOVE_ERROR
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (!tryHandleResponse(message)) {
            if (MASTER_MODULE_ADD_REPLY.matches(message)) {
                fireRegistrationFinished(true);
            } else if (MASTER_MODULE_ADD_ERROR.matches(message)) {
                fireRegistrationFinished(false);
            } else if (MASTER_MODULE_REMOVE_REPLY.matches(message)) {
                fireUnregistrationFinished(true);
            } else if (MASTER_MODULE_REMOVE_ERROR.matches(message)) {
                fireUnregistrationFinished(false);
            } else {
                invalidMessage(message);
            }
        }
    }

    /**
     * Adds a new NewModuleListener to this handler.
     *
     * @param listener the listener to add
     */
    public void addNewModuleListener(AppModifyModuleHandler.NewModuleListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given NewModuleListener from this handler.
     *
     * @param listener the listener to remove
     */
    public void removeNewModuleListener(AppModifyModuleHandler.NewModuleListener listener) {
        listeners.remove(listener);
    }

    private void fireRegistrationFinished(boolean wasSuccessful) {
        for (NewModuleListener listener : listeners) {
            listener.registrationFinished(wasSuccessful);
        }
    }

    private void fireUnregistrationFinished(boolean wasSuccessful) {
        for (NewModuleListener listener : listeners) {
            listener.unregistrationFinished(wasSuccessful);
        }
    }

    /**
     * Registers the given module. Invoker of this method can be notified with a NewModuleListener
     * when the registration is finished.
     *
     * @param module the module to register
     */
    public void addNewModule(Module module) {
        ModifyModulePayload payload = new ModifyModulePayload(module);
        final Future<Void> future = newResponseFuture(sendMessageToMaster(MASTER_MODULE_ADD, new Message(payload)));
        future.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                fireRegistrationFinished(future.isSuccess());
            }
        });
    }

    /**
     * Removes the given module. Invoker of this method can be notified with a NewModuleListener
     * when this action finished.
     *
     * @param module the module to remove
     */
    public void removeModule(Module module) {
        ModifyModulePayload payload = new ModifyModulePayload(module);
        final Future<Void> future = newResponseFuture(sendMessageToMaster(MASTER_MODULE_REMOVE, new Message(payload)));
        future.addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                fireUnregistrationFinished(future.isSuccess());
            }
        });
    }

    /**
     * The listener interface to be notified, when the registration of a new ElectronicModule
     * finished.
     */
    public interface NewModuleListener {
        /**
         * Invoked when the registration of a new ElectronicModule finished.
         *
         * @param wasSuccessful indicates whether the registration was successful or not
         */
        void registrationFinished(boolean wasSuccessful);

        /**
         * Invoked when the deletion of a ElectronicModule finished.
         *
         * @param wasSuccessful indicates whether the deletion was successful or not
         */
        void unregistrationFinished(boolean wasSuccessful);
    }
}
