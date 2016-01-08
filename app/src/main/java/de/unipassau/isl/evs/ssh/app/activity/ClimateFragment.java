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
 * @author Andreas Bucher
 */
public class ClimateFragment extends BoundFragment {
    private static final String TAG = ClimateFragment.class.getSimpleName();
    private ClimateListAdapter adapter;
    private final AppClimateHandler.ClimateHandlerListener listener = new AppClimateHandler.ClimateHandlerListener() {
        @Override
        public void statusChanged(Module module) {
            adapter.notifyDataSetChanged();
        }
    };
    private ListView listView;
    private int counter = 0;

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
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_climate, container, false);
        listView = (ListView) root.findViewById(R.id.climateSensorContainer);
        return root;
    }

    private class ClimateListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private List<Module> climateSensorModules;

        public ClimateListAdapter() {
            this.inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            updateModuleList();
        }

        @Override
        public void notifyDataSetChanged() {
            updateModuleList();
            super.notifyDataSetChanged();
        }

        /**
         * Creates a Map with Modules and links their ClimateStatus to them
         */
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

        /**
         * With getCount the length of the climateSensorModules List is returned.
         *
         * @return Amount of climateSensorModules
         */
        @Override
        public int getCount() {
            return climateSensorModules.size();
        }

        /**
         * Gets the Module on position position of the climateSensorModules List.
         *
         * @param position of Module in List
         * @return climateSensorModule on position position
         */
        @Override
        public Module getItem(int position) {
            return climateSensorModules.get(position);
        }

        /**
         * Gets the NameID of the Module on position position.
         *
         * @param position of Module in List
         * @return NameID of Module
         */
        @Override
        public long getItemId(int position) {
            final Module item = getItem(position);
            if (item != null && item.getName() != null) {
                return item.getName().hashCode();
            } else {
                return 0;
            }
        }

        /**
         * Fills the ClimateFragment with the current SensorData.
         *
         * @param position    of Module in List
         * @param convertView child element of ListView
         * @param parent      ListView being populated
         * @return New View to draw
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout climateSensorLayout;

            if (convertView == null) {
                climateSensorLayout = (LinearLayout) inflater.inflate(R.layout.climatesensor, parent, false);
            } else {
                climateSensorLayout = (LinearLayout) convertView;
            }

            final Module m = getItem(position);

            /*
            If there is more than one ClimateSensor, then a line between two blocks of SensorData
            should be drawn, to separate them visually.
            */
            if (counter > 2) {
                View divider = climateSensorLayout.findViewById(R.id.climatesensor_divider);
                divider.setVisibility(View.VISIBLE);
            }

            /*
            Draw the WeatherSensorData to the Module which is part of the Fragment.
             */
            TextView climateSensorView = (TextView) climateSensorLayout.findViewById(R.id.climateSensor);
            String name = m.getName();
            climateSensorView.setText(name);

            // TODO check if units are correct
            TextView temp1View = (TextView) climateSensorLayout.findViewById(R.id.temp1);
            double temp1 = getComponent(AppClimateHandler.KEY).getTemp1(m);
            temp1View.setText(Double.toString(temp1) + " °C");

            TextView temp2View = (TextView) climateSensorLayout.findViewById(R.id.temp2);
            double temp2 = getComponent(AppClimateHandler.KEY).getTemp2(m);
            temp2View.setText(Double.toString(temp2) + " °C");

            TextView pressureView = (TextView) climateSensorLayout.findViewById(R.id.pressure);
            double pressure = getComponent(AppClimateHandler.KEY).getPressure(m);
            pressureView.setText(Double.toString(pressure) + " Pa");

            TextView altitudeView = (TextView) climateSensorLayout.findViewById(R.id.altitude);
            double altitude = getComponent(AppClimateHandler.KEY).getAltitude(m);
            altitudeView.setText(Double.toString(altitude) + " m");

            TextView humidityView = (TextView) climateSensorLayout.findViewById(R.id.humidity);
            double humidity = getComponent(AppClimateHandler.KEY).getHumidity(m);
            humidityView.setText(Double.toString(humidity) + " %");

            TextView uvView = (TextView) climateSensorLayout.findViewById(R.id.uv);
            double uv = getComponent(AppClimateHandler.KEY).getUv(m);
            uvView.setText(Double.toString(uv) + " W/m²");

            TextView visibleView = (TextView) climateSensorLayout.findViewById(R.id.visible);
            int visible = getComponent(AppClimateHandler.KEY).getVisible(m);
            //cd = Candela SI-unit for light intensity
            visibleView.setText(Integer.toString(visible) + " cd");

            TextView irView = (TextView) climateSensorLayout.findViewById(R.id.ir);
            int ir = getComponent(AppClimateHandler.KEY).getIr(m);
            irView.setText(Integer.toString(ir) + " cd");

            counter++;
            return climateSensorLayout;
        }
    }
}