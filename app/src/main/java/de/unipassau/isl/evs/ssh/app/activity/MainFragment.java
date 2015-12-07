package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * Main page if you start App
 * @author bucher
 */
public class MainFragment extends BoundFragment implements MessageHandler {

    public MainFragment() {
        // Required empty public constructor
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout mLinearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_main,
                container, false);
        final MainActivity parent = (MainActivity) getActivity();

        ImageButton doorButtonOpen = (ImageButton) mLinearLayout.findViewById(R.id.doorButtonOpen);
        ImageButton doorButtonClosed = (ImageButton) mLinearLayout.findViewById(R.id.doorButtonClosed);
//fixme        if (doorIsOpen()) {
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
                parent.showFragmentByClass(DoorFragment.class);
            }
        });
        doorButtonClosed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(DoorFragment.class);
            }
        });

        ImageButton lightButtonOn = (ImageButton) mLinearLayout.findViewById(R.id.lightButtonOn);
        ImageButton lightButtonOff = (ImageButton) mLinearLayout.findViewById(R.id.lightButtonOff);
        //The status of the light imageButton depends in the status of the lights.
//fixme        if (lightTurnedOn()) {
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
                parent.showFragmentByClass(LightFragment.class);
            }
        });
        lightButtonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(LightFragment.class);
            }
        });

        ImageButton climateButton = (ImageButton) mLinearLayout.findViewById(R.id.climateButton);
        climateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(ClimateFragment.class);
            }
        });

        ImageButton holidayButton = (ImageButton) mLinearLayout.findViewById(R.id.holidayButton);
        holidayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(HolidayFragment.class);
            }
        });

        ImageButton statusButton = (ImageButton) mLinearLayout.findViewById(R.id.statusButton);
        statusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(StatusFragment.class);
            }
        });

        ImageButton usersButton = (ImageButton) mLinearLayout.findViewById(R.id.usersButton);
        usersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(ListGroupFragment.class);
            }
        });
        return mLinearLayout;
    }

    /**
     * Checks if one of the registered doors is open.
     *
     * @return If one of the registered doors is open.
     */
/*
    private boolean doorIsOpen() {
        List<Module> list = getDoorModules();
        for (Module m : list) {
            if (m.getStatus()) {
                return true;
            }
        }
        return false;
    }

    */
/**
     * todo
     *
     * @return All modules from the type door.
 *//*

    private List<Module> getDoorModules() {
        // get list
        // filter door modules
        return null;
    }

    */
/**
     * Checks if one of the registered lights is turned on.
     *
     * @return If one of all registered lights is turned on.
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

    /**
     * todo
     *
     * @return All modules from the type light.
     *//*

    private List<Module> getLightModules() {
        // get list
        // filter light modules
        return null;
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
