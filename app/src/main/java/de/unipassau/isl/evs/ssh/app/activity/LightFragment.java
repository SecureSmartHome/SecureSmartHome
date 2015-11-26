package de.unipassau.isl.evs.ssh.app.activity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This fragment allows to display information contained in light messages which are received from the IncomingDispatcher.
 * Furthermore it generates a light messages as instructed by the UI and passes it to the OutgoingRouter.
 */
public class LightFragment extends Fragment implements MessageHandler {

    private static final int BUTTON_HEIGHT = 50;

    public LightFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        LinearLayout mLinearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_light,
                container, false);
        LinearLayout lightButtonLayout = (LinearLayout) mLinearLayout.findViewById(R.id.lightButtonLayout);
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
        }
        return inflater.inflate(R.layout.fragment_light, container, false);
    }
    private List<Module> getLightModules() {
        List<Module> list = new LinkedList<>();

        //ask database which modules are light-modules. maybe enum with module types
//        get them
//        add them to the list

        return list;
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