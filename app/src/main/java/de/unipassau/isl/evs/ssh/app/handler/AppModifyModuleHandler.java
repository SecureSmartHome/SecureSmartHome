package de.unipassau.isl.evs.ssh.app.handler;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModifyModulePayload;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD_ERROR;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD_REPLY;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_REMOVE_REPLY;

/**
 * The AppModifyModuleHandler handles the messaging needed to register a new ElectronicModule.
 *
 * @author Wolfgang Popp
 */
public class AppModifyModuleHandler extends AbstractMessageHandler implements Component {
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

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_MODULE_ADD_REPLY.matches(message)) {
            fireRegistrationFinished(true);
        } else if (MASTER_MODULE_ADD_ERROR.matches(message)) {
            fireRegistrationFinished(false);
        } else if (MASTER_MODULE_REMOVE_REPLY.matches(message)) {
            // FIXME Wolfgang: Uncomment? (Phil, 8-1-16)
            // fireRegistrationFinished(false);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                MASTER_MODULE_ADD_REPLY,
                MASTER_MODULE_ADD_ERROR,
                MASTER_MODULE_REMOVE_REPLY
        };
    }

    /**
     * Registers the given module. Invoker of this method can be notified with a NewModuleListener
     * when the registration is finished.
     *
     * @param module the module to register
     */
    public void addNewModule(Module module) {
        ModifyModulePayload payload = new ModifyModulePayload(module);
        sendMessageToMaster(MASTER_MODULE_ADD, new Message(payload));
    }

    public void removeModule(Module module){
        ModifyModulePayload payload = new ModifyModulePayload(module);
        sendMessageToMaster(MASTER_MODULE_REMOVE, new Message(payload));
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
    }
}
