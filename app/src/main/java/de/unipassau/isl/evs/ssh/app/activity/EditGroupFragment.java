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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.app.handler.AppUserConfigurationHandler;
import de.unipassau.isl.evs.ssh.core.container.Container;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;
import de.unipassau.isl.evs.ssh.core.sec.Permission;

import static de.unipassau.isl.evs.ssh.app.AppConstants.DialogArguments.EDIT_GROUP_DIALOG;

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
        final AppMainActivity activity = (AppMainActivity) getActivity();
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.hasPermission(Permission.CHANGE_GROUP_NAME) && activity.hasPermission(Permission.CHANGE_GROUP_TEMPLATE)) {
                    String name = inputGroupName.getText().toString();
                    String template = ((String) spinner.getSelectedItem());
                    handler.setGroupName(group, name);
                    handler.setGroupTemplate(group, template);
                    Log.i(TAG, "Group " + name + " edited.");
                    activity.showFragmentByClass(ListGroupFragment.class);
                } else {
                    showToast(R.string.you_can_not_edit_groups);
                }
            }
        });

        final Button removeButton = ((Button) getActivity().findViewById(R.id.editgroupfragment_button_remove));
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.hasPermission(Permission.DELETE_MODULE)) {
                    if (handler.getAllGroupMembers(group).isEmpty()) {
                        handler.removeGroup(group);
                        Log.i(TAG, "Group " + group.getName() + " removed.");
                        ((AppMainActivity) getActivity()).showFragmentByClass(ListGroupFragment.class);
                    } else {
                        showToast(R.string.you_can_not_remove_not_empty_group);
                    }
                } else {
                    showToast(R.string.you_can_not_remove_groups);
                }
            }
        });
    }
}