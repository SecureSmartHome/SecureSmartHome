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

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;

import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.EDIT_GROUP_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.TEMPLATE_DIALOG;

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

    private void buildView() {
        final Group group = (Group) getArguments().getSerializable(EDIT_GROUP_DIALOG);
        String[] templateNames = getArguments().getStringArray(TEMPLATE_DIALOG);

        final EditText inputGroupName = (EditText) getActivity().findViewById(R.id.editgroupfragment_group_name);

        final Spinner spinner = (Spinner) getActivity().findViewById(R.id.editgroupfragment_spinner);
        spinner.setAdapter(new ArrayAdapter<>(getActivity().getApplicationContext(),
                android.R.layout.simple_list_item_1,
                (templateNames != null ? templateNames : new String[]{"Missing templates"})));

        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
            return;
        }
        Button editButton = (Button) getActivity().findViewById(R.id.editgroupfragment_button_edit);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = inputGroupName.getText().toString();
                String template = ((String) spinner.getSelectedItem());
                handler.setGroupName(group, name);
                handler.setGroupTemplate(group, template);
                Log.i(TAG, "Group " + name + " edited.");
                String toastText = "Group " + name + " edited.";
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
                String toastText = "Group " + group.getName() + " removed.";
                Toast toast = Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        buildView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Phil Save state of spinner. but I can't restore it in onCreateView as it is called after onContainerConnected
        super.onSaveInstanceState(outState);
    }
}