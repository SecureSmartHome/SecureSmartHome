package de.unipassau.isl.evs.ssh.app.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;

/**
 * This activity allows to enter information describing new user devices and provide a QR-Code
 * which a given user device has to scan. If this functionality is used a message,
 * containing all needed information, is generated and passed to the OutgoingRouter.
 */
public class AddNewUserDeviceFragment extends BoundFragment {
    private static final String TAG = AddNewUserDeviceFragment.class.getSimpleName();
    private List<String> groups;
    private Spinner spinner;

    private final AppUserConfigurationHandler.UserInfoListener userConfigListener = new AppUserConfigurationHandler.UserInfoListener() {
        @Override
        public void userInfoUpdated() {
            updateGroupSpinner();
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

        // TODO get groups
        View view = inflater.inflate(R.layout.fragment_addnewuserdevice, container, false);
        spinner = (Spinner) view.findViewById(R.id.groupSpinner);
        spinner.setAdapter(new ArrayAdapter<>(getActivity().getApplicationContext(),
                android.R.layout.simple_list_item_1, new String[]{"Querying groups"}));
        spinner.setEnabled(false);
        return view;
    }

    @Override
    public void onContainerConnected(Container container) {
        super.onContainerConnected(container);
        AppUserConfigurationHandler handler = container.require(AppUserConfigurationHandler.KEY);
        handler.addUserInfoListener(userConfigListener);
        handler.update();
        updateGroupSpinner();
    }

    @Override
    public void onContainerDisconnected() {
        groups = null;
        AppUserConfigurationHandler handler = getComponent(AppUserConfigurationHandler.KEY);
        if (handler != null) {
            handler.removeUserInfoListener(userConfigListener);
        }
        super.onContainerDisconnected();
    }
}