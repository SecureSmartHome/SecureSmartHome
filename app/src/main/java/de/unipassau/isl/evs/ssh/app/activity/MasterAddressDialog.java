package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.InetSocketAddress;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.activity.BoundActivity;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.network.Client;

/**
 * @author Niko Fink
 */
public class MasterAddressDialog extends DialogFragment {
    private static final String TAG = MasterAddressDialog.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_master_address, null);
        updateView(view);
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);
        return builder.create();
    }


    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    save();
                }
            });
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        updateView(getView());
    }

    private void updateView(@Nullable View view) {
        final Client client = getComponent(Client.KEY);
        if (client != null && view != null) {
            final InetSocketAddress address = client.getConfiguredAddress();
            if (address == null) {
                return;
            }
            ((EditText) view.findViewById(R.id.editTextPort)).setText(String.valueOf(address.getPort()));
            ((EditText) view.findViewById(R.id.editTextHost)).setText(address.getHostString());
        }
    }

    private void save() {
        final Client client = getComponent(Client.KEY);
        if (client == null) {
            Log.i(TAG, "Container not connected");
            return;
        }
        final EditText editTextHost = (EditText) getDialog().findViewById(R.id.editTextHost);
        final EditText editTextPort = (EditText) getDialog().findViewById(R.id.editTextPort);
        try {
            client.onMasterConfigured(
                    InetSocketAddress.createUnresolved(
                            editTextHost.getText().toString(),
                            Integer.parseInt(editTextPort.getText().toString())
                    )
            );
            dismiss();
        } catch (IllegalArgumentException e) {
            Toast.makeText(getActivity(), "Illegal value", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Fetch the Component from the Container or return {@code null} if the Component or the Container itself are not available.
     *
     * @see BoundActivity#getComponent(Key)
     */
    @Nullable
    protected <T extends Component> T getComponent(Key<T> key) {
        if (!(getActivity() instanceof BoundActivity)) {
            return null;
        }
        final Container container = ((BoundActivity) getActivity()).getContainer();
        if (container == null) {
            return null;
        } else {
            return container.get(key);
        }
    }
}
