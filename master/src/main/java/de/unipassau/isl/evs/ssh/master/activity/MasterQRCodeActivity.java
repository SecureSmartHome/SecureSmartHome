package de.unipassau.isl.evs.ssh.master.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.DeviceConnectedPayload;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.master.R;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_MESSAGE;
import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.MASTER_DEVICE_CONNECTED;

/**
 * MasterQRCodeActivity to display a QR-Code in the masters UI. This is used to register the first
 * user-device safely as an admin device.
 *
 * @author Phil Werli
 */
public class MasterQRCodeActivity extends MasterStartUpActivity implements MessageHandler {
    public static final String EXTRA_REGISTERED_DEVICE = "EXTRA_REGISTERED_DEVICE";

    /**
     * The QR-Code which will be displayed.
     */
    private Bitmap bitmap;

    /**
     * Generates a QR-Code from the sent data.
     *
     * @return the created QR-Code
     */
    private Bitmap createQRCodeBitmap() {
        Serializable extra = getIntent().getExtras().getSerializable(EXTRA_QR_DEVICE_INFORMATION);
        if (extra instanceof DeviceConnectInformation) {
            try {
                return ((DeviceConnectInformation) extra).toQRBitmap(Bitmap.Config.ARGB_8888, Color.BLACK, Color.WHITE);
            } catch (WriterException e) {
                throw new IllegalArgumentException("illegal QRCode data", e);
            }
        } else {
            throw new IllegalArgumentException("missing EXTRA_QR_DEVICE_INFORMATION as extra " + EXTRA_QR_DEVICE_INFORMATION);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isSwitching()) {
            setContentView(R.layout.activity_qrcode);
        }
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        if (isSwitching()) {
            return;
        }
        buildView();
        container.require(IncomingDispatcher.KEY).registerHandler(this, MASTER_DEVICE_CONNECTED);
    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, RoutingKey routingKey) {
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (MASTER_DEVICE_CONNECTED.matches(message)) {
            final DeviceConnectedPayload connectedDevice = MASTER_DEVICE_CONNECTED.getPayload(message);
            final UserDevice registeredDevice = (UserDevice) getIntent().getSerializableExtra(EXTRA_REGISTERED_DEVICE);
            if (registeredDevice == null || connectedDevice.deviceID.equals(registeredDevice.getUserDeviceID())) {
                checkSwitchActivity();
            }
        }
    }

    @Override
    public void handlerRemoved(RoutingKey routingKey) {
    }

    @Override
    public void onContainerDisconnected() {
        final IncomingDispatcher dispatcher = getComponent(IncomingDispatcher.KEY);
        if (dispatcher != null) {
            dispatcher.unregisterHandler(this);
        }
        super.onContainerDisconnected();
    }

    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        ImageView imageview = ((ImageView) findViewById(R.id.qrcode_activity_qr_code));
        bitmap = createQRCodeBitmap();

        //Workaround to scale QR-Code
        //Makes bitmap bigger than the screen. The ImageView adjusts the size itself.
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * CoreConstants.QRCodeInformation.QR_CODE_IMAGE_SCALE, bitmap.getHeight() * CoreConstants.QRCodeInformation.QR_CODE_IMAGE_SCALE, false);

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }
        TextView textview = ((TextView) findViewById(R.id.qrcode_activity_text));
        String text = getIntent().getExtras().getString(EXTRA_QR_MESSAGE);
        if (text != null) {
            textview.setText(text);
        }
    }
}
