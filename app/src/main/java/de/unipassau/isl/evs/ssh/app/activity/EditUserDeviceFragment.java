package de.unipassau.isl.evs.ssh.app.activity;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.PermissionDTO;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.ALL_GROUPS_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.EDIT_USERDEVICE_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.FragmentArguments.USER_DEVICE_ARGUMENT_FRAGMENT;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.PERMISSION_GRANT;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.PERMISSION_REVOKE;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.PUSH;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.USERNAME_SET;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.USER_SET_GROUP;

/**
 * This fragment lets the user view, edit and delete information regarding a single user device.
 * It gets its information through the {@link AppUserConfigurationHandler} which sends and receives necessary messages.
 *
 * @author Phil Werli
 * @see ListGroupFragment
 * @see ListUserDeviceFragment
 */
public class EditUserDeviceFragment extends BoundFragment {
    private static final String TAG = EditUserDeviceFragment.class.getSimpleName();

    /**
     * The device the fragment is created for.
     */
    private UserDevice device;
    private ListView userPermissionList;
    private PermissionListAdapter permissionListAdapter;

    final private AppUserConfigurationHandler.UserInfoListener listener = new AppUserConfigurationHandler.UserInfoListener() {
        @Override
        public void userInfoUpdated(final UserConfigurationEvent event) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (event.getType().equals(PUSH)) {
                        permissionListAdapter.notifyDataSetChanged();
                    } else if (event.getType().equals(PERMISSION_GRANT) && !event.wasSuccessful()) {
                        Toast.makeText(getActivity(), R.string.could_not_grant, Toast.LENGTH_SHORT).show();
                    } else if (event.getType().equals(PERMISSION_REVOKE) && !event.wasSuccessful()) {
                        Toast.makeText(getActivity(), R.string.could_not_revoke, Toast.LENGTH_SHORT).show();
                    } else if (event.getType().equals(USERNAME_SET) && !event.wasSuccessful()) {
                        Toast.makeText(getActivity(), R.string.could_not_edit_user_device_name, Toast.LENGTH_SHORT).show();
                    } else if (event.getType().equals(USER_SET_GROUP) && !event.wasSuccessful()) {
                        Toast.makeText(getActivity(), R.string.could_not_edit_user_device_group, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edituserdevice, container, false);
    }

    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        device = ((UserDevice) getArguments().getSerializable(USER_DEVICE_ARGUMENT_FRAGMENT));

        TextView deviceName = ((TextView) getActivity().findViewById(R.id.userdevice_user_name));
        deviceName.setText(device.getName());

        Resources res = getResources();

        TextView deviceID = ((TextView) getActivity().findViewById(R.id.userdevice_user_deviceid));
        deviceID.setText(String.format(res.getString(R.string.device_id), device.getUserDeviceID().getIDString()));

        TextView deviceGroup = ((TextView) getActivity().findViewById(R.id.userdevice_user_group));
        deviceGroup.setText(String.format(res.getString(R.string.is_in_group), device.getInGroup()));

        Button editButton = (Button) getActivity().findViewById(R.id.userdevice_edit_button);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putSerializable(EDIT_USERDEVICE_DIALOG, device);
                String[] groups = listGroups();
                if (groups != null) {
                    bundle.putStringArray(ALL_GROUPS_DIALOG, groups);
                }

                // check if user has permission to edit a group
                final AppMainActivity activity = (AppMainActivity) getActivity();
                if (activity != null && !activity.hasPermission(Permission.CHANGE_USER_NAME)) {
                    Toast.makeText(getActivity(), R.string.you_can_not_edit_user_devices, Toast.LENGTH_SHORT).show();
                } else if (activity != null && activity.hasPermission(Permission.CHANGE_USER_GROUP)) {
                    Toast.makeText(getActivity(), R.string.you_can_not_edit_groups, Toast.LENGTH_SHORT).show();
                } else {
                    showEditUserDeviceDialog(bundle);
                }
            }
        });

        userPermissionList = (ListView) getActivity().findViewById(R.id.listUserPermissionContainer);
        permissionListAdapter = new PermissionListAdapter();
        userPermissionList.setAdapter(permissionListAdapter);
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        buildView();
        container.require(AppUserConfigurationHandler.KEY).addUserInfoListener(listener);
    }

    @Override
    public void onContainerDisconnected() {
        AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler != null) {
            handler.removeUserInfoListener(listener);
        }
        super.onContainerDisconnected();
    }

    /**
     * @return A String Array of group names.
     */
    private String[] listGroups() {
        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        String[] groupNames = new String[0];
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
        } else {
            Set<Group> groups = handler.getAllGroups();
            groupNames = new String[groups.size()];
            int counter = 0;
            for (Group g :
                    groups) {
                groupNames[counter] = g.getName();
                counter++;
            }
        }
        return groupNames;
    }


    /**
     * Creates and shows a dialogs that gives the user the option to edit a group.
     */
    private void showEditUserDeviceDialog(Bundle bundle) {
        final UserDevice userDevice = (UserDevice) bundle.getSerializable(EDIT_USERDEVICE_DIALOG);
        if (userDevice == null) {
            Log.i(TAG, "No device found.");
            return;
        }

        final String[] groupNames = bundle.getStringArray(ALL_GROUPS_DIALOG);
        if (groupNames == null) {
            Log.i(TAG, "Empty bundle");
            return;
        }

        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
            return;
        }

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_edituserdevice, null);

        final TextView title = (TextView) dialogView.findViewById(R.id.editdevicedialog_title);
        title.setText(String.format(getResources().getString(R.string.edit_user_device), userDevice.getName()));

        final EditText userDeviceName = (EditText) dialogView.findViewById(R.id.editdevicedialog_username);
        userDeviceName.setText(userDevice.getName());

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, groupNames);
        final Spinner groupName = ((Spinner) dialogView.findViewById(R.id.editdevicedialog_spinner));
        groupName.setAdapter(adapter);
        groupName.setSelection(adapter.getPosition(userDevice.getInGroup()));

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Resources res = getResources();
        final AlertDialog editDialog = builder
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.edit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final AppMainActivity activity = (AppMainActivity) getActivity();
                        if (activity.hasPermission(Permission.CHANGE_USER_NAME) && activity.hasPermission(Permission.CHANGE_USER_GROUP)) {
                            String name = userDeviceName.getText().toString();
                            String group = ((String) groupName.getSelectedItem());
                            DeviceID userDeviceID = userDevice.getUserDeviceID();

                            // no permission check as method only gets called when user can edit user name / user group
                            handler.setUserName(userDeviceID, name);
                            handler.setUserGroup(userDeviceID, group);
                        } else {
                            Toast.makeText(getActivity(), R.string.you_can_not_edit_user_devices, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNeutralButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (((AppMainActivity) getActivity()).hasPermission(Permission.DELETE_USER)) {
                            handler.removeUserDevice(userDevice.getUserDeviceID());
                            String toastText = String.format(res.getString(R.string.device_removed), userDevice.getName());
                            Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), R.string.you_can_not_remove_users, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .create();

        // open the soft keyboard when dialog gets focused
        userDeviceName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    editDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        editDialog.show();
        userDeviceName.requestFocus();
    }

    /**
     * Adapter used for {@link #userPermissionList}.
     */
    private class PermissionListAdapter extends BaseAdapter {
        private List<PermissionDTO> allPermissions;
        private Set<PermissionDTO> userPermissions;

        public PermissionListAdapter() {
            updatePermissionList();
        }

        @Override
        public void notifyDataSetChanged() {
            updatePermissionList();
            super.notifyDataSetChanged();
        }

        private void updatePermissionList() {
            final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }
            userPermissions = handler.getPermissionForUser(device.getUserDeviceID());
            Set<PermissionDTO> tempPermissionList = handler.getAllPermissions();
            allPermissions = Lists.newArrayList(tempPermissionList);
            Collections.sort(allPermissions, new Comparator<PermissionDTO>() {
                @Override
                public int compare(PermissionDTO lhs, PermissionDTO rhs) {
                    if (lhs.getPermission() == null) {
                        return rhs.getPermission() == null ? 0 : 1;
                    }
                    if (rhs.getPermission() == null) {
                        return -1;
                    }
                    FragmentActivity activity = getActivity();
                    return lhs.toLocalizedString(activity).compareTo(rhs.toLocalizedString(activity));
                }
            });
        }

        @Override
        public int getCount() {
            if (allPermissions != null) {
                return allPermissions.size();
            } else {
                return 0;
            }
        }

        @Override
        public PermissionDTO getItem(int position) {
            if (allPermissions != null) {
                return allPermissions.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            final PermissionDTO item = getItem(position);
            if (item != null && item.getPermission() != null) {
                return item.getPermission().hashCode();
            } else {
                return 0;
            }
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            // TODO Phil: test what {@code false} does exactly (Phil, 2016-01-13)
            return areAllItemsEnabled();
        }

        /**
         * Creates a view for every permission registered in the system.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final PermissionDTO permission = getItem(position);
            LinearLayout permissionLayout;
            if (convertView == null) {
                permissionLayout = (LinearLayout) inflater.inflate(R.layout.permissionlayout, parent, false);
            } else {
                permissionLayout = (LinearLayout) convertView;
            }

            final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
            } else {
                TextView permissionText = (TextView) permissionLayout.findViewById(R.id.listpermission_permission_text);
                permissionText.setText(permission.toLocalizedString(getActivity()));

                final Button permissionButton = (Button) permissionLayout.findViewById(R.id.listpermission_permission_button);
                final boolean deviceHasPermission = userDeviceHasPermission(permission);
                if (deviceHasPermission) {
                    permissionButton.setText(getResources().getString(R.string.revoke));
                } else {
                    permissionButton.setText(getResources().getString(R.string.grant));
                }
                permissionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final AppMainActivity activity = (AppMainActivity) getActivity();
                        if (activity != null && activity.hasPermission(Permission.MODIFY_USER_PERMISSION)) {
                            if (!deviceHasPermission) {
                                handler.grantPermission(device.getUserDeviceID(), permission);
                                Log.i(TAG, permission.toLocalizedString(getActivity())
                                        + " granted for user device " + device.getName());
                            } else {
                                handler.revokePermission(device.getUserDeviceID(), permission);
                                Log.i(TAG, permission.toLocalizedString(getActivity())
                                        + " revoked for user device " + device.getName());
                            }
                            updatePermissionList();
                        } else {
                            Toast.makeText(getActivity(), R.string.you_can_not_set_permissions, Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                final TextView permissionDescription = (TextView) permissionLayout.findViewById(R.id.listpermission_permission_type);
                permissionDescription.setText(permission.getPermission().getLocalizedDescription(getActivity()));
            }

            return permissionLayout;
        }

        /**
         * @return {@code true} if a user device is granted a certain permission.
         */
        private boolean userDeviceHasPermission(PermissionDTO permission) {
            return userPermissions.contains(permission);
        }
    }
}
