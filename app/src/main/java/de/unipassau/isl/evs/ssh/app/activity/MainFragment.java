package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Created by bucher on 23.11.15.
 */
public class MainFragment extends Fragment implements Handler{

    @Override
    public void handle(Message message) {

    }

    public MainFragment() {
        // Required empty public constructor
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout mLinearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_main,
                container, false);

        Button doorButton = (Button) mLinearLayout.findViewById(R.id.doorButton);
        doorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DoorFragment fragment = new DoorFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });

        Button lightButton = (Button) mLinearLayout.findViewById(R.id.lightButton);
        lightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LightFragment fragment = new LightFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });

        Button climateButton = (Button) mLinearLayout.findViewById(R.id.climateButton);
        climateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClimateFragment fragment = new ClimateFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });

        Button holidayButton = (Button) mLinearLayout.findViewById(R.id.holidayButton);
        holidayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HolidayFragment fragment = new HolidayFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });

        Button statusButton = (Button) mLinearLayout.findViewById(R.id.statusButton);
        statusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StatusFragment fragment = new StatusFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });

        Button usersButton = (Button) mLinearLayout.findViewById(R.id.usersButton);
        usersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ModifyPermissionFragment fragment = new ModifyPermissionFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });
        return mLinearLayout;
    }
}
