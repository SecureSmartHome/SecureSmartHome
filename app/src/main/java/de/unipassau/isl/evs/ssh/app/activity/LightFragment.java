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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppLightHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * This fragment allows to display the status of all registered lights.
 * The fragment gets the information from the {@link AppLightHandler}.
 *
 * @see AppLightHandler
 * @author Phil Werli
 */
public class LightFragment extends BoundFragment {
    private static final String TAG = LightFragment.class.getSimpleName();
    private LightListAdapter adapter;
    private final AppLightHandler.LightHandlerListener listener = new AppLightHandler.LightHandlerListener() {
        @Override
        public void statusChanged(Module module) {
            adapter.notifyDataSetChanged();
        }
    };
    private ListView listView;

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        container.require(AppLightHandler.KEY).addListener(listener);
        adapter = new LightListAdapter();
        listView.setAdapter(adapter);
    }


    @Override
    public void onContainerDisconnected() {
        getComponent(AppLightHandler.KEY).removeListener(listener);
        super.onContainerDisconnected();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_light, container, false);
        listView = (ListView) root.findViewById(R.id.lightButtonContainer);
        FloatingActionButton fab = ((FloatingActionButton) root.findViewById(R.id.light_fab));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).showFragmentByClass(AddModuleFragment.class);
            }
        });
        return root;
    }

    private class LightListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private List<Module> lightModules;

        public LightListAdapter() {
            this.inflater = (LayoutInflater) getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            updateModuleList();
        }

        @Override
        public void notifyDataSetChanged() {
            updateModuleList();
            super.notifyDataSetChanged();
        }

        private void updateModuleList() {
            AppLightHandler handler = getComponent(AppLightHandler.KEY);
            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }

            final Map<Module, AppLightHandler.LightStatus> lightModulesStatus = handler.getAllLightModuleStates();
            lightModules = Lists.newArrayList(lightModulesStatus.keySet());
            Collections.sort(lightModules, new Comparator<Module>() {
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
            return lightModules.size();
        }

        @Override
        public Module getItem(int position) {
            return lightModules.get(position);
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
        public boolean hasStableIds() {
            return true;
        }

        /**
         * Creates a view for every registered light.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //create LinearLayout as defined in lightbutton.xml file
            LinearLayout lightButtonLayout;
            if (convertView == null) {
                lightButtonLayout = (LinearLayout) inflater.inflate(R.layout.lightbutton, parent, false);
            } else {
                lightButtonLayout = (LinearLayout) convertView;
            }

            final Module m = getItem(position);

            TextView textView = (TextView) lightButtonLayout.findViewById(R.id.lightButtonTextView);
            Button button = (Button) lightButtonLayout.findViewById(R.id.lightButtonButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getComponent(AppLightHandler.KEY).toggleLight(m);
                }
            });
            // set up TextView
            textView.setText(m.getName());

            // set up ImageView and button
            ImageView imageViewOn = (ImageView) lightButtonLayout.findViewById(R.id.lightButtonImageViewOn);
            ImageView imageViewOff = (ImageView) lightButtonLayout.findViewById(R.id.lightButtonImageViewOff);
            boolean isLightOn = getComponent(AppLightHandler.KEY).isLightOnCached(m);
            if (isLightOn) {
                imageViewOff.setVisibility(View.GONE);
                imageViewOn.setVisibility(View.VISIBLE);
                button.setText(getResources().getString(R.string.turn_off));
                button.setBackgroundColor(getResources().getColor(R.color.button_red));
            } else {
                imageViewOn.setVisibility(View.GONE);
                imageViewOff.setVisibility(View.VISIBLE);
                button.setText(getResources().getString(R.string.turn_on));
                button.setBackgroundColor(getResources().getColor(R.color.button_green));
            }

            return lightButtonLayout;
        }
    }
}