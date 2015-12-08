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
import de.unipassau.isl.evs.ssh.core.database.dto.Permission;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.EDIT_PERMISSION_DIALOG;
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

    private PermissionListAdapter permissionListAdapter;
    /**
     * The device the fragment is created for.
     */
    private UserDevice device;

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
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_edituserdevice, container, false);
        device = ((UserDevice) getArguments().getSerializable(USER_DEVICE_ARGUMENT_FRAGMENT));

        TextView deviceName = ((TextView) root.findViewById(R.id.userdevice_user_name));
        deviceName.setText(device.getName());

        TextView deviceGroup = ((TextView) root.findViewById(R.id.userdevice_user_group));
        deviceGroup.setText(R.string.is_in_group + device.getInGroup() + R.string.dot);

        TextView deviceID = ((TextView) root.findViewById(R.id.userdevice_user_deviceid));
        deviceID.setText(R.string.device_id + device.getUserDeviceID().getIDString());

        ListView userPermissionList = (ListView) root.findViewById(R.id.listUserPermissionContainer);
        userPermissionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                                      // when a user clicks short on an item, he opens the ListUserDeviceFragment
                                                      @Override
                                                      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                          Permission item = permissionListAdapter.getItem(position);
                                                          Bundle bundle = new Bundle();
                                                          bundle.putSerializable(EDIT_PERMISSION_DIALOG, item);
                                                          createEditPermissionDialog(bundle).show();
                                                      }
                                                  }
        );
        permissionListAdapter = new PermissionListAdapter(inflater);
        userPermissionList.setAdapter(permissionListAdapter);

        return root;
    }

    /**
     * Creates and returns a dialogs that gives the user the option to edit a group.
     */
    private Dialog createEditPermissionDialog(Bundle bundle) {
        final Permission permission = (Permission) bundle.getSerializable(EDIT_PERMISSION_DIALOG);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_editgroup, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final AlertDialog dialog = builder.create();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        builder.setMessage(R.string.permission_dialog_title + " " + permission.getName())
                .setView(dialogView)
                .setPositiveButton(R.string.grant, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getComponent(AppUserConfigurationHandler.KEY).grantPermission(device, permission);
                    }
                })
                .setNegativeButton(R.string.revoke, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        getComponent(AppUserConfigurationHandler.KEY).revokePermission(device, permission);
                    }
                });
        return dialog;
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
            AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }
            permissions = handler.getPermissionForUser(device);
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
         * Creates a view for every registered permission.
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
            TextView textViewPermissionName = (TextView) permissionLayout.findViewById(R.id.listpermission_permission_name);
            textViewPermissionName.setText(permission.getName());

            TextView textViewPermissionType = ((TextView) permissionLayout.findViewById(R.id.listpermission_permission_type));
            textViewPermissionType.setText(createPermissionTypeText(permission));

            return permissionLayout;
        }

        /**
         * Creates a small description where a permission is
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
    }
}
