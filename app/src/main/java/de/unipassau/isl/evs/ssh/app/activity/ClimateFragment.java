package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppClimateHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * This activity allows to display information contained in climate messages which are received from
 * the IncomingDispatcher.
 * Furthermore it generates a climate messages as instructed by the UI and passes it to the OutgoingRouter.
 *
 * @author bucher
 */
public class ClimateFragment extends BoundFragment {
    private static final String TAG = ClimateFragment.class.getSimpleName();
    private ClimateListAdapter adapter;
    private ListView listView;
    private final AppClimateHandler.ClimateHandlerListener listener = new AppClimateHandler.ClimateHandlerListener() {
        @Override
        public void statusChanged(Module module) {
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        container.require(AppClimateHandler.KEY).addListener(listener);
        adapter = new ClimateListAdapter();
        listView.setAdapter(adapter);
    }

    @Override
    public void onContainerDisconnected() {
        getComponent(AppClimateHandler.KEY).removeListener(listener);
        super.onContainerDisconnected();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_light, container, false);
        listView = (ListView) root.findViewById(R.id.climateSensorContainer);
        return root;
    }

    private class ClimateListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private List<Module> climateSensorModules;

        public ClimateListAdapter() {
            this.inflater = (LayoutInflater) getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            updateModuleList();
        }

        @Override
        public void notifyDataSetChanged() {
            updateModuleList();
            super.notifyDataSetChanged();
        }

        private void updateModuleList() {

            AppClimateHandler handler = getComponent(AppClimateHandler.KEY);
            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }

            final Map<Module, AppClimateHandler.ClimateStatus> climateStatus = handler.getAllClimateModuleStates();
            climateSensorModules = Lists.newArrayList(climateStatus.keySet());
            Collections.sort(climateSensorModules, new Comparator<Module>() {
                @Override
                public int compare(Module lhs, Module rhs) {
                    if (lhs.getName() == null) {
                        return rhs.getName() == null ? 0 : 1;
                    }
                    if (rhs.getName() == null) {
                        return -1;
                    }
                    return lhs.getName().compareTo(rhs.getName());
                }
            });
        }

        @Override
        public int getCount() {
            return climateSensorModules.size();
        }

        @Override
        public Module getItem(int position) {
            return climateSensorModules.get(position);
        }

        @Override
        public long getItemId(int position) {
            final Module item = getItem(position);
            if (item != null && item.getName() != null) {
                return item.getName().hashCode();
            } else {
                return 0;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout climateSensorLayout;
            if (convertView == null) {
                climateSensorLayout = (LinearLayout) inflater.inflate(R.layout.climatesensor, parent, false);
            } else {
                climateSensorLayout = (LinearLayout) convertView;
            }

            final Module m = getItem(position);

            TextView climateSensorView = (TextView) climateSensorLayout.findViewById(R.id.climateSensor);
            TextView temp1View = (TextView) climateSensorLayout.findViewById(R.id.temp1);
            TextView temp2View = (TextView) climateSensorLayout.findViewById(R.id.temp2);
            TextView pressureView = (TextView) climateSensorLayout.findViewById(R.id.pressure);
            TextView altitudeView = (TextView) climateSensorLayout.findViewById(R.id.altitude);
            TextView humidityView = (TextView) climateSensorLayout.findViewById(R.id.humidity);
            TextView uvView = (TextView) climateSensorLayout.findViewById(R.id.uv);
            TextView visibleView = (TextView) climateSensorLayout.findViewById(R.id.visible);
            TextView irView = (TextView) climateSensorLayout.findViewById(R.id.ir);


            //TODO climateSensorView.setText(climateSensorView.getText() + DataOfSensor) ;
            climateSensorView.getText();
            temp1View.getText();
            temp2View.getText();
            pressureView.getText();
            altitudeView.getText();
            humidityView.getText();
            uvView.getText();
            visibleView.getText();
            irView.getText();

            return null;
        }
    }
}
