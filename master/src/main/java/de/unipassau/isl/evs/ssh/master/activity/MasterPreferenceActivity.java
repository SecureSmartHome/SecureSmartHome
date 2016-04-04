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

package de.unipassau.isl.evs.ssh.master.activity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import com.google.common.base.Strings;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.master.R;
import de.unipassau.isl.evs.ssh.master.network.Server;

/**
 * Preferences Activity for the Master Odroid
 *
 * @author Niko Fink
 */
public class MasterPreferenceActivity extends MasterStartUpActivity {
    public MasterPreferenceActivity() {
        super(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isSwitching()) {
            setContentView(R.layout.activity_master_preference);
            findViewById(R.id.textViewInfo).setVisibility(
                    getSharedPreferences().getBoolean(PREF_PREFERENCES_SET, false) ? View.GONE : View.VISIBLE
            );
            findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (allPreferencesSet()) {
                        getSharedPreferences().edit()
                                .putBoolean(PREF_PREFERENCES_SET, true)
                                .commit();
                        doBind();
                    } else {
                        Toast.makeText(MasterPreferenceActivity.this, R.string.please_set_prefs, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            });
        }
    }

    @Override
    protected void onStop() {
        doUnbind();
        super.onStop();
    }

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences(CoreConstants.FILE_SHARED_PREFS, MODE_PRIVATE);
    }

    @SuppressLint("CommitPrefEdits")
    private boolean allPreferencesSet() {
        final SharedPreferences prefs = getSharedPreferences();
        final SharedPreferences.Editor edit = prefs.edit();

        try {
            return validatePort(prefs, edit, getResources().getString(R.string.master_port_local), Server.PREF_SERVER_LOCAL_PORT) &&
                    validatePort(prefs, edit, getResources().getString(R.string.master_port_intern), Server.PREF_SERVER_PUBLIC_PORT) &&
                    validatePort(prefs, edit, getResources().getString(R.string.master_port_extern), null) &&
                    !Strings.isNullOrEmpty(prefs.getString(getResources().getString(R.string.master_city_name), null));
        } catch (ClassCastException e) {
            return false;
        } finally {
            edit.commit();
        }
    }

    private boolean validatePort(SharedPreferences prefs, SharedPreferences.Editor edit, String stringPref, @Nullable String intPref) {
        int value;
        try {
            value = prefs.getInt(stringPref, -1);
        } catch (ClassCastException e) {
            try {
                value = Integer.parseInt(prefs.getString(stringPref, "-1"));
            } catch (NumberFormatException | ClassCastException e1) {
                return false;
            }
        }
        if (intPref != null) {
            edit.putInt(intPref, value);
        }
        return value >= 0 && value <= 65535;
    }
}