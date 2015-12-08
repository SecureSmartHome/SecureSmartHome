package de.unipassau.isl.evs.ssh.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.List;

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

        private void updateModuleList() {
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }
}