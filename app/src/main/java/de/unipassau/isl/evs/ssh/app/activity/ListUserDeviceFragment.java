package de.unipassau.isl.evs.ssh.app.activity;


import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.NamedDTO;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.DELETE_USERDEVICE_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.FragmentArguments.GROUP_ARGUMENT_FRAGMENT;
import static de.unipassau.isl.evs.ssh.app.AppConstants.FragmentArguments.USER_DEVICE_ARGUMENT_FRAGMENT;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.USERNAME_SET;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.USER_DELETE;


/**
 * This fragment lists all user devices from a single group.
 * Gets the group through the {@link ListGroupFragment} and receives with this information the
 * user devices from the {@link AppUserConfigurationHandler}.
 *
 * @author Phil Werli
 * @see EditUserDeviceFragment
 */
public class ListUserDeviceFragment extends BoundFragment {
    private static final String TAG = ListUserDeviceFragment.class.getSimpleName();
    private UserDeviceListAdapter adapter;
    private ListView userDeviceList;
    /**
     * The group the fragment is created for.
     */
    private Group group;
    final private AppUserConfigurationHandler.UserInfoListener listener = new AppUserConfigurationHandler.UserInfoListener() {
        @Override
        public void userInfoUpdated(final UserConfigurationEvent event) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (event.getType().equals(UserConfigurationEvent.EventType.PUSH)) {
                        update();
                    } else if (event.getType().equals(USERNAME_SET)) {
                        if (event.wasSuccessful()) {
                            update();
                            showToast(R.string.edit_user_success);
                        } else {
                            showToast(R.string.could_not_edit_user);
                        }
                    } else if (event.getType().equals(USER_DELETE)) {
                        if (event.wasSuccessful()) {
                            update();
                            showToast(R.string.delete_user_success);
                        } else {
                            showToast(R.string.could_not_delete_user);
                        }
                    }
                }
            });
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_listuserdevicefromgroup, container, false);
    }

    /**
     * Updates the adapter.
     */
    private void update() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        buildView();
        container.require(AppUserConfigurationHandler.KEY).addUserInfoListener(listener);
    }

    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        group = (Group) getArguments().getSerializable(GROUP_ARGUMENT_FRAGMENT);
        final TextView groupName = (TextView) getActivity().findViewById(R.id.listuserdevice_groupname);
        groupName.setText(group.getName());

        userDeviceList = (ListView) getActivity().findViewById(R.id.listuserDeviceContainer);
        userDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // when a user clicks short on an item, he opens the ListUserDeviceFragment
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final AppMainActivity activity = (AppMainActivity) getActivity();
                if (activity != null && activity.hasPermission(Permission.CHANGE_USER_NAME) && activity.hasPermission(Permission.CHANGE_USER_GROUP)) {
                    UserDevice item = adapter.getItem(position);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(USER_DEVICE_ARGUMENT_FRAGMENT, item);
                    activity.showFragmentByClass(EditUserDeviceFragment.class, bundle);
                } else {
                    showToast(R.string.you_can_not_edit_user_devices);
                }
            }
        });

        userDeviceList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                UserDevice item = adapter.getItem(position);
                Bundle bundle = new Bundle();
                bundle.putSerializable(DELETE_USERDEVICE_DIALOG, item);
                final AppMainActivity activity = (AppMainActivity) getActivity();
                if (activity != null && activity.hasPermission(Permission.DELETE_USER)) {
                    showRemoveUserDeviceDialog(bundle);
                    return true;
                } else {
                    showToast(R.string.you_can_not_remove_users);
                    return false;
                }

            }
        });

        final FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.listuserdevice_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AppMainActivity activity = (AppMainActivity) getActivity();
                if (activity.hasPermission(Permission.ADD_USER)) {
                    ((AppMainActivity) getActivity()).showFragmentByClass(AddNewUserDeviceFragment.class);
                } else {
                    showToast(R.string.you_can_not_add_new_users);
                }
            }
        });

        adapter = new UserDeviceListAdapter();
        userDeviceList.setAdapter(adapter);
    }

    @Override
    public void onContainerDisconnected() {
        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler != null) {
            handler.removeUserInfoListener(listener);
        }
        super.onContainerDisconnected();
    }

    /**
     * Creates and returns a dialogs that gives the user the option to delete a user device.
     */
    private void showRemoveUserDeviceDialog(Bundle bundle) {
        final UserDevice userDevice = (UserDevice) bundle.getSerializable(DELETE_USERDEVICE_DIALOG);
        if (userDevice == null) {
            Log.i(TAG, "No device found.");
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Resources res = getResources();
        AlertDialog dialog = builder
                .setMessage(String.format(res.getString(R.string.deleteuserdevice_dialog_title), userDevice.getName()))
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
                        if (handler == null) {
                            Log.i(TAG, "Container not yet connected!");
                        } else if (((AppMainActivity) getActivity()).hasPermission(Permission.DELETE_USER)) {
                            handler.removeUserDevice(userDevice.getUserDeviceID());
                        } else {
                            showToast(R.string.you_can_not_remove_users);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
    }

    /**
     * Adapter used for {@link #userDeviceList}.
     */
    private class UserDeviceListAdapter extends BaseAdapter {
        private List<UserDevice> userDevices = new ArrayList<>();

        public UserDeviceListAdapter() {
            updateUserDeviceList();
        }

        @Override
        public void notifyDataSetChanged() {
            updateUserDeviceList();
            super.notifyDataSetChanged();
        }

        private void updateUserDeviceList() {
            final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);

            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }
            userDevices.clear();
            userDevices.addAll(handler.getAllGroupMembers(group));
            Collections.sort(userDevices, NamedDTO.COMPARATOR);
        }

        @Override
        public int getCount() {
            return userDevices.size();
        }

        @Override
        public UserDevice getItem(int position) {
            return userDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return userDevices.get(position).hashCode();
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
            LayoutInflater inflater = getActivity().getLayoutInflater();
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