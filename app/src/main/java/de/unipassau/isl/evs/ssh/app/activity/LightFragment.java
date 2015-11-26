package de.unipassau.isl.evs.ssh.app.activity;

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

import java.util.Map;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppLightHandler;
import de.unipassau.isl.evs.ssh.app.handler.HandlerUpdateListener;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * This fragment allows to display information contained in light messages which are received from the IncomingDispatcher.
 * Furthermore it generates a light messages as instructed by the UI and passes it to the OutgoingRouter.
 *
 * @author Phil
 */
public class LightFragment extends Fragment {

    private HandlerUpdateListener handlerUpdateListener;


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

        Map<Module, Boolean> lightModules = getLightModules();
        for (final Module m : lightModules.keySet()) {
            //create linearlayout as defined in lightbutton.xml file
            LinearLayout lightButtonContainer = (LinearLayout) inflater.inflate(R.layout.lightbutton, container, false);

            TextView textView = (TextView) lightButtonContainer.findViewById(R.id.lightButtonTextView);
            Button button = (Button) lightButtonContainer.findViewById(R.id.lightButtonButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) getActivity()).getContainer().require(AppLightHandler.KEY).toggleLight(m);
                }
            });
            // set up TextView
            char[] name = getName(m);
            textView.setText(name, 0, name.length);

            // set up ImageView and button
            ImageView imageViewOn = (ImageView) lightButtonContainer.findViewById(R.id.lightButtonImageViewOn);
            ImageView imageViewOff = (ImageView) lightButtonContainer.findViewById(R.id.lightButtonImageViewOff);
            if (lightModules.get(m)) {
                imageViewOff.setVisibility(View.GONE);
                imageViewOn.setVisibility(View.VISIBLE);
                button.setText(getResources().getString(R.string.string_turn_off));
                button.setBackgroundColor(getResources().getColor(R.color.button_red));
            } else {
                imageViewOn.setVisibility(View.GONE);
                imageViewOff.setVisibility(View.VISIBLE);
                button.setText(getResources().getString(R.string.string_turn_on));
                button.setBackgroundColor(getResources().getColor(R.color.button_green));
            }
            lightButtonLayout.addView(lightButtonContainer);
        }

        handlerUpdateListener = new LightHandlerUpdateListener();
        ((MainActivity) getActivity()).getContainer().require(AppLightHandler.KEY).addHandlerUpdateListener(
                handlerUpdateListener);

        return inflater.inflate(R.layout.fragment_light, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handlerUpdateListener = new LightHandlerUpdateListener();
        ((MainActivity) getActivity()).getContainer().require(AppLightHandler.KEY).removeHandlerUpdateListener(
                handlerUpdateListener);
    }

    private char[] getName(Module m) {
        return m.getName().toCharArray();
    }

    private Map<Module, Boolean> getLightModules() {
        return ((MainActivity) getActivity()).getContainer().require(AppLightHandler.KEY).getAllLightModuleStates();
    }

    private class LightHandlerUpdateListener implements HandlerUpdateListener {

        @Override
        public void updatePerformed() {
            AppLightHandler lightHandler = ((MainActivity) getActivity()).getContainer().require(AppLightHandler.KEY);
            for (Module m : getLightModules().keySet()) {
                lightHandler.isModuleOn(m);
            }
        }
    }
}