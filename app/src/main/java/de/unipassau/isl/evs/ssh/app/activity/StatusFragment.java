package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows to visualize status information of the system. If this functionality is used a message,
 * requesting all needed information, is generated and passed to the OutgoingRouter.
 */
public class StatusFragment extends Fragment implements MessageHandler {

    public StatusFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_status, container, false);
        ListView listView = (ListView) view.findViewById(R.id.deviceStatusListView);

        List<Slave> slaves = new LinkedList<>();
        slaves.add(new Slave("sl1", true, "1.1.1.1"));
        slaves.add(new Slave("sl2", true, "1.1.1.2"));

        ArrayAdapter<Slave> adapter = new ArrayAdapter<Slave>(getActivity().getApplicationContext(), R.layout.device_status_list_item, slaves.toArray(new Slave[2])) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TableLayout layout;
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    layout = (TableLayout) inflater.inflate(R.layout.device_status_list_item, parent, false);
                } else {
                    layout = (TableLayout) convertView;
                }

                TextView slaveName = (TextView) layout.findViewById(R.id.deviceStatusSlaveName);
                TextView slaveStatus = (TextView) layout.findViewById(R.id.deviceStatusSlaveStatus);
                TextView slaveIP = (TextView) layout.findViewById(R.id.deviceStatusSlaveIP);

                Slave slave = getItem(position);
                slaveName.setText(slave.getName());
                slaveIP.setText(slave.getIpAdress());
                slaveStatus.setText("online");
                return layout;
            }
        };


        listView.setAdapter(adapter);
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

    public class Slave{
        private String name;
        private boolean isOnline;
        private String ipAdress;

        public Slave(String name, boolean isOnline, String ipAdress) {
            this.name = name;
            this.isOnline = isOnline;
            this.ipAdress = ipAdress;
        }

        public String getName() {
            return name;
        }

        public boolean isOnline() {
            return isOnline;
        }

        public String getIpAdress() {
            return ipAdress;
        }
    }

}