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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                        Toast.makeText(getActivity(), R.string.could_not_switch_light, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getActivity(), R.string.you_can_not_add_new_modules, Toast.LENGTH_SHORT).show();
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

            final Map<Module, AppLightHandler.LightStatus> lightModulesStatus = handler.getAllLightModuleStates();
            lightModules.clear();
            lightModules.addAll(lightModulesStatus.keySet());
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
            final Module item = getItem(position);
            if (item != null && item.getName() != null) {
                int hash = item.getName().hashCode();
                final AppLightHandler appLightHandler = getComponent(AppLightHandler.KEY);
                if (appLightHandler != null && appLightHandler.isLightOn(item)) {
                    hash = ~hash;
                }
                return hash;
            } else {
                return 0;
            }
        }

        @Override
        public boolean hasStableIds() {
            return false;
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
                            Toast.makeText(getActivity(), R.string.you_can_not_switch_light, Toast.LENGTH_SHORT).show();
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