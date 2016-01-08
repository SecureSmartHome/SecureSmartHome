package de.unipassau.isl.evs.ssh.core.messaging.payload;

import android.content.Context;

/**
 * Payload sent in a reply message if a requested exception fails.
 * Extends Exception so that it can be thrown as a whole on the receiver side.
 *
 * @author Niko Sell
 */
public class ErrorPayload extends Exception implements MessagePayload {
    private int messageResourceId;

    public ErrorPayload() {
    }

    public ErrorPayload(String detailMessage) {
        super(detailMessage);
    }

    public ErrorPayload(Throwable throwable, String detailMessage) {
        super(detailMessage, throwable);
    }

    public ErrorPayload(Throwable throwable) {
        super(throwable);
    }

    public ErrorPayload(int messageResourceId) {
        this.messageResourceId = messageResourceId;
    }

    public ErrorPayload(String detailMessage, int messageResourceId) {
        super(detailMessage);
        this.messageResourceId = messageResourceId;
    }

    public ErrorPayload(Throwable throwable, String detailMessage, int messageResourceId) {
        super(detailMessage, throwable);
        this.messageResourceId = messageResourceId;
    }

    public ErrorPayload(Throwable throwable, int messageResourceId) {
        super(throwable);
        this.messageResourceId = messageResourceId;
    }

    public int getMessageResourceId() {
        return messageResourceId;
    }

    @Override
    @Deprecated
    public String getLocalizedMessage() {
        return super.getLocalizedMessage();
    }

    public String getLocalizedMessage(Context context) {
        return context.getResources().getString(getMessageResourceId());
    }
}
