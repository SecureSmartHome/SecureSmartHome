/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.core.schedule;

import android.content.Intent;
import android.os.Bundle;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;

/**
 * A {@link Component} that can receive alarms scheduled by the {@link Scheduler}.
 *
 * @author Christoph Fraedrich
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
