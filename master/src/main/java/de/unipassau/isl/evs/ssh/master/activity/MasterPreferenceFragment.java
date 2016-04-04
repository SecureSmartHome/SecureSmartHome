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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.google.common.base.Strings;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.master.R;

/**
 * PreferenceFragment containing preferences for the Master.
 *
 * @author Niko Fink
 * @author Christoph Frädrich
 */
public class MasterPreferenceFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(CoreConstants.FILE_SHARED_PREFS);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // show the current value in the settings screen
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            initSummary(getPreferenceScreen().getPreference(i));
        }
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceCategory) {
            PreferenceCategory cat = (PreferenceCategory) p;
            for (int i = 0; i < cat.getPreferenceCount(); i++) {
                initSummary(cat.getPreference(i));
            }
        } else {
            updatePreferences(p);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreferences(findPreference(key));
    }

    private void updatePreferences(Preference p) {
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (Strings.isNullOrEmpty(editTextPref.getText())) {
                resetSummary(editTextPref);
            } else {
                if (editTextPref.getKey().equals(getResources().getString(R.string.master_port_local))
                        || editTextPref.getKey().equals(getResources().getString(R.string.master_port_extern))
                        || editTextPref.getKey().equals(getResources().getString(R.string.master_port_intern))) {
                    //We can be sure this is a number due to the preference Activity only accepting numbers
                    int port = 0;
                    try {
                        port = Integer.valueOf(editTextPref.getText());
                    } catch (NumberFormatException e) {
                        //number is too big. error message will still appear because default value is out of range.
                    }

                    if (port < 1024 || port > 65535) {
                        Toast.makeText(getActivity(), R.string.invalid_port, Toast.LENGTH_SHORT).show();

                        //Reset values
                        SharedPreferences.Editor edit = getPreferenceManager().getSharedPreferences().edit();
                        edit.putString(editTextPref.getKey(), "");
                        edit.apply();
                        editTextPref.setText("");
                        resetSummary(editTextPref);

                    } else {
                        p.setSummary(editTextPref.getText());
                    }

                } else if (editTextPref.getKey().equals(getResources().getString((R.string.master_city_name)))) {
                    p.setSummary(editTextPref.getText());
                }
            }
        }
    }

    private void resetSummary(EditTextPreference p) {
        if (p.getKey().equals(getResources().getString(R.string.master_city_name))) {
            p.setSummary(getResources().getString(R.string.master_city_name_summary));
        } else if (p.getKey().equals(getResources().getString(R.string.master_port_local))) {
            p.setSummary(getResources().getString(R.string.master_port_local_summary));
        } else if (p.getKey().equals(getResources().getString(R.string.master_port_extern))) {
            p.setSummary(getResources().getString(R.string.master_port_extern_summary));
        } else if (p.getKey().equals(getResources().getString(R.string.master_port_intern))) {
            p.setSummary(getResources().getString(R.string.master_port_intern_summary));
        } else {
            p.setSummary("");
        }
    }
}
