package de.unipassau.isl.evs.ssh.app.activity;

import android.content.res.Resources;
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

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;

import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.EDIT_GROUP_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.TEMPLATE_DIALOG;

/**
 * This fragment gives the user the option to choose a name and a template used to edit an existing group.
 *
 * @author Phil Werli
 */
public class EditGroupFragment extends BoundFragment {

    //TODO Wolfgang/Phil: add Listener (Wolfgang, 2016-01-13)
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
        String[] templateNames = getArguments().getStringArray(TEMPLATE_DIALOG);

        final EditText inputGroupName = (EditText) getActivity().findViewById(R.id.editgroupfragment_group_name);

        final Spinner spinner = (Spinner) getActivity().findViewById(R.id.editgroupfragment_spinner);
        spinner.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1,
                (templateNames != null ? templateNames :
                        new String[]{getResources().getString(R.string.missingTemplates)})));

        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
            return;
        }
        if (group == null) {
            Log.i(TAG, "Can't build View. Missing group.");
            return;
        }
        final Resources res = getResources();
        Button editButton = (Button) getActivity().findViewById(R.id.editgroupfragment_button_edit);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = inputGroupName.getText().toString();
                String template = ((String) spinner.getSelectedItem());
                handler.setGroupName(group, name);
                handler.setGroupTemplate(group, template);
                Log.i(TAG, "Group " + name + " edited.");
                String toastText = String.format(res.getString(R.string.group_edited), name);
                Toast toast = Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        Button removeButton = ((Button) getActivity().findViewById(R.id.editgroupfragment_button_remove));
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeGroup(group);
                Log.i(TAG, "Group " + group.getName() + " removed.");
                String toastText = String.format(res.getString(R.string.group_removed), group.getName());
                Toast toast = Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }
}