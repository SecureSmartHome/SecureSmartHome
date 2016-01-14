package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;

import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.EDIT_GROUP_DIALOG;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_GROUP_NAME;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.CHANGE_GROUP_TEMPLATE;
import static de.unipassau.isl.evs.ssh.core.sec.Permission.DELETE_MODULE;

/**
 * This fragment gives the user the option to choose a name and a template used to edit an existing group.
 *
 * @author Phil Werli
 */
public class EditGroupFragment extends BoundFragment {
    private static final String TAG = EditGroupFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_editnewgroup, container, false);
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
        final Group group = (Group) getArguments().getSerializable(EDIT_GROUP_DIALOG);
        if (group == null) {
            Log.i(TAG, "Can't build View. Missing group.");
            return;
        }
        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
            return;
        }
        final Set<String> unsortedTemplates = handler.getAllTemplates();
        List<String> sortedTemplates = new LinkedList<>(unsortedTemplates);
        Collections.sort(sortedTemplates, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                if (lhs == null) {
                    return rhs == null ? 0 : 1;
                }
                if (rhs == null) {
                    return -1;
                }
                return lhs.compareTo(rhs);
            }
        });

        final EditText inputGroupName = (EditText) getActivity().findViewById(R.id.editgroupfragment_group_name);
        inputGroupName.setText(group.getName());

        final Spinner spinner = (Spinner) getActivity().findViewById(R.id.editgroupfragment_spinner);
        final ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, sortedTemplates);
        spinner.setAdapter(adapter);
        spinner.setSelection(adapter.getPosition(group.getTemplateName()));


        final Button editButton = (Button) getActivity().findViewById(R.id.editgroupfragment_button_edit);
        final MainActivity activity = (MainActivity) getActivity();
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.hasPermission(CHANGE_GROUP_NAME) && activity.hasPermission(CHANGE_GROUP_TEMPLATE)) {
                    String name = inputGroupName.getText().toString();
                    String template = ((String) spinner.getSelectedItem());
                    handler.setGroupName(group, name);
                    handler.setGroupTemplate(group, template);
                    Log.i(TAG, "Group " + name + " edited.");
                    activity.showFragmentByClass(ListGroupFragment.class);
                    // TODO Phil: better handling (Phil, 2016-01-13)
                } else {
                    Toast.makeText(getActivity(), R.string.you_can_not_edit_groups, Toast.LENGTH_SHORT).show();
                }
            }
        });

        final Button removeButton = ((Button) getActivity().findViewById(R.id.editgroupfragment_button_remove));
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.hasPermission(DELETE_MODULE)) {
                    if (!handler.getAllGroupMembers(group).isEmpty()) {
                        handler.removeGroup(group);
                        Log.i(TAG, "Group " + group.getName() + " removed.");
                        ((MainActivity) getActivity()).showFragmentByClass(ListGroupFragment.class);
                        // TODO Phil: better handling (Phil, 2016-01-13)
                    } else {
                        Toast.makeText(getActivity(), R.string.you_can_not_remove_not_empty_group, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.you_can_not_remove_groups, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}