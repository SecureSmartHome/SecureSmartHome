package de.unipassau.isl.evs.ssh.app.activity.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;

import de.unipassau.isl.evs.ssh.app.R;
import de.unipassau.isl.evs.ssh.core.database.dto.Group;

import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.EDIT_GROUP_DIALOG;
import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.TEMPLATE_DIALOG;

/**
 * Dialog class to create a dialog that lets the user edit a group.
 *
 * @author Phil Werli
 */
public class EditGroupDialog extends DialogFragment {
    EditGroupDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = ((EditGroupDialogListener) activity);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EditGroupDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Group group = (Group) getArguments().getSerializable(EDIT_GROUP_DIALOG);
        String[] templateNames = getArguments().getStringArray(TEMPLATE_DIALOG);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.editgroupdialog, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.edit_group_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDialogPositiveClick(EditGroupDialog.this);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditGroupDialog.this.getDialog().cancel();
                    }
                });
        EditText groupName = (EditText) dialogView.findViewById(R.id.edit_group_dialog_group_name);
        groupName.setHint(group.getName());
        groupName.clearFocus();
        ArrayList<String> templateList = new ArrayList<>(Arrays.asList(templateNames));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.template_list, templateList);
        Spinner templateName = ((Spinner) dialogView.findViewById(R.id.edit_group_dialog_spinner));
        templateName.setAdapter(adapter);
        return builder.create();
    }

    public interface EditGroupDialogListener {
        void onDialogPositiveClick(EditGroupDialog dialog);
    }


}