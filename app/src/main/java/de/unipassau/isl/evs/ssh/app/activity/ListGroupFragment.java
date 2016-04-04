/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.app.activity;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;

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
import de.unipassau.isl.evs.ssh.core.naming.DeviceID;
import de.unipassau.isl.evs.ssh.core.naming.NamingManager;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.EDIT_GROUP_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.FragmentArguments.GROUP_ARGUMENT_FRAGMENT;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.GROUP_DELETE;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.GROUP_SET_NAME;

/**
 * This fragment shows a list of all groups of user devices registered in the system.
 * It gets its information through the {@link AppUserConfigurationHandler} which sends and receives necessary messages.
 *
 * @author Phil Werli
 * @see ListUserDeviceFragment
 * @see EditUserDeviceFragment
 */
public class ListGroupFragment extends BoundFragment {
    private static final String TAG = ListGroupFragment.class.getSimpleName();
    private GroupListAdapter adapter;
    /**
     * Listener for either updating the adapter or showing a {@link Toast}.
     */
    final private AppUserConfigurationHandler.UserInfoListener listener = new AppUserConfigurationHandler.UserInfoListener() {
        @Override
        public void userInfoUpdated(final UserConfigurationEvent event) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (event.getType().equals(UserConfigurationEvent.EventType.PUSH)) {
                        update();
                    } else if (event.getType().equals(UserConfigurationEvent.EventType.GROUP_ADD)) {
                        if (event.wasSuccessful()) {
                            int resId = R.string.group_created;
                            showToast(resId);
                            update();
                        } else {
                            showToast(R.string.could_not_edit_group);
                        }
                    } else if (event.getType().equals(GROUP_SET_NAME)) {
                        if (event.wasSuccessful()) {
                            showToast(R.string.edit_group_success);
                            update();
                        } else {
                            showToast(R.string.could_not_edit_group);
                        }
                    } else if (event.getType().equals(GROUP_DELETE)) {
                        if (event.wasSuccessful()) {
                            showToast(R.string.delete_group_success);
                            update();
                        } else {
                            showToast(R.string.could_not_delete_group);
                        }
                    }
                }
            });
        }
    };
    private ListView groupList;

    /**
     * Updates the adapter.
     */
    private void update() {
        adapter.notifyDataSetChanged();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_listgroup, container, false);
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
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        final AppMainActivity activity = (AppMainActivity) getActivity();
        groupList = (ListView) activity.findViewById(R.id.listGroupContainer);
        groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                             @Override
                                             public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                 Group item = adapter.getItem(position);
                                                 Bundle bundle = new Bundle();
                                                 bundle.putSerializable(GROUP_ARGUMENT_FRAGMENT, item);
                                                 activity.showFragmentByClass(ListUserDeviceFragment.class, bundle);
                                             }
                                         }
        );
        groupList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (activity.hasPermission(Permission.CHANGE_GROUP_NAME) && activity.hasPermission(Permission.CHANGE_GROUP_TEMPLATE)) {
                    Group item = adapter.getItem(position);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(EDIT_GROUP_DIALOG, item);
                    activity.showFragmentByClass(EditGroupFragment.class, bundle);
                    return true;
                } else {
                    showToast(R.string.you_can_not_edit_groups);
                    return false;
                }
            }
        });
        FloatingActionButton fab = ((FloatingActionButton) activity.findViewById(R.id.addgroup_fab));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.hasPermission(Permission.ADD_GROUP)) {
                    activity.showFragmentByClass(AddGroupFragment.class);
                } else {
                    showToast(R.string.you_can_not_add_groups);
                }
            }
        });
        adapter = new GroupListAdapter();
        groupList.setAdapter(adapter);
    }

    /**
     * Adapter used for {@link #groupList}.
     */
    private class GroupListAdapter extends BaseAdapter {
        private List<Group> groups = new ArrayList<>();

        public GroupListAdapter() {
            updateGroupList();
        }

        @Override
        public void notifyDataSetChanged() {
            updateGroupList();
            super.notifyDataSetChanged();
        }

        private void updateGroupList() {
            final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);

            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }
            groups.clear();
            groups.addAll(handler.getAllGroups());
            Collections.sort(groups, NamedDTO.COMPARATOR);
        }

        @Override
        public int getCount() {
            return groups.size();
        }

        @Override
        public Group getItem(int position) {
            return groups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
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
            // the Group the list item is created for
            final Group group = getItem(position);

            final LinearLayout groupLayout;
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            if (convertView == null) {
                groupLayout = (LinearLayout) inflater.inflate(R.layout.grouplayout, parent, false);
            } else {
                groupLayout = (LinearLayout) convertView;
            }

            if (isCurrentUser(group)) {
                groupLayout.setBackgroundColor(getResources().getColor(R.color.color_own_id));
            } else {
                groupLayout.setBackgroundColor(0);
            }

            final TextView textViewGroupName = (TextView) groupLayout.findViewById(R.id.listgroup_group_name);
            textViewGroupName.setText(group.getName());

            final TextView textViewGroupMembers = (TextView) groupLayout.findViewById(R.id.listgroup_group_members);
            textViewGroupMembers.setText(createLocalizedGroupMemberText(group));

            return groupLayout;
        }

        /**
         * @return {@code true} if the current user is in the group the listview item is created for.
         */
        private boolean isCurrentUser(Group group) {
            final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
            final NamingManager namingManager = getComponent(NamingManager.KEY);
            if (handler == null || namingManager == null) {
                Log.i(TAG, "Container not yet connected!");
                return false;
            }
            final DeviceID ownID = namingManager.getOwnID();
            for (UserDevice groupMember : handler.getAllGroupMembers(group)) {
                if (groupMember.getUserDeviceID().equals(ownID)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Creates a text describes a group in one sentence.
         *
         * @param group The group the text is created for.
         * @return the text to display
         */
        private String createLocalizedGroupMemberText(Group group) {
            final Resources res = getResources();
            String groupMemberText = res.getString(R.string.group_has_no_members);
            final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
            } else {
                final ArrayList<UserDevice> groupMembers = Lists.newArrayList(handler.getAllGroupMembers(group));
                int numberOfMembers = groupMembers.size();
                if (numberOfMembers >= 3) {
                    groupMemberText = String.format(res.getString(R.string.group_many_members), groupMembers.get(0).getName(), groupMembers.get(1).getName());
                } else if (numberOfMembers == 1) {
                    groupMemberText = String.format(res.getString(R.string.group_one_member), groupMembers.get(0).getName());
                } else if (numberOfMembers == 2) {
                    groupMemberText = String.format(res.getString(R.string.group_two_member), groupMembers.get(0).getName(), groupMembers.get(1).getName());
                }
            }
            return groupMemberText;
        }
    }
}
