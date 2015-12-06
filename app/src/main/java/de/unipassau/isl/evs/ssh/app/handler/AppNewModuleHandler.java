package de.unipassau.isl.evs.ssh.app.handler;

import java.util.LinkedList;
import java.util.List;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.AddNewModulePayload;

/**
 * The AppNewModuleHandler handles the messaging needed to register a new ElectronicModule.
 *
 * @author Wolfgang Popp
 */
public class AppNewModuleHandler extends AbstractComponent implements MessageHandler {
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
        String routingKey = message.getRoutingKey();
        if (routingKey.equals(CoreConstants.RoutingKeys.APP_MODULE_ADD)) {
            fireRegistrationFinished(true);
        }
    }

    @Override
    public void init(Container container) {
        super.init(container);
        container.require(IncomingDispatcher.KEY).registerHandler(this, CoreConstants.RoutingKeys.APP_MODULE_ADD);
    }

    @Override
    public void destroy() {
        getComponent(IncomingDispatcher.KEY).unregisterHandler(this, CoreConstants.RoutingKeys.APP_MODULE_ADD);
        super.destroy();
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }

    /**
     * Registers the given module. Invoker of this method can be notified with a NewModuleListener
     * when the registration is finished.
     *
     * @param module the module to register
     */
    public void addNewModule(Module module) {
        AddNewModulePayload payload = new AddNewModulePayload(module);
        OutgoingRouter router = getComponent(OutgoingRouter.KEY);

        Message message = new Message(payload);

        message.putHeader(Message.HEADER_REPLY_TO_KEY, CoreConstants.RoutingKeys.APP_MODULE_ADD);
        router.sendMessageToMaster(CoreConstants.RoutingKeys.MASTER_MODULE_ADD, message);
    }
}
