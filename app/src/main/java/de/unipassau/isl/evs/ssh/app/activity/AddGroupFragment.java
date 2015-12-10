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
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;

import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.TEMPLATE_DIALOG;

/**
 * This fragment gives the user the option to choose a name and a template to create a new group.
 *
 * @author Phil Werli
 */
public class AddGroupFragment extends BoundFragment {
    private static final String TAG = AddNewUserDeviceFragment.class.getSimpleName();

    private Spinner spinner;
    private EditText inputGroupName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String[] templateNames = getArguments().getStringArray(TEMPLATE_DIALOG);
        View view = inflater.inflate(R.layout.fragment_addnewgroup, container, false);

        inputGroupName = (EditText) view.findViewById(R.id.addgroupfragment_group_name);

        spinner = (Spinner) view.findViewById(R.id.addgroupfragment_spinner);
        spinner.setAdapter(new ArrayAdapter<>(getActivity().getApplicationContext(),
                android.R.layout.simple_list_item_1,
                (templateNames != null ? templateNames : new String[]{"Missing templates"})));

        Button button = (Button) view.findViewById(R.id.addgroupfragment_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInputFields() && isContainerConnected()) {
                    String name = inputGroupName.getText().toString();
                    String template = ((String) spinner.getSelectedItem());
                    getComponent(AppUserConfigurationHandler.KEY).addGroup(new Group(name, template));
                    Log.i(TAG, "Group " + name + " added.");
                    String toastText = "Group " + name + " created.";
                    Toast toast = Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_cannot_add_group));
                }
            }
        });
        return view;
    }


    // returns true if all input fields are filled in correctly
    private boolean checkInputFields() {
        return spinner.isEnabled() && !inputGroupName.getText().toString().equals("");
    }
}