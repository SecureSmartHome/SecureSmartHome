package de.unipassau.isl.evs.ssh.core.schedule;

import android.content.Intent;

import de.unipassau.isl.evs.ssh.core.container.Component;

public interface ScheduledComponent extends Component {
    void onReceive(Intent intent);
}
