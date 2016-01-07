package de.unipassau.isl.evs.ssh.slave.handler;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.unipassau.isl.evs.ssh.core.container.ContainerService;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.CameraPayload;
import de.unipassau.isl.evs.ssh.slave.activity.OdroidCamera;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.SLAVE_CAMERA_GET;

/**
 * Handles messages requesting pictures from the camera (via API calls) and generates messages,
 * containing the pictures, and sends these to the master.
 *
 * @author Niko Fink
 */
public class SlaveCameraHandler extends AbstractMessageHandler {
    /**
     * Will perform actions based on the message given, e.g. permission/sanity checks.
     *
     * @param message Message to handle.
     */
    @Override
    public void handle(final Message.AddressedMessage message) {
        if (SLAVE_CAMERA_GET.matches(message)) {
            final CameraPayload payload = SLAVE_CAMERA_GET.getPayload(message);
            final Context context = requireComponent(ContainerService.KEY_CONTEXT);
            final Intent intent = OdroidCamera.getIntent(
                    context,
                    payload.getCameraID(),
                    payload.getModuleName(),
                    message
            );
            Log.d(getClass().getSimpleName(), "Starting Camera Activity: " + intent);
            context.startActivity(intent);
        } else {
            invalidMessage(message);
        }
    }

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{SLAVE_CAMERA_GET};
    }
}