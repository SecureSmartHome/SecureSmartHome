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

import com.google.common.base.Strings;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;

import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.TEMPLATE_DIALOG;

/**
 * This fragment gives the user the option to choose a name and a template to create a new group.
 *
 * @author Phil Werli
 */
public class AddGroupFragment extends BoundFragment {
    private static final String TAG = AddNewUserDeviceFragment.class.getSimpleName();
    private final AppUserConfigurationHandler.UserInfoListener userInfoListener = new AppUserConfigurationHandler.UserInfoListener() {
        @Override
        public void userInfoUpdated() {
            String toastText = getResources().getString(R.string.group_created);
            Toast toast = Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT);
            toast.show();
        }
    };
    private Spinner spinner;
    private EditText inputGroupName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_addnewgroup, container, false);
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        container.require(AppUserConfigurationHandler.KEY).addUserInfoListener(userInfoListener);
        buildView();
    }

    /**
     * Gets called in {@link #onContainerConnected(Container)}.
     * Builds the view components that require the container.
     */
    private void buildView() {
        final String[] templateNames = getArguments().getStringArray(TEMPLATE_DIALOG);

        inputGroupName = (EditText) getActivity().findViewById(R.id.addgroupfragment_group_name);

        spinner = (Spinner) getActivity().findViewById(R.id.addgroupfragment_spinner);
        spinner.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1,
                (templateNames != null ? templateNames :
                        new String[]{getResources().getString(R.string.missingTemplates)})));

        Button button = (Button) getActivity().findViewById(R.id.addgroupfragment_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInputFields() && isContainerConnected()) {
                    String name = inputGroupName.getText().toString();
                    String template = ((String) spinner.getSelectedItem());
                    final AppUserConfigurationHandler component = getComponent(AppUserConfigurationHandler.KEY);
                    if (component == null) {
                        Log.i(TAG, "Can't add group, Container not yet connected!");
                    } else {
                        component.addGroup(new Group(name, template));
                        Log.i(TAG, "Group " + name + " added.");
                    }
                } else {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_cannot_add_group));
                }
            }
        });
    }

    @Override
    public void onContainerDisconnected() {
        final AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler == null) {
            Log.i(TAG, "Container not yet connected!");
        } else {
            handler.removeUserInfoListener(userInfoListener);
        }
        super.onContainerDisconnected();
    }

    /**
     * @return {@code true} if all input fields are filled in correctly
     */
    private boolean checkInputFields() {
        return spinner.isEnabled() && !(Strings.isNullOrEmpty(String.valueOf(inputGroupName.getText())));
    }
}