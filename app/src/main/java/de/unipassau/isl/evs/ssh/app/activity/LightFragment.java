package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.handler.Handler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows to display information contained in light messages which are received from the IncomingDispatcher.
 * Furthermore it generates a light messages as instructed by the UI and passes it to the OutgoingRouter.
 */
public class LightFragment extends Fragment implements Handler{
    @Override
    public void handle(Message message) {
        //TODO implement
        throw new UnsupportedOperationException();
    }

    public LightFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_light, container, false);
    }

}