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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.app.handler.UserConfigurationEvent;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

/**
 * This fragment gives the user the option to choose a name and a template to create a new group.
 *
 * @author Phil Werli
 */
public class AddGroupFragment extends BoundFragment {
    private static final String TAG = AddNewUserDeviceFragment.class.getSimpleName();
    private final AppUserConfigurationHandler.UserInfoListener userInfoListener = new AppUserConfigurationHandler.UserInfoListener() {
        @Override
        public void userInfoUpdated(final UserConfigurationEvent event) {
            maybeRunOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (event.getType().equals(UserConfigurationEvent.EventType.GROUP_ADD)) {
                        String toastText = getResources().getString(R.string.group_created);
                        Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
                    }
                }
            });
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

        inputGroupName = (EditText) getActivity().findViewById(R.id.addgroupfragment_group_name);

        spinner = (Spinner) getActivity().findViewById(R.id.addgroupfragment_spinner);
        final ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, sortedTemplates);
        spinner.setAdapter(adapter);

        final Button button = (Button) getActivity().findViewById(R.id.addgroupfragment_button);
        button.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {
                                          if (checkInputFields() && isContainerConnected()) {
                                              if (((MainActivity) getActivity()).hasPermission(Permission.ADD_GROUP)) {
                                                  final String name = inputGroupName.getText().toString();
                                                  final String template = ((String) spinner.getSelectedItem());
                                                  handler.addGroup(new Group(name, template));
                                                  Log.i(TAG, "Group " + name + " added.");
                                              } else {
                                                  Toast.makeText(getActivity(), R.string.you_can_not_add_groups, Toast.LENGTH_SHORT).show();
                                              }
                                          } else {
                                              ErrorDialog.show(getActivity(), getActivity().getResources().getString(R.string.error_cannot_add_group));
                                          }
                                      }
                                  }
        );
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