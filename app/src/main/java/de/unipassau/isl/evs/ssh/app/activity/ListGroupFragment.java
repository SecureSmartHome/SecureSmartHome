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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
    private ListView groupList;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_listgroup, container, false);
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        buildView();
    }

    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        groupList = (ListView) getActivity().findViewById(R.id.listGroupContainer);
        groupList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                             @Override
                                             public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                 Group item = adapter.getItem(position);
                                                 Bundle bundle = new Bundle();
                                                 bundle.putSerializable(AppConstants.Fragment_Arguments.GROUP_ARGUMENT_FRAGMENT, item);
                                                 ((MainActivity) getActivity()).showFragmentByClass(ListUserDeviceFragment.class, bundle);
                                             }
                                         }
        );
        groupList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Group item = adapter.getItem(position);
                Bundle bundle = new Bundle();
                bundle.putSerializable(EDIT_GROUP_DIALOG, item);
                String[] templates = buildTemplatesFromGroups();
                if (templates != null) {
                    bundle.putStringArray(TEMPLATE_DIALOG, templates);
                }
                ((MainActivity) getActivity()).showFragmentByClass(EditGroupFragment.class, bundle);
                return true;
            }
        });
        FloatingActionButton fab = ((FloatingActionButton) getActivity().findViewById(R.id.addgroup_fab));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                String[] templates = buildTemplatesFromGroups();
                if (templates != null) {
                    bundle.putStringArray(TEMPLATE_DIALOG, buildTemplatesFromGroups());
                }
                ((MainActivity) getActivity()).showFragmentByClass(AddGroupFragment.class, bundle);
            }
        });
        adapter = new GroupListAdapter();
        groupList.setAdapter(adapter);
    }

    /**
     * @return A String Array of Template names generated from all groups.
     */
    private String[] buildTemplatesFromGroups() {
        String[] templateArray = new String[0];
        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
        } else {
            List<Group> groups = handler.getAllGroups();
            List<String> templateNames = Lists.newArrayList(Iterables.transform(groups, new Function<Group, String>() {
                @Override
                public String apply(Group input) {
                    return input.getTemplateName();
                }
            }));
            templateArray = new String[templateNames.size()];
            int counter = 0;
            for (String s : templateNames) {
                templateArray[counter] = s;
                counter++;
            }
        }
        return templateArray;
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
            List<Group> allGroups = handler.getAllGroups();
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
                List<UserDevice> groupMembers = handler.getAllGroupMembers(group);
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
