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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppLightHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.NamedDTO;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
 * This fragment allows to display the status of all registered lights.
 * The fragment gets the information from the {@link AppLightHandler}.
 *
 * @author Phil Werli
 * @see AppLightHandler
 */
public class LightFragment extends BoundFragment {
    private static final String TAG = LightFragment.class.getSimpleName();

    final private LightListAdapter adapter = new LightListAdapter();
    private final AppLightHandler.LightHandlerListener listener = new AppLightHandler.LightHandlerListener() {
        @Override
        public void statusChanged(Module module) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onLightSetFinished(final boolean wasSuccess) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!wasSuccess) {
                        showToast(R.string.could_not_switch_light);
                    }
                }
            });
        }

        @Override
        public void onLightGetFinished(boolean wasSuccess) {
            // this fragment does not handle explicit get requests
        }
    };

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        container.require(AppLightHandler.KEY).addListener(listener);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onContainerDisconnected() {
        final AppLightHandler component = getComponent(AppLightHandler.KEY);
        if (component != null) {
            component.removeListener(listener);
        }
        super.onContainerDisconnected();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final FrameLayout view = (FrameLayout) inflater.inflate(R.layout.fragment_light, container, false);
        final ListView lights = (ListView) view.findViewById(R.id.lightButtonContainer);
        lights.setAdapter(adapter);

        final FloatingActionButton fab = ((FloatingActionButton) view.findViewById(R.id.light_fab));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AppMainActivity activity = (AppMainActivity) getActivity();
                if (activity != null && activity.hasPermission(Permission.ADD_MODULE)) {
                    activity.showFragmentByClass(AddModuleFragment.class);
                } else {
                    showToast(R.string.you_can_not_add_new_modules);
                }
            }
        });
        return view;
    }

    /**
     * Adapter used for {@code lights}.
     */
    private class LightListAdapter extends BaseAdapter {
        private final List<Module> lightModules = new ArrayList<>();
        private LayoutInflater inflater;

        @Override
        public void notifyDataSetChanged() {
            AppLightHandler handler = getComponent(AppLightHandler.KEY);
            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }

            lightModules.clear();
            lightModules.addAll(handler.getAllLightModuleStates().keySet());
            Collections.sort(lightModules, NamedDTO.COMPARATOR);

            super.notifyDataSetChanged();
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
            return getItem(position).hashCode();
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
            final Module module = getItem(position);
            final AppLightHandler appLightHandler = getComponent(AppLightHandler.KEY);
            final boolean isLightOn = appLightHandler != null && appLightHandler.isLightOn(module);

            final LinearLayout lightButtonLayout;
            if (convertView == null) {
                if (inflater == null) {
                    inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                }
                //create LinearLayout as defined in layout file
                lightButtonLayout = (LinearLayout) inflater.inflate(R.layout.lightbutton, parent, false);
            } else {
                lightButtonLayout = (LinearLayout) convertView;
            }

            final Button button = (Button) lightButtonLayout.findViewById(R.id.lightButtonButton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AppLightHandler handler = getComponent(AppLightHandler.KEY);
                    if (handler != null) {
                        if (((AppMainActivity) getActivity()).hasPermission(Permission.SWITCH_LIGHT, module.getName())) {
                            handler.toggleLight(module);
                        } else {
                            showToast(R.string.you_can_not_switch_light);
                        }
                    } else {
                        Log.w(TAG, "Could not switch light, AppLightHandler not available");
                    }
                }
            });
            button.setEnabled(appLightHandler != null);

            final TextView textView = (TextView) lightButtonLayout.findViewById(R.id.lightButtonTextView);
            textView.setText(module.getName());

            // set up ImageView and button
            final ImageView imageViewOn = (ImageView) lightButtonLayout.findViewById(R.id.lightButtonImageViewOn);
            final ImageView imageViewOff = (ImageView) lightButtonLayout.findViewById(R.id.lightButtonImageViewOff);
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