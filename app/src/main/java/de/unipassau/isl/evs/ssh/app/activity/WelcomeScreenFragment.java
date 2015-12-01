package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.R;

/**
 * WelcomeScreenFragment to display a welcome message to every user when he initially starts the app.
 * As the device isn't registered yet, it asks the user to scan a QR-Code by calling {@link ScanQRCodeFragment}.
 *
 * @author Phil Werli
 */
public class WelcomeScreenFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout root = (LinearLayout) inflater.inflate(R.layout.fragment_welcomescreen, container, false);

        Button button = (Button) root.findViewById(R.id.welcomescreen_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).showFragmentByClass(ScanQRCodeFragment.class);
            }
        });
        return root;
    }
}
