package de.unipassau.isl.evs.ssh.app.activity;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.DELETE_USERDEVICE_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.EDIT_PERMISSION_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.Fragment_Arguments.GROUP_ARGUMENT_FRAGMENT;


/**
 * This fragment lists all user devices from a single group.
 * Gets the group through the {@link ListGroupFragment ListGroupFragment} and receives with this information the
 * user devices from the {@link AppUserConfigurationHandler AppUserConfigurationHandler}.
 *
 * @author Phil Werli
 * @see EditUserDeviceFragment
 */
public class ListUserDeviceFragment extends BoundFragment {
    private static final String TAG = ListUserDeviceFragment.class.getSimpleName();
    private UserDeviceListAdapter adapter;
    /**
     * The group the fragment is created for.
     */
    private Group group;

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
    }

    @Override
    public void onContainerDisconnected() {
        super.onContainerDisconnected();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_listuserdevicefromgroup, container, false);
        group = ((Group) getArguments().getSerializable(GROUP_ARGUMENT_FRAGMENT));

        TextView groupName = (TextView) root.findViewById(R.id.listuserdevice_groupname);
        groupName.setText(group.getName());

        ListView userDeviceList = (ListView) root.findViewById(R.id.listuserDeviceContainer);
        userDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                  // when a user clicks short on an item, he opens the ListUserDeviceFragment
                                                  @Override
                                                  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                      UserDevice item = adapter.getItem(position);
                                                      Bundle bundle = new Bundle();
                                                      bundle.putSerializable(EDIT_PERMISSION_DIALOG, item);
                                                      ((MainActivity) getActivity()).showFragmentByClass(EditUserDeviceFragment.class, bundle);
                                                  }
                                              }
        );
        userDeviceList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                UserDevice item = adapter.getItem(position);
                Bundle bundle = new Bundle();
                bundle.putSerializable(DELETE_USERDEVICE_DIALOG, item);
                createDeleteUserDeviceDialog(bundle);
                return true;

            }
        });

        FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.listuserdevice_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).showFragmentByClass(AddNewUserDeviceFragment.class);
            }
        });

        adapter = new UserDeviceListAdapter(inflater);
        userDeviceList.setAdapter(adapter);

        return root;
    }

    /**
     * Creates and returns a dialogs that gives the user the option to delete a user device.
     */
    private Dialog createDeleteUserDeviceDialog(Bundle bundle) {
        final UserDevice userDevice = (UserDevice) bundle.getSerializable(EDIT_PERMISSION_DIALOG);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_editgroup, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final AlertDialog dialog = builder.create();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        builder.setMessage(R.string.deleteuserdevice_dialog_title + " " + userDevice.getName() + "?")
                .setView(dialogView)
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getComponent(AppUserConfigurationHandler.KEY).removeUserDevice(userDevice);
                    }
                })
                .setNegativeButton(R.string.revoke, null);
        return dialog;
    }


    private class UserDeviceListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private List<UserDevice> userDevices;

        public UserDeviceListAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
            updateUserDeviceList();
        }

        @Override
        public void notifyDataSetChanged() {
            updateUserDeviceList();
            super.notifyDataSetChanged();
        }

        private void updateUserDeviceList() {
            AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);

            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }
            userDevices = handler.getAllGroupMembers(group);

            if (userDevices != null) {
                Collections.sort(userDevices, new Comparator<UserDevice>() {
                    @Override
                    public int compare(UserDevice lhs, UserDevice rhs) {
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
            if (userDevices != null) {
                return userDevices.size();
            } else {
                return 0;
            }
        }

        @Override
        public UserDevice getItem(int position) {
            if (userDevices != null) {
                return userDevices.get(position);
            } else {
                return null;
            }
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

        /**
         * Creates a view for every device in the specific group.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // the UserDevice the list item is created for
            final UserDevice device = getItem(position);

            LinearLayout userDeviceLayout;
            if (convertView == null) {
                userDeviceLayout = (LinearLayout) inflater.inflate(R.layout.userdevicelayout, parent, false);
            } else {
                userDeviceLayout = (LinearLayout) convertView;
            }

            TextView userDeviceName = ((TextView) userDeviceLayout.findViewById(R.id.userdevicelistitem_device_name));
            userDeviceName.setText(device.getName());

            TextView userDeviceDeviceID = ((TextView) userDeviceLayout.findViewById(R.id.userdevicelistitem_device_information));
            userDeviceDeviceID.setText(device.getUserDeviceID().toString());

            return userDeviceLayout;
        }
    }
}