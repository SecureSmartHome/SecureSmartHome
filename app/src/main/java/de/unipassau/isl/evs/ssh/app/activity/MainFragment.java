package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

        ImageButton doorButtonOpen = (ImageButton) mLinearLayout.findViewById(R.id.doorButtonOpen);
        ImageButton doorButtonClosed = (ImageButton) mLinearLayout.findViewById(R.id.doorButtonClosed);
//        if (doorIsOpen()) {
        if (true) {
            doorButtonOpen.setVisibility(View.VISIBLE);
            doorButtonClosed.setVisibility(View.GONE);
        } else {
            doorButtonClosed.setVisibility(View.VISIBLE);
            doorButtonOpen.setVisibility(View.GONE);

        }
        doorButtonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DoorFragment fragment = new DoorFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });
        doorButtonClosed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DoorFragment fragment = new DoorFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });

        ImageButton lightButtonOn = (ImageButton) mLinearLayout.findViewById(R.id.lightButtonOn);
        ImageButton lightButtonOff = (ImageButton) mLinearLayout.findViewById(R.id.lightButtonOff);
        //The status of the light imageButton depends in the status of the lights.
//        if (lightTurnedOn()) {
        if (true) {
            lightButtonOff.setVisibility(View.GONE);
            lightButtonOn.setVisibility(View.VISIBLE);
        } else {
            lightButtonOn.setVisibility(View.GONE);
            lightButtonOff.setVisibility(View.VISIBLE);
        }
        lightButtonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LightFragment fragment = new LightFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });
        lightButtonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LightFragment fragment = new LightFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction =
                        getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
            }
        });

        ImageButton climateButton = (ImageButton) mLinearLayout.findViewById(R.id.climateButton);
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

        ImageButton holidayButton = (ImageButton) mLinearLayout.findViewById(R.id.holidayButton);
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

        ImageButton statusButton = (ImageButton) mLinearLayout.findViewById(R.id.statusButton);
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

        ImageButton usersButton = (ImageButton) mLinearLayout.findViewById(R.id.usersButton);
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
/*

    */
/**
 * Checks if one of the registered doors is open.
 *
 * @return If one of the registered doors is open.
 *//*

    private boolean doorIsOpen() {
        List<Module> list = getDoorModules();
        for (Module m : list){
            if (m.getStatus()) {
                return true;
            }
        }
        return false;
    }

    */

    /**
     * Checks if one of the registered lights is turned on.
     *
     * @return If one of all registered lights is turned on.
     * @see LightFragment#//getLightModules()
     *//*

    private boolean lightTurnedOn() {
        List<Module> list = getLightModules(); //TODO get light modules from database
        for (Module m : list) {
            if (m.getStatus()) {
                return true;
            }
        }
        return false;
    }
*/

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
