package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppHolidaySimulationHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
 * This activity allows to start and stop the holiday simulation. If this functionality is used a message,
 * containing all needed information, is generated and passed to the OutgoingRouter.
 *
 * @author Christoph Fraedrich
 */
public class HolidayFragment extends BoundFragment {

    private static final String TAG = HolidayFragment.class.getSimpleName();
    private Button switchButton;
    private TextView statusView;

    private final AppHolidaySimulationHandler.HolidaySimulationListener holidaySimulationListener
            = new AppHolidaySimulationHandler.HolidaySimulationListener() {
        @Override
        public void onHolidaySetReply(final boolean wasSuccessful) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (wasSuccessful) {
                        updateView();
                    } else {
                        showToast(R.string.could_not_switch_holiday);
                    }
                }
            });
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_holiday, container, false);
        switchButton = (Button) view.findViewById(R.id.holidayFragmentButton);
        statusView = (TextView) view.findViewById(R.id.holidayFragmentStatusField);

        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AppMainActivity activity = (AppMainActivity) getActivity();
                if (activity != null && activity.hasPermission(Permission.HOLIDAY_MODE_SWITCHED_ON)
                        && activity.hasPermission(Permission.HOLIDAY_MODE_SWITCHED_OFF)) {
                    switchButtonAction();
                } else {
                    showToast(R.string.you_can_not_start_or_stop_the_holiday_simulation);
                }
            }
        });

        return view;
    }

    @Override
    public void onContainerConnected(Container container) {
        container.require(AppHolidaySimulationHandler.KEY).addListener(holidaySimulationListener);
        updateView();
    }

    @Override
    public void onContainerDisconnected() {
        final AppHolidaySimulationHandler handler = getHolidaySimulationHandler();
        if (handler != null) {
            handler.removeListener(holidaySimulationListener);
        }

        super.onContainerDisconnected();
    }

    @Nullable
    private AppHolidaySimulationHandler getHolidaySimulationHandler() {
        return getComponent(AppHolidaySimulationHandler.KEY);
    }

    /**
     * Updates the buttons in this fragment's to represent the current holiday status.
     */
    private void updateView() {
        final AppHolidaySimulationHandler handler = getHolidaySimulationHandler();
        if (handler == null) {
            Log.i(TAG, "Container not bound.");
            return;
        }

        if (handler.isOn()) {
            switchButton.setText(R.string.switchSimulationOff);
            statusView.setText(R.string.holidaySimulationOn);
        } else {
            switchButton.setText(R.string.switchSimulationOn);
            statusView.setText(R.string.holidaySimulationOff);
        }
    }

    /**
     * executed, when the "Switch" button was pressed.
     */
    private void switchButtonAction() {
        final AppHolidaySimulationHandler handler = getHolidaySimulationHandler();

        if (handler == null) {
            Log.i(TAG, "Container not bound.");
            return;
        }

        if (handler.isOn()) {
            handler.switchHolidaySimulation(false);
        } else {
            handler.switchHolidaySimulation(true);
        }
        updateView();
    }
}