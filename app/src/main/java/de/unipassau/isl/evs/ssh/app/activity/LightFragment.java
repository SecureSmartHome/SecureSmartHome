package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
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
import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * This fragment allows to display information contained in light messages which are received from the IncomingDispatcher.
 * Furthermore it generates a light messages as instructed by the UI and passes it to the OutgoingRouter.
 *
 * @author Phil Werli
 */
public class LightFragment extends BoundFragment {
    private LightListAdapter adapter;
    private final AppLightHandler.LightHandlerListener listener = new AppLightHandler.LightHandlerListener() {
        @Override
        public void statusChanged(Module module) {
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        getContainer().require(AppLightHandler.KEY).addListener(listener);

    }

    @Override
    public void onStop() {
        getContainer().require(AppLightHandler.KEY).removeListener(listener);
        super.onStop();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_light, container, false);
        ListView list = (ListView) root.findViewById(R.id.lightButtonContainer);

        adapter = new LightListAdapter(inflater);
        list.setAdapter(adapter);

        return root;
    }

    private class LightListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private List<Module> lightModules;

        public LightListAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
            updateModuleList();
        }

        @Override
        public void notifyDataSetChanged() {
            updateModuleList();
            super.notifyDataSetChanged();
        }

        private void updateModuleList() {
            final Map<Module, AppLightHandler.LightStatus> lightModulesStatus =
                    getContainer().require(AppLightHandler.KEY).getAllLightModuleStates();
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
                    getContainer().require(AppLightHandler.KEY).toggleLight(m);
                }
            });
            // set up TextView
            textView.setText(m.getName());

            // set up ImageView and button
            ImageView imageViewOn = (ImageView) lightButtonLayout.findViewById(R.id.lightButtonImageViewOn);
            ImageView imageViewOff = (ImageView) lightButtonLayout.findViewById(R.id.lightButtonImageViewOff);
            boolean isLightOn = getContainer().require(AppLightHandler.KEY).isLightOnCached(m);
            if (isLightOn) {
                imageViewOff.setVisibility(View.GONE);
                imageViewOn.setVisibility(View.VISIBLE);
                button.setText(getResources().getString(R.string.string_turn_off));
                button.setBackgroundColor(getResources().getColor(R.color.button_red));
            } else {
                imageViewOn.setVisibility(View.GONE);
                imageViewOff.setVisibility(View.VISIBLE);
                button.setText(getResources().getString(R.string.string_turn_on));
                button.setBackgroundColor(getResources().getColor(R.color.button_green));
            }

            return lightButtonLayout;
        }
    }
}