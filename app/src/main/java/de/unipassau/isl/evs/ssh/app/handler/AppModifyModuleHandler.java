package de.unipassau.isl.evs.ssh.app.handler;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessagePayload;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModifyModulePayload;
import io.netty.util.concurrent.Future;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE_REPLY;

/**
 * The AppModifyModuleHandler handles the messaging needed to register a new ElectronicModule.
 *
 * @author Wolfgang Popp
 */
public class AppModifyModuleHandler extends AbstractAppHandler implements Component {
    public static final Key<AppModifyModuleHandler> KEY = new Key<>(AppModifyModuleHandler.class);

    private List<NewModuleListener> listeners = new LinkedList<>();

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
     * Registers the given module. Invoker of this method can be notified with a NewModuleListener
     * when the registration is finished.
     *
     * @param module the module to register
     */
    public Future<MessagePayload> addNewModule(Module module) {
        ModifyModulePayload payload = new ModifyModulePayload(module);
        return newResponseFuture(sendMessageToMaster(MASTER_MODULE_ADD, new Message(payload)));
    }

    public Future<MessagePayload> removeModule(Module module) {
        ModifyModulePayload payload = new ModifyModulePayload(module);
        return newResponseFuture(sendMessageToMaster(MASTER_MODULE_REMOVE, new Message(payload)));
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

        void unregistrationFinished(boolean wasSuccessful);
    }
}
