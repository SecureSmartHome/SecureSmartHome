package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

public class MainFragment extends Fragment implements MessageHandler {

    public MainFragment() {
        // Required empty public constructor
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout mLinearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_main,
                container, false);

        final MainActivity parent = (MainActivity) getActivity();

        Button doorButton = (Button) mLinearLayout.findViewById(R.id.doorButton);
        doorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByID(R.id.nav_door);
            }
        });

        Button lightButton = (Button) mLinearLayout.findViewById(R.id.lightButton);
        lightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByID(R.id.nav_light);
            }
        });

        Button climateButton = (Button) mLinearLayout.findViewById(R.id.climateButton);
        climateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByID(R.id.nav_climate);
            }
        });

        Button holidayButton = (Button) mLinearLayout.findViewById(R.id.holidayButton);
        holidayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByID(R.id.nav_holiday);
            }
        });

        Button statusButton = (Button) mLinearLayout.findViewById(R.id.statusButton);
        statusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByID(R.id.nav_status);
            }
        });

        Button usersButton = (Button) mLinearLayout.findViewById(R.id.usersButton);
        usersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByID(R.id.nav_addNewUserDevice);
            }
        });
        return mLinearLayout;
    }

    @Override
    public void handle(Message.AddressedMessage message) {

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }
}
