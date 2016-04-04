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

package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppClimateHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.NamedDTO;

/**
 * This Fragment displays current data of the WeatherSensor. {@link AppClimateHandler} delivers this data.
 * It is also possible to show data from more than one WeatherSensor.
 *
 * @author Andreas Bucher
 * @see AppClimateHandler
 */
public class ClimateFragment extends BoundFragment {
    private static final String TAG = ClimateFragment.class.getSimpleName();

    private ClimateListAdapter adapter;
    private final AppClimateHandler.ClimateHandlerListener listener = new AppClimateHandler.ClimateHandlerListener() {
        @Override
        public void statusChanged() {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    update();
                }
            });
        }
    };

    private ListView climateSensorList;
    private TextView header;

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        container.require(AppClimateHandler.KEY).addListener(listener);
        adapter = new ClimateListAdapter();
        climateSensorList.setAdapter(adapter);
        update();
    }

    @Override
    public void onContainerDisconnected() {
        AppClimateHandler handler = getComponent(AppClimateHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
        } else {
            handler.removeListener(listener);
        }
        super.onContainerDisconnected();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RelativeLayout root = (RelativeLayout) inflater.inflate(R.layout.fragment_climate, container, false);
        climateSensorList = (ListView) root.findViewById(R.id.climateSensorContainer);

        final FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.climate_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppClimateHandler handler = getComponent(AppClimateHandler.KEY);
                if (handler == null) {
                    Log.i(TAG, "Container not yet connected!");
                    return;
                }
                handler.refreshAllWeatherBoards();
            }
        });
        header = (TextView) root.findViewById(R.id.climate_header);
        return root;
    }

    private void update() {

        /**
         * Creates a Map with Modules and links their ClimateStatus to them
         */
        AppClimateHandler handler = getComponent(AppClimateHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
            return;
        }

        handler.maybeUpdateModules();
        final Set<Module> modules = handler.getAllClimateModuleStates().keySet();
        if (modules.size() < 1) {
            header.setText(getResources().getString(R.string.no_climate_sensor_connected));
        } else {
            header.setText(getResources().getString(R.string.connected_weather_boards));
        }
        adapter.update(modules);
    }

    /**
     * Adapter used for {@link #climateSensorList}
     */
    private class ClimateListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private List<Module> climateSensorModules = new ArrayList<>();

        public ClimateListAdapter() {
            this.inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        private void update(Set<Module> climateSensors) {
            climateSensorModules.clear();
            climateSensorModules.addAll(climateSensors);
            Collections.sort(climateSensorModules, NamedDTO.COMPARATOR);
            notifyDataSetChanged();
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
            return getItem(position).getName().hashCode();
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
            Draw the WeatherSensorData to the Module which is part of the Fragment.
             */
            TextView climateSensorView = (TextView) climateSensorLayout.findViewById(R.id.climateSensor);
            String name = m.getName();
            climateSensorView.setText(name);

            AppClimateHandler handler = getComponent(AppClimateHandler.KEY);

            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
            } else {
                TextView temp1View = (TextView) climateSensorLayout.findViewById(R.id.temp1);
                double temp1 = handler.getTemp1(m);
                temp1View.setText(String.format(getResources().getString(R.string.si_degree), temp1));

                TextView temp2View = (TextView) climateSensorLayout.findViewById(R.id.temp2);
                double temp2 = handler.getTemp2(m);
                temp2View.setText(String.format(getResources().getString(R.string.si_degree), temp2));

                TextView pressureView = (TextView) climateSensorLayout.findViewById(R.id.pressure);
                double pressure = handler.getPressure(m);
                pressureView.setText(String.format(getResources().getString(R.string.si_pressure), pressure));

                TextView altitudeView = (TextView) climateSensorLayout.findViewById(R.id.altitude);
                double altitude = handler.getAltitude(m);
                altitudeView.setText(String.format(getResources().getString(R.string.si_altitude), altitude));

                TextView humidityView = (TextView) climateSensorLayout.findViewById(R.id.humidity);
                double humidity = handler.getHumidity(m);
                humidityView.setText(String.format(getResources().getString(R.string.si_humidity), humidity));

                TextView uvView = (TextView) climateSensorLayout.findViewById(R.id.uv);
                double uv = handler.getUv(m);
                uvView.setText(String.format("%2.2f", uv));

                TextView visibleView = (TextView) climateSensorLayout.findViewById(R.id.visible);
                int visible = handler.getVisible(m);
                visibleView.setText(String.format(getResources().getString(R.string.si_visible), visible));

                TextView irView = (TextView) climateSensorLayout.findViewById(R.id.ir);
                int ir = handler.getIr(m);
                irView.setText(String.format(getResources().getString(R.string.si_visible), ir));
            }
            return climateSensorLayout;
        }
    }
}