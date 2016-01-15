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
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppRegisterNewDeviceHandler;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.database.dto.UserDevice;
import de.unipassau.isl.evs.ssh.core.sec.DeviceConnectInformation;

/**
 * This fragment is used to start the process of registering a new user. The name and the group of the new user can be
 * set in this fragment.
 *
 * @author Wolfgang Popp
 */
public class AddNewUserDeviceFragment extends BoundFragment {
    private static final String TAG = AddNewUserDeviceFragment.class.getSimpleName();
    private final AppRegisterNewDeviceHandler.RegisterNewDeviceListener registerNewDeviceListener = new AppRegisterNewDeviceHandler.RegisterNewDeviceListener() {
        @Override
        public void tokenResponse(final DeviceConnectInformation deviceConnectInformation) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(CoreConstants.QRCodeInformation.EXTRA_QR_DEVICE_INFORMATION, deviceConnectInformation);
                    ((AppMainActivity) getActivity()).showFragmentByClass(QRCodeFragment.class, bundle);
                }
            });
        }

        @Override
        public void tokenError() {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), R.string.add_new_user_fail, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    private List<String> groups;
    private Spinner spinner;
    private final AppUserConfigurationHandler.UserInfoListener userConfigListener = new AppUserConfigurationHandler.UserInfoListener() {
        @Override
        public void userInfoUpdated(UserConfigurationEvent event) {
            if (event.getType().equals(UserConfigurationEvent.EventType.PUSH)) {
                updateGroupSpinner();
            }
        }
    };
    private EditText inputUserName;
    private Button button;

    private void updateGroupSpinner() {
        AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler == null) {
            Log.e(TAG, "Container not connected!");
            return;
        }

        Set<Group> allGroups = handler.getAllGroups();
        if (allGroups.size() < 1) {
            Log.i(TAG, "No groups available, yet.");
            return;
        }

        this.groups = Lists.newArrayList(Iterables.transform(allGroups, new Function<Group, String>() {
            @Override
            public String apply(Group input) {
                return input.getName();
            }
        }));
        SpinnerAdapter adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, groups);
        spinner.setAdapter(adapter);
        spinner.setEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_addnewuserdevice, container, false);
        spinner = (Spinner) view.findViewById(R.id.groupSpinner);
        spinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                new String[]{getResources().getString(R.string.querying_groups)}));
        spinner.setEnabled(false);

        inputUserName = (EditText) view.findViewById(R.id.addNewDeviceUserName);

        button = (Button) view.findViewById(R.id.add_user_button);
        return view;
    }

    private void addButtonOnClickListener(final Button button, final Container container) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInputFields() && isContainerConnected()) {
                    String name = inputUserName.getText().toString();
                    String group = ((String) spinner.getSelectedItem());
                    UserDevice user = new UserDevice(name, group, null);
                    container.require(AppRegisterNewDeviceHandler.KEY).requestToken(user);
                } else {
                    ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_cannot_add_user));
                }
            }
        });
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
        addButtonOnClickListener(button, container);
        updateGroupSpinner();
    }

    @Override
    public void onContainerDisconnected() {
        groups = null;
        AppUserConfigurationHandler configHandler = getComponent(AppUserConfigurationHandler.KEY);
        if (configHandler != null) {
            configHandler.removeUserInfoListener(userConfigListener);
        }

        AppRegisterNewDeviceHandler registerHandler = getComponent(AppRegisterNewDeviceHandler.KEY);
        if (registerHandler != null) {
            registerHandler.removeRegisterDeviceListener(registerNewDeviceListener);
        }
        super.onContainerDisconnected();
    }
}