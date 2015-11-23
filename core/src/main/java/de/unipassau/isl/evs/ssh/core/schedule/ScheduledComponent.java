package de.unipassau.isl.evs.ssh.core.schedule;

import android.content.Intent;
import android.os.Bundle;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;

/**
 * A {@link Component} that can receive alarms scheduled by the {@link Scheduler}.
 *
 * @see Scheduler
 */
public interface ScheduledComponent extends Component {
    /**
     * Called when the {@link de.unipassau.isl.evs.ssh.core.container.ContainerService ContainerService}
     * receives an {@link Intent} from the {@link android.app.AlarmManager AlarmManager}.
     *
     * @param intent the Intent that was scheduled using the {@link android.app.AlarmManager AlarmManager}
     * @see Scheduler#getPendingScheduleIntent(Key, Bundle, int) Scheduler on how to generate and schedule Intents
     */
    void onReceive(Intent intent);
}
