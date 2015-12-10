package de.unipassau.isl.evs.ssh.app.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.ALL_GROUPS_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.EDIT_USERDEVICE_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.Fragment_Arguments.USER_DEVICE_ARGUMENT_FRAGMENT;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_edituserdevice, container, false);
        device = ((UserDevice) getArguments().getSerializable(USER_DEVICE_ARGUMENT_FRAGMENT));

        TextView deviceName = ((TextView) root.findViewById(R.id.userdevice_user_name));
        deviceName.setText(device.getName());

        TextView deviceGroup = ((TextView) root.findViewById(R.id.userdevice_user_group));
        deviceGroup.setText(getResources().getString(R.string.is_in_group) + device.getInGroup() + getResources().getString(R.string.dot));

        TextView deviceID = ((TextView) root.findViewById(R.id.userdevice_user_deviceid));
        deviceID.setText(getResources().getString(R.string.device_id) + device.getUserDeviceID().getIDString());

        Button editButton = (Button) root.findViewById(R.id.userdevice_edit_button);
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

        ListView userPermissionList = (ListView) root.findViewById(R.id.listUserPermissionContainer);
        PermissionListAdapter permissionListAdapter = new PermissionListAdapter(inflater);
        userPermissionList.setAdapter(permissionListAdapter);

        return root;
    }

    /**
     * @return A String Array of group names.
     */
    private String[] listGroups() {
        List<Group> groups = getComponent(AppUserConfigurationHandler.KEY).getAllGroups();
        if (groups == null) {
            return null;
        }
        String[] groupNames = new String[groups.size()];
        int counter = 0;
        for (Group g :
                groups) {
            groupNames[counter] = g.getName();
            counter++;
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
        View dialogView = inflater.inflate(R.layout.dialog_edituserdevice, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText userDeviceName = (EditText) dialogView.findViewById(R.id.edit_userdevice_dialog_userdevice_name);
        List<String> groupList = new ArrayList<>(Arrays.asList(groupNames));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, groupList);
        final Spinner groupName = ((Spinner) dialogView.findViewById(R.id.edit_userdevice_dialog_spinner));
        groupName.setAdapter(adapter);

        final AlertDialog dialog = builder.create();
        builder.setMessage(R.string.edit_group_dialog_title)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.edit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String name = userDeviceName.getText().toString();
                        String group = ((String) groupName.getSelectedItem());
                        getComponent(AppUserConfigurationHandler.KEY).editUserDevice(userDevice,
                                new UserDevice(name, group, userDevice.getUserDeviceID()));
                        String toastText = "Device " + name + " edited.";
                        Toast toast = Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                })
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getComponent(AppUserConfigurationHandler.KEY).removeUserDevice(userDevice);
                        String toastText = "Device " + userDevice.getName() + " removed.";
                        Toast toast = Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                })
                .create()
                .show();
        userDeviceName.requestFocus();

//        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
//        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
//        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }


    private class PermissionListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private List<Permission> allPermissions;
        private List<Permission> userPermissions;

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
            AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }
            userPermissions = handler.getPermissionForUser(device);

            List<Permission> tempPermissionList = handler.getAllPermissions();

            if (tempPermissionList != null) {
                allPermissions = Lists.newArrayList(tempPermissionList);
                Collections.sort(allPermissions, new Comparator<Permission>() {
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
         * Creates a view for every permission registered in the system. If the user device is granted a permission
         * the switch button is {@code On}.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Permission permission = getItem(position);
            LinearLayout permissionLayout;
            if (convertView == null) {
                permissionLayout = (LinearLayout) inflater.inflate(R.layout.permissionlayout, parent, false);
            } else {
                permissionLayout = (LinearLayout) convertView;
            }

            final Switch permissionSwitch = (Switch) permissionLayout.findViewById(R.id.listpermission_permission_switch);
            permissionSwitch.setText(permission.getName());
            permissionSwitch.setChecked(userDeviceHasPermission(permission));
            permissionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        getComponent(AppUserConfigurationHandler.KEY).grantPermission(device, permission);
                        Log.i(TAG, permission.getName() + " granted for user device " + device.getName());
                    } else {
                        getComponent(AppUserConfigurationHandler.KEY).revokePermission(device, permission);
                        Log.i(TAG, permission.getName() + " revoked for user device " + device.getName());
                    }
                    permissionSwitch.toggle();
                }
            });

            TextView textViewPermissionType = ((TextView) permissionLayout.findViewById(R.id.listpermission_permission_type));
            textViewPermissionType.setText(createPermissionTypeText(permission));

            return permissionLayout;
        }

        /**
         * Creates a small description where a permission is
         *
         * @param permission The permission the text is created for.
         * @return the text to display.
         */
        private String createPermissionTypeText(Permission permission) {
            String output;
            String moduleName = permission.getModuleName();
            if (moduleName != null) {
                output = "This permission is connected to " + moduleName;
            } else {
                output = "This permission is not connected to any module.";
            }
            return output;
        }

        /**
         * @return If a user device is granted a certain permission.
         */
        private boolean userDeviceHasPermission(Permission permission) {
            return userPermissions.contains(permission);
        }
    }
}
