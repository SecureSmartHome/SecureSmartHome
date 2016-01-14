package de.unipassau.isl.evs.ssh.slave.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.WriterException;

import java.io.Serializable;

import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.network.Client;
import de.unipassau.isl.evs.ssh.core.network.ClientConnectionListener;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;
import de.unipassau.isl.evs.ssh.slave.R;

import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION;
import static de.unipassau.isl.evs.ssh.core.CoreConstants.QRCodeInformation.QR_CODE_IMAGE_SCALE;

/**
 * SlaveQRCodeActivity to display a QR-Code in the slaves UI. This is used to register new slaves to the system.
 *
 * @author Wolfgang Popp.
 */
public class SlaveQRCodeActivity extends SlaveStartUpActivity implements ClientConnectionListener {
    private static final int LOCAL_MASTER_REQUEST_CODE = 2;
    private static final String LOCAL_MASTER_PACKAGE = "de.unipassau.isl.evs.ssh.master";
    private static final String LOCAL_MASTER_ACTIVITY = LOCAL_MASTER_PACKAGE + ".activity.RegisterLocalSlaveActivity";
    private static final String EXTRA_TRIED_LOCAL_CONNECTION = "triedLocalConnection";

    private ProgressDialog dialog;
    private boolean triedLocalConnection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isSwitching()) {
            return;
        }
        setContentView(R.layout.activity_qrcode);

        ImageView imageview = ((ImageView) findViewById(R.id.qrcode_activity_qr_code));
        Bitmap bitmap;
        try {
            bitmap = getDeviceInformation().toQRBitmap(Bitmap.Config.ARGB_8888, Color.BLACK, Color.WHITE);
        } catch (WriterException e) {
            throw new IllegalArgumentException("illegal QRCode data", e);
        }

        //Workaround to scale QR-Code
        //Makes bitmap bigger than the screen. The ImageView adjusts the size itself.
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * QR_CODE_IMAGE_SCALE,
                bitmap.getHeight() * QR_CODE_IMAGE_SCALE, false);

        if (bitmap != null) {
            imageview.setImageBitmap(bitmap);
            imageview.setVisibility(View.VISIBLE);
        }

        if (savedInstanceState != null) {
            triedLocalConnection = savedInstanceState.getBoolean(EXTRA_TRIED_LOCAL_CONNECTION, triedLocalConnection);
        }

        if (!triedLocalConnection) {
            tryLocalConnection();
        }
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        if (isSwitching()) {
            return;
        }
        container.require(Client.KEY).addListener(this);
    }

    @Override
    public void onContainerDisconnected() {
        final Client client = getComponent(Client.KEY);
        if (client != null) {
            client.removeListener(this);
        }
        super.onContainerDisconnected();
    }

    @Override
    protected void onStop() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCAL_MASTER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            showConnectingDialog();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_TRIED_LOCAL_CONNECTION, triedLocalConnection);
    }

    /**
     * Try to open the Master Activity for adding a local slave
     */
    protected boolean tryLocalConnection() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(LOCAL_MASTER_PACKAGE, LOCAL_MASTER_ACTIVITY));
            intent.putExtra(EXTRA_QR_DEVICE_INFORMATION, getDeviceInformation());
            startActivityForResult(intent, LOCAL_MASTER_REQUEST_CODE);
            triedLocalConnection = true;
            return true;
        } catch (ActivityNotFoundException ignore) {
            return false;
        }
    }

    @NonNull
    private DeviceConnectInformation getDeviceInformation() {
        Serializable extra = getIntent().getExtras().getSerializable(EXTRA_QR_DEVICE_INFORMATION);
        if (extra instanceof DeviceConnectInformation) {
            return (DeviceConnectInformation) extra;
        } else {
            throw new IllegalArgumentException("missing DeviceConnectInformation as extra " + EXTRA_QR_DEVICE_INFORMATION);
        }
    }

    private void showConnectingDialog() {
        if (dialog == null || !dialog.isShowing()) {
            dialog = ProgressDialog.show(
                    this,
                    "Connecting...",
                    "Trying to find address of Master and connect to it.",
                    true, //indeterminate
                    true  //cancellable
            );
        }
    }

    @Override
    public void onMasterFound() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isSwitching()) return;
                showConnectingDialog();
                dialog.setMessage("Address of Master found, connecting");
            }
        });
    }

    @Override
    public void onClientConnecting(final String host, final int port) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isSwitching()) return;
                showConnectingDialog();
                dialog.setMessage("Address of Master found, connecting to " + host + ":" + port);
            }
        });
    }

    @Override
    public void onClientConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isSwitching()) return;
                if (dialog != null && dialog.isShowing()) {
                    dialog.setMessage("Successfully connected to Master");
                }
                checkSwitchActivity();
            }
        });
    }

    @Override
    public void onClientDisconnected() {
    }

    @Override
    public void onClientRejected(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isSwitching()) return;
                showConnectingDialog();
                dialog.setMessage("Master rejected connection with message: " + message + "\nPlease rescan the QR-Code.");
                dialog.setIndeterminate(false);
                dialog.setCancelable(true);
            }
        });
    }
}
