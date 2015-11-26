package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import de.unipassau.isl.evs.ssh.app.AppModuleHandler;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * This fragment allows to display information contained in light messages which are received from the IncomingDispatcher.
 * Furthermore it generates a light messages as instructed by the UI and passes it to the OutgoingRouter.
 */
public class LightFragment extends Fragment {

    private static final int BUTTON_HEIGHT = 50;
    private final Context context = getActivity();


    public LightFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FrameLayout mLinearLayout = (FrameLayout) inflater.inflate(R.layout.fragment_light,
                container, false);
        LinearLayout lightButtonLayout = (LinearLayout) mLinearLayout.findViewById(R.id.lightButtonLayout);

        List<Module> lightModules = getLightModules();
        for (Module m : lightModules) {
            LinearLayout lightButton = (LinearLayout) inflater.inflate(R.layout.lightbutton, container, false);

            TextView textView = (TextView) lightButton.findViewById(R.id.lightButtonTextView);
            Button button = (Button) lightButton.findViewById(R.id.lightButtonButton);
            ImageView imageViewOn = (ImageView) lightButton.findViewById(R.id.lightButtonImageViewOn);
            ImageView imageViewOff = (ImageView) lightButton.findViewById(R.id.lightButtonImageViewOff);
            if (lightTurnedOn(m)) {
                imageViewOff.setVisibility(View.GONE);
                imageViewOn.setVisibility(View.VISIBLE);
                button.setText();
            } else {
                imageViewOn.setVisibility(View.GONE);
                imageViewOff.setVisibility(View.VISIBLE);
            }
            char[] name = getName(m);
            textView.setText(name, 0, name.length);

            lightButtonLayout.addView(lightButton);
        }

        /*
//        FIXME out comment and fix
        List lightModules = getLightModules();
        for (Module m : lightModules) {
            Button light_button = new Button(getActivity());
            light_button.setHeight(BUTTON_HEIGHT);
            light_button.setId("@+id/" + m.getID());
            light_button.setText(m.getName());
            light_button.setTextColor();
            if (m.getStatus) {
                light_button.setBackground(Drawable.createFromPath("/drawable-hdpi/ic_light_on.png"));
            } else {
                light_button.setBackground(Drawable.createFromPath("/drawable-hdpi/light_off.png"));
            }
            lightButtonLayout.addView(light_button);
        }*/
        return inflater.inflate(R.layout.fragment_light, container, false);

    }

    private char[] getName(Module m) {

        return "tor".toCharArray();
    }

    private boolean lightTurnedOn(Module m) {
        requestLightStatus(m);


        return false;
    }



    private List<Module> getLightModules() {
        return AppModuleHandler.getLights();
    }

}