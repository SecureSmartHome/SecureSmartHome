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
import android.widget.ArrayAdapter;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.unipassau.isl.evs.ssh.app.AppConstants;
import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;

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
public class ListGroupFragment extends BoundFragment {
    private static final String TAG = ListGroupFragment.class.getSimpleName();

    private GroupListAdapter adapter;

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
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.fragment_listgroup, container, false);
        ListView list = (ListView) root.findViewById(R.id.listGroupContainer);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            Group item = adapter.getItem(position);
                                            Bundle bundle = new Bundle();
                                            bundle.putSerializable(AppConstants.Fragment_Arguments.GROUP_ARGUMENT_FRAGMENT, item);
                                            ((MainActivity) getActivity()).showFragmentByClass(ListUserDeviceFragment.class, bundle);
                                        }
                                    }
        );
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Group item = adapter.getItem(position);
                Bundle bundle = new Bundle();
                bundle.putSerializable(EDIT_GROUP_DIALOG, item);
                String[] templates = buildTemplatesFromGroups();
                if (templates != null) {
                    bundle.putStringArray(TEMPLATE_DIALOG, buildTemplatesFromGroups());
                }
                createEditGroupDialog(bundle).show();
                return item == null;
            }
        });
        FloatingActionButton fab = ((FloatingActionButton) root.findViewById(R.id.addgroup_fab));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                String[] templates = buildTemplatesFromGroups();
                if (templates != null) {
                    bundle.putStringArray(TEMPLATE_DIALOG, buildTemplatesFromGroups());
                }
                createAddGroupDialog(bundle).show();
            }
        });
        adapter = new GroupListAdapter(inflater);
        list.setAdapter(adapter);

        return root;
    }

    /**
     * Creates and returns a dialogs that gives the user the option to add a group.
     */
    private Dialog createAddGroupDialog(Bundle bundle) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        String[] templateNames = bundle.getStringArray(TEMPLATE_DIALOG);
        View dialogView = inflater.inflate(R.layout.dialog_addgroup, null, false);
        final EditText groupName = ((EditText) dialogView.findViewById(R.id.edit_group_dialog_group_name));
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        ArrayList<String> templateList = new ArrayList<>(Arrays.asList(templateNames));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.template_list, templateList);

        final Spinner templateName = ((Spinner) dialogView.findViewById(R.id.edit_group_dialog_spinner));
        templateName.setAdapter(adapter);

        final AlertDialog dialog = builder.create();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        builder.setMessage(R.string.add_new_group_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String name = groupName.getText().toString();
                        String template = ((String) templateName.getSelectedItem());
                        getComponent(AppUserConfigurationHandler.KEY).addGroup(new Group(name, template));
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        return dialog;
    }

    /**
     * Creates and returns a dialogs that gives the user the option to edit a group.
     */
    private Dialog createEditGroupDialog(Bundle bundle) {
        final Group group = (Group) bundle.getSerializable(EDIT_GROUP_DIALOG);
        String[] templateNames = bundle.getStringArray(TEMPLATE_DIALOG);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_editgroup, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText groupName = (EditText) dialogView.findViewById(R.id.edit_group_dialog_group_name);
        groupName.setHint(group.getName());
        ArrayList<String> templateList = new ArrayList<>(Arrays.asList(templateNames));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.template_list, templateList);
        final Spinner templateName = ((Spinner) dialogView.findViewById(R.id.edit_group_dialog_spinner));

        templateName.setAdapter(adapter);

        final AlertDialog dialog = builder.create();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        builder.setMessage(R.string.edit_group_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String name = groupName.getText().toString();
                        String template = ((String) templateName.getSelectedItem());
                        getComponent(AppUserConfigurationHandler.KEY).editGroup(group, new Group(name, template));
                    }
                });
        return dialog;
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
            AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);

            if (handler == null) {
                Log.i(TAG, "Container not yet connected!");
                return;
            }

            groups = handler.getAllGroups();
            // TODO delete bellow. only test
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

            TextView textViewGroupName = (TextView) groupLayout.findViewById(R.id.listgroup_group_name);
            textViewGroupName.setText(group.getName());

            TextView textViewGroupMembers = (TextView) groupLayout.findViewById(R.id.listgroup_group_members);
            textViewGroupMembers.setText(createGroupMemberText(group));

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
