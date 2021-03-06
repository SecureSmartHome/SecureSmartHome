/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

import com.google.common.base.Strings;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.dialogs.ErrorDialog;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
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
    private Spinner spinner;
    private EditText inputGroupName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_addnewgroup, container, false);
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

        spinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, sortedTemplates));

        final Button button = (Button) getActivity().findViewById(R.id.addgroupfragment_button);
        button.setOnClickListener(new View.OnClickListener() {
                                      final AppMainActivity activity = (AppMainActivity) getActivity();
                                      @Override
                                      public void onClick(View v) {
                                          if (checkInputFields() && isContainerConnected()) {
                                              if (activity.hasPermission(Permission.ADD_GROUP)) {
                                                  final String name = inputGroupName.getText().toString();
                                                  final String template = ((String) spinner.getSelectedItem());
                                                  handler.addGroup(new Group(name, template));
                                                  Log.i(TAG, "Group " + name + " added.");
                                                  activity.showFragmentByClass(ListGroupFragment.class);
                                              } else {
                                                  showToast(R.string.you_can_not_add_groups);
                                              }
                                          } else {
                                              ErrorDialog.show(activity, getActivity().getResources().getString(R.string.error_cannot_add_group));
                                          }
                                      }
                                  }
        );
    }

    /**
     * @return {@code true} if all input fields are filled in correctly
     */
    private boolean checkInputFields() {
        return spinner.isEnabled() && !(Strings.isNullOrEmpty(String.valueOf(inputGroupName.getText())));
    }
}