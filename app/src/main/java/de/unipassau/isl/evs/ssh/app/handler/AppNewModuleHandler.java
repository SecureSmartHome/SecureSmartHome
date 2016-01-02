package de.unipassau.isl.evs.ssh.app.handler;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.AddNewModulePayload;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.APP_MODULE_ADD;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_MODULE_ADD;

/**
 * The AppNewModuleHandler handles the messaging needed to register a new ElectronicModule.
 *
 * @author Wolfgang Popp
 */
public class AppNewModuleHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppNewModuleHandler> KEY = new Key<>(AppNewModuleHandler.class);

    private List<NewModuleListener> listeners = new LinkedList<>();

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

    /**
     * Adds a new NewModuleListener to this handler.
     *
     * @param listener the listener to add
     */
    public void addNewModuleListener(AppNewModuleHandler.NewModuleListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given NewModuleListener from this handler.
     *
     * @param listener the listener to remove
     */
    public void removeNewModuleListener(AppNewModuleHandler.NewModuleListener listener) {
        listeners.remove(listener);
    }

    private void fireRegistrationFinished(boolean wasSuccessful) {
        for (NewModuleListener listener : listeners) {
            listener.registrationFinished(wasSuccessful);
        }
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (APP_MODULE_ADD.matches(message)) {
            fireRegistrationFinished(true);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{APP_MODULE_ADD};
    }

    /**
     * Registers the given module. Invoker of this method can be notified with a NewModuleListener
     * when the registration is finished.
     *
     * @param module the module to register
     */
    public void addNewModule(Module module) {
        AddNewModulePayload payload = new AddNewModulePayload(module);
        OutgoingRouter router = requireComponent(OutgoingRouter.KEY);

        Message message = new Message(payload);

        message.putHeader(Message.HEADER_REPLY_TO_KEY, APP_MODULE_ADD.getKey());
        router.sendMessageToMaster(MASTER_MODULE_ADD, message);
    }
}
