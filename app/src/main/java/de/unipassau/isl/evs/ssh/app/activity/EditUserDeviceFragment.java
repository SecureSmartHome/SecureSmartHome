package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Dialog;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.ALL_GROUPS_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.EDIT_USERDEVICE_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.FragmentArguments.USER_DEVICE_ARGUMENT_FRAGMENT;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.GRANT_USER_PERMISSION;

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
    private PermissionListAdapter permissionListAdapter;

    final private AppUserConfigurationHandler.UserInfoListener listener = new AppUserConfigurationHandler.UserInfoListener() {
        @Override
        public void userInfoUpdated(final UserConfigurationEvent event) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (event.getType().equals(UserConfigurationEvent.EventType.PUSH)) {
                        permissionListAdapter.notifyDataSetChanged();
                    } else if (event.getType().equals(UserConfigurationEvent.EventType.PERMISSION_GRANT) && !event.wasSuccessful()) {
                        Toast.makeText(getActivity(), R.string.could_not_grant, Toast.LENGTH_SHORT).show();
                    } else if (event.getType().equals(UserConfigurationEvent.EventType.PERMISSION_REVOKE) && !event.wasSuccessful()) {
                        Toast.makeText(getActivity(), R.string.could_not_revoke, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    };

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
                createEditUserDeviceDialog(bundle).show();
            }
        });

        ListView userPermissionList = (ListView) getActivity().findViewById(R.id.listUserPermissionContainer);
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
            handler.addUserInfoListener(listener);
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
            List<Group> groups = handler.getAllGroups();
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
     * Creates and returns a dialogs that gives the user the option to edit a group.
     */
    private Dialog createEditUserDeviceDialog(Bundle bundle) {
        final UserDevice userDevice = (UserDevice) bundle.getSerializable(EDIT_USERDEVICE_DIALOG);

        String[] groupNames = bundle.getStringArray(ALL_GROUPS_DIALOG);
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final ViewGroup parent = (ViewGroup) getActivity().findViewById(R.id.fragment_edit_user);
        View dialogView = inflater.inflate(R.layout.dialog_edituserdevice, parent, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText userDeviceName = (EditText) dialogView.findViewById(R.id.editdevicedialog_username);
        List<String> groupList = new ArrayList<>();
        if (groupNames == null) {
            Log.i(TAG, "Empty bundle");
        } else {
            Collections.addAll(groupList, groupNames);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, groupList);
        final Spinner groupName = ((Spinner) dialogView.findViewById(R.id.editdevicedialog_spinner));
        groupName.setAdapter(adapter);

        final AlertDialog dialog = builder.create();
        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
        } else if (userDevice == null) {
            Log.i(TAG, "No device found.");
        } else {
            builder.setMessage(R.string.edit_group_dialog_title)
                    .setView(dialogView)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.edit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            String name = userDeviceName.getText().toString();
                            String group = ((String) groupName.getSelectedItem());
                            handler.setUserGroup(userDevice.getUserDeviceID(), group);
                            handler.setUserName(userDevice.getUserDeviceID(), name);
                            String toastText = "Device " + name + " edited.";
                            Toast toast = Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    })
                    .setNeutralButton(R.string.remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handler.removeUserDevice(userDevice.getUserDeviceID());
                            String toastText = "Device " + userDevice.getName() + " removed.";
                            Toast toast = Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    })
                    .create()
                    .show();
        }
        userDeviceName.requestFocus();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    private class PermissionListAdapter extends BaseAdapter {
        private List<Permission> allPermissions;
        private List<Permission> userPermissions;

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
            userPermissions = handler.getPermissionForUser(device);

            List<Permission> tempPermissionList = handler.getAllPermissions();

            allPermissions = Lists.newArrayList(tempPermissionList);
            Collections.sort(allPermissions, new Comparator<Permission>() {
                @Override
                public int compare(Permission lhs, Permission rhs) {
                    if (lhs.getPermission() == null) {
                        return rhs.getPermission() == null ? 0 : 1;
                    }
                    if (rhs.getPermission() == null) {
                        return -1;
                    }
                    FragmentActivity activity = getActivity();
                    return lhs.getPermission().toLocalizedString(activity).compareTo(rhs.getPermission().toLocalizedString(activity));
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
        public Permission getItem(int position) {
            if (allPermissions != null) {
                return allPermissions.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            final Permission item = getItem(position);
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
        public boolean areAllItemsEnabled() {
            return userMayEdit();
        }

        @Override
        public boolean isEnabled(int position) {
            return areAllItemsEnabled();
        }

        /**
         * Creates a view for every permission registered in the system. If the user device is granted a permission
         * the switch button is {@code On}.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final Permission permission = getItem(position);
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
                permissionText.setText(permission.getPermission().toLocalizedString(getActivity()));

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
                        if (userMayEdit()) {
                            if (!deviceHasPermission) {
                                handler.grantPermission(device.getUserDeviceID(), permission);
                                Log.i(TAG, permission.getPermission().toLocalizedString(getActivity())
                                        + " granted for user device " + device.getName());
                            } else {
                                handler.revokePermission(device.getUserDeviceID(), permission);
                                Log.i(TAG, permission.getPermission().toLocalizedString(getActivity())
                                        + " revoked for user device " + device.getName());
                            }
                            updatePermissionList();
                        } else {
                            Toast.makeText(getActivity(), "Missing permission.", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

                final TextView permissionDescription = (TextView) permissionLayout.findViewById(R.id.listpermission_permission_type);
                permissionDescription.setText(permission.getPermission().getLocalizedDescription(getActivity()));
            }

            return permissionLayout;
        }

        /**
         * Creates a small description where a permission is
         *
         * @param permission The permission the text is created for.
         * @return the text to display.
         */
        private String createLocalizedPermissionTypeText(Permission permission) {
            String output;
            String moduleName = permission.getModuleName();
            if (moduleName != null) {
                output = String.format(getResources().getString(R.string.permission_connected_to), moduleName);
            } else {
                output = getResources().getString(R.string.permission_not_connected_to);
            }
            return output;
        }

        /**
         * @return {@code true} if the current user has the permission to grant other users permissions.
         */
        private boolean userMayEdit() {
            return userDeviceHasPermission(new Permission(GRANT_USER_PERMISSION));
        }

        /**
         * @return {@code true} if a user device is granted a certain permission.
         */
        private boolean userDeviceHasPermission(Permission permission) {
            return userPermissions.contains(permission);
        }
    }
}
