package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.AppConstants;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

/**
 * This fragment lets the user view, edit and delete information regarding a single user device.
 * It gets its information through the {@link AppUserConfigurationHandler} which sends and receives necessary messages.
 *
 * @author Phil Werli
 * @see ListGroupFragment
 * @see ListUserDeviceFragment
 */
public class EditUserDeviceFragment extends BoundFragment {
    private PermissionListAdapter permissionListAdapter;

    private UserDevice device;

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
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_edituserdevice, container, false);
        ListView userPermissionList = (ListView) root.findViewById(R.id.listUserPermissionContainer);
        userPermissionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                      // when a user clicks short on an item, he opens the ListUserDeviceFragment
                                                      @Override
                                                      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                          Permission item = permissionListAdapter.getItem(position);
                                                          Bundle bundle = new Bundle();
                                                          bundle.putSerializable(AppConstants.Fragment_Arguments.ARGUMENT_FRAGMENT, item);
                                                          ((MainActivity) getActivity()).showFragmentByClass(ListUserDeviceFragment.class, bundle);
                                                      }
                                                  }
        );

        return super.onCreateView(inflater, container, savedInstanceState);
    }


    private class PermissionListAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private List<Permission> permissions;

        public PermissionListAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
            updatePermissionList();
        }

        @Override
        public void notifyDataSetChanged() {
            updatePermissionList();
            super.notifyDataSetChanged();
        }

        private void updatePermissionList() {
            permissions = getComponent(AppUserConfigurationHandler.KEY).getPermissionForUser(device);

            if (permissions != null) {
                Collections.sort(permissions, new Comparator<Permission>() {
                    @Override
                    public int compare(Permission lhs, Permission rhs) {
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
        }

        @Override
        public int getCount() {
            if (permissions != null) {
                return permissions.size();
            } else {
                return 0;
            }
        }

        @Override
        public Permission getItem(int position) {
            if (permissions != null) {
                return permissions.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            final Permission item = getItem(position);
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
         * Creates a view for every registered group.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {


            return null;
        }
    }
}
