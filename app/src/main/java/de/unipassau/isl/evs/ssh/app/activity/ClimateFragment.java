package de.unipassau.isl.evs.ssh.app.activity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;

/**
 * This activity allows to display information contained in climate messages which are received from
 * the IncomingDispatcher.
 * Furthermore it generates a climate messages as instructed by the UI and passes it to the OutgoingRouter.
 */
public class ClimateFragment extends Fragment implements MessageHandler {
    NotificationCompat.Builder climateWarning;
    private static final int uniqueID = 037735;

    public ClimateFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //climateWarning = new NotificationCompat.Builder(this);
        climateWarning.setAutoCancel(true);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_climate, container, false);
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

    public void notificationButtonClicked(View view){
        //Build notification
        climateWarning.setSmallIcon(R.drawable.icon);
        climateWarning.setTicker("Climate Warning!");
        climateWarning.setWhen(System.currentTimeMillis());
        climateWarning.setContentText("Humidity in Room is to high! Please open Window.");

//        //If Notification is clicked send to this Page
//        Intent intent = new Intent(this, ClimateFragment.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        climateWarning.setContentIntent(pendingIntent);
//
//        //Send notification out to Device
//        NotificationManager nm = {NotificationManager} getSystemService{NOTIFICATION_SERVICE};
//        nm.notify(uniqueID, climateWarning.build());
    }
}