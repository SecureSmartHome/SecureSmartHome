package de.unipassau.isl.evs.ssh.master.handler;

import java.util.HashMap;
import java.util.Map;

import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.OutgoingRouter;
import de.unipassau.isl.evs.ssh.core.messaging.payload.MessageErrorPayload;

/**
 * This is a MasterHandler providing functionality all MasterHandlers need. This will avoid needing to implement the
 * same functionality over and over again.
 * @author leon
 */
public abstract class AbstractMasterHandler implements MessageHandler {
    protected IncomingDispatcher incomingDispatcher;
    private Map<Integer, Message.AddressedMessage> inbox = new HashMap<>();
    private Map<Integer, Integer> onBehalfOfMessage = new HashMap<>();

    @Override
    public void handle(Message.AddressedMessage message) {
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {
        incomingDispatcher = dispatcher;
    }

    @Override
    public void handlerRemoved(String routingKey) {
    }

    /**
     * Save a Message to be able to respond to Messages from the app after requesting information from the slave.
     * @param message Message to save.
     */
    protected void saveMessage(Message.AddressedMessage message) {
        //Todo clear sometime.
        inbox.put(message.getSequenceNr(), message);
    }

    /**
     * Get a saved Message.
     * @param sequenceNr Sequence number of the requested Message.
     * @return Requested message.
     */
    protected Message.AddressedMessage getMessage(int sequenceNr) {
        return inbox.get(sequenceNr);
    }

    /**
     * Get saved Message by sequence number of the Message send on behalf of the saved Message.
     * @param sequenceNr Sequence number of the Message send on behalf of the saved Message.
     * @return Requested Message.
     */
    protected Message.AddressedMessage getMessageOnBehalfOfSequenceNr(int sequenceNr) {
        return getMessage(getSequenceNrOnBehalfOfSequenceNr(sequenceNr));
    }

    /**
     * Make a connection between a received Message sequence number and the Message sequence number of the Message send
     * on behalf of that Message.
     * @param newMessageSequenceNr Message sequence number of the Message send on behalf of the Message.
     * @param onBehalfOfMessageSequenceNr Message sequence number of the original Message.
     */
    protected void putOnBehalfOf(int newMessageSequenceNr, int onBehalfOfMessageSequenceNr) {
        onBehalfOfMessage.put(newMessageSequenceNr, onBehalfOfMessageSequenceNr);

    }

    /**
     * Request the Message sequence number of a previously made connection between Message sequence numbers.
     * @param sequenceNr The Message sequence number of the Message send on behalf of the requested Message.
     * @return Requested Message's sequence number..
     */
    protected Integer getSequenceNrOnBehalfOfSequenceNr(int sequenceNr) {
        return onBehalfOfMessage.get(sequenceNr);
    }

    /**
     * Respond with an error message to a given AddressedMessage.
     * @param original Original Message.
     */
    protected void sendErrorMessage(Message.AddressedMessage original) {
        Message reply = new Message(new MessageErrorPayload(original.getPayload()));
        reply.putHeader(Message.HEADER_REFERENCES_ID, original.getSequenceNr());
        incomingDispatcher.getContainer().require(OutgoingRouter.KEY).sendMessage(original.getFromID(),
                original.getHeader(Message.HEADER_REPLY_TO_KEY), reply);
    }
}
