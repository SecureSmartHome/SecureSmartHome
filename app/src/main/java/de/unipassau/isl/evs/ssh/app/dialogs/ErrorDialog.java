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

package de.unipassau.isl.evs.ssh.app.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import de.unipassau.isl.evs.ssh.app.R;

/**
 * The ErrorDialog Fragment is a popup dialog window that displays an error message.
 *
 * @author Wolfgang Popp
 */
public class ErrorDialog extends DialogFragment {
    private static final String ARG_MESSAGE = "de.unipassau.isl.evs.ssh.app.dialog.MESSAGE";
    private String message;

    /**
     * Creates and shows a new error dialog with the given message.
     *
     * @param activity the parent activity of the error dialog
     * @param message  the error message
     */
    public static void show(Activity activity, String message) {
        DialogFragment dialog = new ErrorDialog();
        Bundle args = new Bundle(1);
        args.putString(ErrorDialog.ARG_MESSAGE, message);
        dialog.setArguments(args);
        dialog.show(activity.getFragmentManager(), "error");
    }

    @Override
    public void setArguments(Bundle args) {
        message = args.getString(ARG_MESSAGE);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.error));
        builder.setNeutralButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        String savedMessage = null;
        if (savedInstanceState != null) {
            savedMessage = savedInstanceState.getString(ARG_MESSAGE);
        }
        if (savedMessage != null) {
            message = savedMessage;
        }
        builder.setMessage(message);
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_MESSAGE, message);
    }
}
