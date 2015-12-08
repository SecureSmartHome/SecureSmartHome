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
import android.widget.SpinnerAdapter;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppRegisterNewDeviceHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.sec.QRDeviceInformation;

/**
 * This activity allows to enter information describing new user devices and provide a QR-Code
 * which a given user device has to scan. If this functionality is used a message,
 * containing all needed information, is generated and passed to the OutgoingRouter.
 */
public class AddNewUserDeviceFragment extends BoundFragment {
    private static final String TAG = AddNewUserDeviceFragment.class.getSimpleName();
    private List<String> groups;
    private Spinner spinner;
    private EditText inputUserName;

    private final AppUserConfigurationHandler.UserInfoListener userConfigListener = new AppUserConfigurationHandler.UserInfoListener() {
        @Override
        public void userInfoUpdated() {
            updateGroupSpinner();
        }
    };

    private final AppRegisterNewDeviceHandler.RegisterNewDeviceListener registerNewDeviceListener = new AppRegisterNewDeviceHandler.RegisterNewDeviceListener() {
        @Override
        public void tokenResponse(QRDeviceInformation qrDeviceInformation) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION, qrDeviceInformation);
            ((MainActivity) getActivity()).showFragmentByClass(QRCodeFragment.class, bundle);
        }
    };

    private void updateGroupSpinner() {
        List<Group> allGroups = getComponent(AppUserConfigurationHandler.KEY).getAllGroups();
        if (allGroups == null) {
            Log.i(TAG, "No groups available, yet.");
            return;
        }
        this.groups = Lists.newArrayList(Iterables.transform(allGroups, new Function<Group, String>() {
            @Override
            public String apply(Group input) {
                return input.getName();
            }
        }));
        SpinnerAdapter adapter = new ArrayAdapter<>(getActivity().getApplicationContext(),
                android.R.layout.simple_list_item_1, groups);
        spinner.setAdapter(adapter);
        spinner.setEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_addnewuserdevice, container, false);
        spinner = (Spinner) view.findViewById(R.id.groupSpinner);
        spinner.setAdapter(new ArrayAdapter<>(getActivity().getApplicationContext(),
                android.R.layout.simple_list_item_1, new String[]{"Querying groups"}));
        spinner.setEnabled(false);

        inputUserName = (EditText) view.findViewById(R.id.addNewDeviceUserName);

        Button button = (Button) view.findViewById(R.id.add_user_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInputFields() && isContainerConnected()) {
                    String name = inputUserName.getText().toString();
                    String group = ((Group) spinner.getSelectedItem()).getName();
                    UserDevice user = new UserDevice(name, group, null);
                    getComponent(AppRegisterNewDeviceHandler.KEY).requestToken(user);
                } else {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_cannot_add_user));
                }
            }
        });
        return view;
    }


    // returns true if all input fields are filled in correctly
    private boolean checkInputFields() {
        return spinner.isEnabled() && !inputUserName.getText().toString().equals("");
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);

        AppUserConfigurationHandler configHandler = container.require(AppUserConfigurationHandler.KEY);
        configHandler.addUserInfoListener(userConfigListener);

        AppRegisterNewDeviceHandler registerHandler = container.require(AppRegisterNewDeviceHandler.KEY);
        registerHandler.addRegisterDeviceListener(registerNewDeviceListener);
        updateGroupSpinner();
    }

    @Override
    public void onContainerDisconnected() {
        groups = null;
        AppUserConfigurationHandler configHandler = getComponent(AppUserConfigurationHandler.KEY);
        assert configHandler != null;
        configHandler.removeUserInfoListener(userConfigListener);

        AppRegisterNewDeviceHandler registerHandler = getComponent(AppRegisterNewDeviceHandler.KEY);
        assert registerHandler != null;
        registerHandler.addRegisterDeviceListener(registerNewDeviceListener);
        super.onContainerDisconnected();
    }
}