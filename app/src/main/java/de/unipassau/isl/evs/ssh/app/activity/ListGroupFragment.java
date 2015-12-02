package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

/**
 * ListGroupFragment to show a list of all groups of user devices registered in the system.
 *
 * @author Phil Werli
 * @see ListUserDeviceFragment
 * @see EditUserDeviceFragment
 */
public class ListGroupFragment extends Fragment {
    private GroupListAdapter adapter;

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.listgroupfragment, container, false);
        ListView list = (ListView) root.findViewById(R.id.listGroupContainer);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            UserDevice item = adapter.getItem(position);
                                            //todo start ListUserDeviceFragment with userDevice item
                                        }
                                    }
        );
        adapter = new GroupListAdapter(inflater);
        list.setAdapter(adapter);

        return root;
    }

    private Container getContainer() {
        return ((MainActivity) getActivity()).getContainer();
    }

//    UsermanagementController.getGroups

    private class GroupListAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private List<UserDevice> groups;

        public GroupListAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
//            updateUserDeviceList();//fixme
        }

        @Override
        public int getCount() {
            return groups.size();
        }

        @Override
        public UserDevice getItem(int position) {
            return groups.get(position);
        }

        @Override
        public long getItemId(int position) {
            final UserDevice item = getItem(position);
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
            LinearLayout groupLayout = null;//fixme
            if (convertView == null) {
                groupLayout = (LinearLayout) inflater.inflate(R.layout.grouplayout, parent, false);
            } else {
                groupLayout = (LinearLayout) convertView;
            }

            final UserDevice userDevice = getItem(position);

            return groupLayout;
        }
    }
}
