package de.unipassau.isl.evs.ssh.master;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Start Service if Master has finished boot up
 *
 * @author Andreas Bucher
 */
public class OnBootReceiver extends BroadcastReceiver {
    public OnBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, MasterContainer.class));
    }
}
