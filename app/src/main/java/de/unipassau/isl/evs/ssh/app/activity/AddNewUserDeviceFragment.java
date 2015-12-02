package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.AppModuleHandler;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows to enter information describing new user devices and provide a QR-Code
 * which a given user device has to scan. If this functionality is used a message,
 * containing all needed information, is generated and passed to the OutgoingRouter.
 */
public class AddNewUserDeviceFragment extends Fragment implements MessageHandler {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        // TODO get groups
        List<String> groups = new LinkedList<>();
        groups.add("Eltern");
        groups.add("Kinder");
        groups.add("Gäste");
        View view = inflater.inflate(R.layout.fragment_addnewuserdevice, container, false);
        Spinner spinner = (Spinner) view.findViewById(R.id.groupSpinner);
        SpinnerAdapter adapter = new ArrayAdapter<>(getActivity().getApplicationContext(),
                android.R.layout.simple_list_item_1, groups);
        spinner.setAdapter(adapter);
        return view;
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