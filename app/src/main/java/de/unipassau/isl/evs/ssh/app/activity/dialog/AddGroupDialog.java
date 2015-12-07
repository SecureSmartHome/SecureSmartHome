package de.unipassau.isl.evs.ssh.app.activity.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import de.unipassau.isl.evs.ssh.app.R;

import static de.unipassau.isl.evs.ssh.app.AppConstants.Dialog_Arguments.TEMPLATE_DIALOG;

/**
 * Dialog class to create a dialog that lets the user add a new group to the system.
 *
 * @author Phil Werli
 */
public class AddGroupDialog extends DialogFragment {
    AddGroupDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        String[] templateNames = getArguments().getStringArray(TEMPLATE_DIALOG);
        View dialogView = inflater.inflate(R.layout.addgroupdialog, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.add_new_group_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        listener.onDialogPositiveClick(AddGroupDialog.this);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        AddGroupDialog.this.getDialog().cancel();
                    }
                });
        EditText groupName = (EditText) dialogView.findViewById(R.id.add_group_dialog_group_name);
        groupName.clearFocus();
//        ArrayList<String> templateList = new ArrayList<>(Arrays.asList(templateNames));
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), R.layout.template_list, templateList);
//        Spinner templateName = ((Spinner) dialogView.findViewById(R.id.edit_group_dialog_spinner));
//        templateName.setAdapter(adapter);
        return builder.create();
    }

    public interface AddGroupDialogListener {
        void onDialogPositiveClick(AddGroupDialog dialog);
    }
}