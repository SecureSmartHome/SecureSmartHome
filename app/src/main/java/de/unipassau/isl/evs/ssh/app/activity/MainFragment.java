package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import de.unipassau.isl.evs.ssh.app.R;

/**
 * MainFragment gives the user an overview over the most important functions.
 * Other functions can be accessed through the Navigation Drawer.
 *
 * @author Andreas Bucher
 */
public class MainFragment extends BoundFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout mLinearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_main,
                container, false);
        final MainActivity parent = (MainActivity) getActivity();

        ImageButton doorButton = (ImageButton) mLinearLayout.findViewById(R.id.doorButtonOpen);
        doorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.showFragmentByClass(DoorFragment.class);
            }
        });
        ImageButton lightButton = (ImageButton) mLinearLayout.findViewById(R.id.lightButtonOn);
        lightButton.setOnClickListener(new View.OnClickListener() {
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
}
