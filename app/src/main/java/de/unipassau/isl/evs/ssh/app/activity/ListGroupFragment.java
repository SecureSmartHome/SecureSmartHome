package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.AppConstants;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.AddGroupDialog;
import de.unipassau.isl.evs.ssh.app.dialogs.EditGroupDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.ADD_GROUP_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.EDIT_GROUP_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.TEMPLATE_DIALOG;

/**
 * This fragment shows a list of all groups of user devices registered in the system.
 * It gets its information through the {@link AppUserConfigurationHandler} which sends and receives necessary messages.
 *
 * @author Phil Werli
 * @see ListUserDeviceFragment
 * @see EditUserDeviceFragment
 */
public class ListGroupFragment extends BoundFragment implements EditGroupDialog.EditGroupDialogListener, AddGroupDialog.AddGroupDialogListener {
    private GroupListAdapter adapter;

    private AddGroupDialog.AddGroupDialogListener addGroupDialogListener;
    private EditGroupDialog.EditGroupDialogListener editGroupDialogListener;

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
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.listgroupfragment, container, false);
        ListView list = (ListView) root.findViewById(R.id.listGroupContainer);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        // when a user clicks short on an item, he opens the ListUserDeviceFragment
                                        @Override
                                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            Group item = adapter.getItem(position);
                                            Bundle bundle = new Bundle();
                                            bundle.putSerializable(AppConstants.Fragment_Arguments.ARGUMENT_FRAGMENT, item);
                                            ((MainActivity) getActivity()).showFragmentByClass(ListUserDeviceFragment.class, bundle);
                                        }
                                    }
        );
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Group item = adapter.getItem(position);
                //TODO edit group dialog
                EditGroupDialog editGroupDialog = new EditGroupDialog();
                Bundle bundle = new Bundle();
                bundle.putSerializable(EDIT_GROUP_DIALOG, item);
                String[] templates = buildTemplatesFromGroups();
                if (templates != null) {
                    bundle.putStringArray(TEMPLATE_DIALOG, buildTemplatesFromGroups());
                }
                editGroupDialog.setArguments(bundle);
                editGroupDialog.show(getFragmentManager(), EDIT_GROUP_DIALOG);
                return item == null;
            }
        });
        FloatingActionButton fab = ((FloatingActionButton) root.findViewById(R.id.addgroup_fab));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO add new group dialog
                AddGroupDialog addGroupDialog = new AddGroupDialog();
                Bundle bundle = new Bundle();
                String[] templates = buildTemplatesFromGroups();
                if (templates != null) {
                    bundle.putStringArray(TEMPLATE_DIALOG, buildTemplatesFromGroups());
                }
                addGroupDialog.setArguments(bundle);
                addGroupDialog.show(getFragmentManager(), ADD_GROUP_DIALOG);

            }
        });
        adapter = new GroupListAdapter(inflater);
        list.setAdapter(adapter);

        return root;
    }

    private String[] buildTemplatesFromGroups() {
        List<Group> groups = getComponent(AppUserConfigurationHandler.KEY).getAllGroups();
        if (groups == null) {
            return null;
        }
        List<String> templateNames = Lists.newArrayList(Iterables.transform(groups, new Function<Group, String>() {
            @Override
            public String apply(Group input) {
                return input.getTemplateName();
            }
        }));
        return (String[]) ImmutableSet.copyOf(templateNames).toArray();
    }

    @Override
    public void onDialogPositiveClick(AddGroupDialog dialog) {
        EditText editText = (EditText) dialog.getView().findViewById(R.id.add_group_dialog_group_name);
        String groupName = editText.getText().toString();
        Spinner spinner = (Spinner) dialog.getView().findViewById(R.id.add_group_dialog_spinner);
        String templateName = spinner.getSelectedItem().toString();

        Group newGroup = new Group(groupName, templateName);

        getComponent(AppUserConfigurationHandler.KEY).addGroup(newGroup);
    }

    @Override
    public void onDialogPositiveClick(EditGroupDialog dialog) {
        EditText editText = (EditText) dialog.getView().findViewById(R.id.edit_group_dialog_group_name);
        String newGroupName = editText.getText().toString();
        Spinner spinner = (Spinner) dialog.getView().findViewById(R.id.edit_group_dialog_spinner);
        String newTemplateName = (String) spinner.getSelectedItem();
        String oldGroupName = editText.getHint().toString();
        String oldTemplateName = editText.getText().toString();

        Group newGroup = new Group(newGroupName, newTemplateName);
        Group oldGroup = new Group(oldGroupName, oldTemplateName);

        if (!(newGroupName.equals(oldGroupName) && newTemplateName.equals(oldTemplateName))) {
            getComponent(AppUserConfigurationHandler.KEY).editGroup(newGroup, oldGroup);
        }
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
            groups = getComponent(AppUserConfigurationHandler.KEY).getAllGroups();
            groups = new ArrayList<>();
            Group group1 = new Group("Admin", "Template 1");
            Group group2 = new Group("Eltern", "Template 2");
            Group group3 = new Group("Kinder", "Template 3");
            groups.add(group1);
            groups.add(group2);
            groups.add(group3);
            Group group4 = new Group("Gäste", "Template 3");
            Group group5 = new Group("Gästeeltern", "Template 3");
            Group group6 = new Group("Gästekinder", "Template 3");
            groups.add(group4);
            groups.add(group5);
            groups.add(group6);

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

        /**
         * Creates a text describes a group in one sentence.
         *
         * @param group The group the text is created for.
         * @return the text to display
         */
        private String createGroupMemberText(Group group) {
            String groupMemberText = "This group has no members.";
            List<UserDevice> groupMembers = getComponent(AppUserConfigurationHandler.KEY).getAllGroupMembers(group);
            if (groupMembers != null) {
                int numberOfMembers = groupMembers.size();
                if (numberOfMembers >= 3) {
                    groupMemberText = groupMembers.get(0).getName() + " and " + groupMembers.get(1).getName() + " and more are members";
                } else if (numberOfMembers == 1) {
                    groupMemberText = groupMembers.get(0).getName() + " is the only member.";
                } else if (numberOfMembers == 2) {
                    groupMemberText = groupMembers.get(0).getName() + " and " + groupMembers.get(1).getName() + " are members.";
                }
            }
            return groupMemberText;
        }
    }
}
