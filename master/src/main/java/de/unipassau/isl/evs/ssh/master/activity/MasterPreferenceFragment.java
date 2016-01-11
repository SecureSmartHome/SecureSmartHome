package de.unipassau.isl.evs.ssh.master.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import com.google.common.base.Strings;

import net.aksingh.owmjapis.OpenWeatherMap;

import java.io.IOException;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.master.MasterConstants;
import de.unipassau.isl.evs.ssh.master.R;

/**
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
                if (editTextPref.getKey().equals(getResources().getResourceEntryName(R.string.master_port_local))
                        || editTextPref.getKey().equals(getResources().getResourceEntryName(R.string.master_port_extern))
                        || editTextPref.getKey().equals(getResources().getResourceEntryName(R.string.master_port_intern))) {
                    //We can be sure this is a number due to the preference Activity only accepting numbers
                    int port = Integer.valueOf(editTextPref.getText());

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

                } else if (editTextPref.getKey().equals(getResources().getResourceEntryName((R.string.master_city_name)))) {
                    p.setSummary(editTextPref.getText());
                }
            }
        }
    }

    private void resetSummary(EditTextPreference p) {
        int resId = getResources().getIdentifier(p.getKey() + "_summary", "string",
                getActivity().getPackageName());

        String summary = getResources().getString(resId);

        p.setSummary(summary);
    }
}
