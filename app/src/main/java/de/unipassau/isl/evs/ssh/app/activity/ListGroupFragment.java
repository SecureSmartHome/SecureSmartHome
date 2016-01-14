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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.EDIT_GROUP_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.FragmentArguments.GROUP_ARGUMENT_FRAGMENT;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.GROUP_DELETE;
import static de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent.EventType.GROUP_SET_NAME;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.ADD_GROUP;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_GROUP_NAME;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_GROUP_TEMPLATE;

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
                    if (event.getType().equals(UserConfigurationEvent.EventType.GROUP_ADD)) {
                        if (event.wasSuccessful()) {
                            Toast.makeText(getActivity(), R.string.group_created, Toast.LENGTH_SHORT).show();
                            update();
                        } else {
                            Toast.makeText(getActivity(), R.string.could_not_edit_group, Toast.LENGTH_SHORT).show();
                        }
                    } else if (event.getType().equals(GROUP_SET_NAME)) {
                        if (event.wasSuccessful()) {
                            Toast.makeText(getActivity(), R.string.edit_group_success, Toast.LENGTH_SHORT).show();
                            update();
                        } else {
                            Toast.makeText(getActivity(), R.string.could_not_edit_group, Toast.LENGTH_SHORT).show();
                        }
                    } else if (event.getType().equals(GROUP_DELETE)) {
                        if (event.wasSuccessful()) {
                            Toast.makeText(getActivity(), R.string.delete_group_success, Toast.LENGTH_SHORT).show();
                            update();
                        } else {
                            Toast.makeText(getActivity(), R.string.could_not_delete_group, Toast.LENGTH_SHORT).show();
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
        final MainActivity activity = (MainActivity) getActivity();
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
                if (activity.hasPermission(CHANGE_GROUP_NAME) && activity.hasPermission(CHANGE_GROUP_TEMPLATE)) {
                    Group item = adapter.getItem(position);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(EDIT_GROUP_DIALOG, item);
                    activity.showFragmentByClass(EditGroupFragment.class, bundle);
                    return true;
                } else {
                    Toast.makeText(activity, R.string.you_can_not_edit_groups, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        FloatingActionButton fab = ((FloatingActionButton) activity.findViewById(R.id.addgroup_fab));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.hasPermission(ADD_GROUP)) {
                    activity.showFragmentByClass(AddGroupFragment.class);
                } else {
                    Toast.makeText(activity, R.string.you_can_not_add_groups, Toast.LENGTH_SHORT).show();
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
        private List<Group> groups;

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
            Set<Group> allGroups = handler.getAllGroups();
            groups = Lists.newArrayList(allGroups);
            Collections.sort(groups, new Comparator<Group>() {
                @Override
                public int compare(Group lhs, Group rhs) {
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

        @Override
        public int getCount() {
            if (groups != null) {
                return groups.size();
            } else {
                return 0;
            }
        }

        @Override
        public Group getItem(int position) {
            if (groups != null) {
                return groups.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            final Group item = getItem(position);
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
            // the Group the list item is created for
            final Group group = getItem(position);

            LinearLayout groupLayout;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            if (convertView == null) {
                groupLayout = (LinearLayout) inflater.inflate(R.layout.grouplayout, parent, false);
            } else {
                groupLayout = (LinearLayout) convertView;
            }

            TextView textViewGroupName = (TextView) groupLayout.findViewById(R.id.listgroup_group_name);
            textViewGroupName.setText(group.getName());

            TextView textViewGroupMembers = (TextView) groupLayout.findViewById(R.id.listgroup_group_members);
            textViewGroupMembers.setText(createLocalizedGroupMemberText(group));

            return groupLayout;
        }

        /**
         * Creates a text describes a group in one sentence.
         *
         * @param group The group the text is created for.
         * @return the text to display
         */
        private String createLocalizedGroupMemberText(Group group) {
            Resources res = getResources();
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
