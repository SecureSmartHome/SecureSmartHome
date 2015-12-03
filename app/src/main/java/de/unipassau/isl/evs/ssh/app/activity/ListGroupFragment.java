package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import de.unipassau.isl.evs.ssh.app.handler.AppUserDeviceHandler;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

/**
 * ListGroupFragment to show a list of all groups of user devices registered in the system.
 *
 * @author Phil Werli
 * @see ListUserDeviceFragment
 * @see EditUserDeviceFragment
 */
public class ListGroupFragment extends BoundFragment {
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
                                            Group item = adapter.getItem(position);
                                            //todo start ListUserDeviceFragment with group item
                                        }
                                    }
        );
        adapter = new GroupListAdapter(inflater);
        list.setAdapter(adapter);

        return root;
    }

    private class GroupListAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private List<Group> groups;

        public GroupListAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
            updateGroupList();
        }

        @Override
        public void notifyDataSetChanged() {
            updateGroupList();
            super.notifyDataSetChanged();
        }

        private void updateGroupList() {
            final List<Group> groups =
                    getComponent(AppUserDeviceHandler.KEY).getAllGroups();
            if (groups != null) {
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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // the Group the list item is created for
            final Group group = getItem(position);

            LinearLayout groupLayout;
            if (convertView == null) {
                groupLayout = (LinearLayout) inflater.inflate(R.layout.grouplayout, parent, false);
            } else {
                groupLayout = (LinearLayout) convertView;
            }

            TextView textView = (TextView) groupLayout.findViewById(R.id.listgroup_group_name);
            textView.setText(group.getName());

            TextView textViewGroupMembers = (TextView) groupLayout.findViewById(R.id.listgroup_group_members);
            String groupMembers = createGroupMemberText(group);
            textViewGroupMembers.setText(groupMembers);

            return groupLayout;
        }

        private String createGroupMemberText(Group group) {
            String groupMemberText = "";
            List<UserDevice> groupMembers = getComponent(AppUserDeviceHandler.KEY).getAllGroupMembers(group);
            if (groupMembers != null) {
                int numberOfMembers = groupMembers.size();
                if (numberOfMembers <= 0) {
                    groupMemberText = "This group has no members.";
                } else if (numberOfMembers == 1) {
                    groupMemberText = groupMembers.get(0).getName() + " is the only member.";
                } else if (numberOfMembers == 2) {
                    groupMemberText = groupMembers.get(0).getName() + " and " + groupMembers.get(1).getName() + " are members..";
                } else {
                    groupMemberText = groupMembers.get(0).getName() + " and " + groupMembers.get(1).getName() + " and more are members";
                }
            }
            return groupMemberText;
        }
    }
}
