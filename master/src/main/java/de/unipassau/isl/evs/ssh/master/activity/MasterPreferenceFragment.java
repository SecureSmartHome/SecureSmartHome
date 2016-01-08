package de.unipassau.isl.evs.ssh.master.activity;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import de.unipassau.isl.evs.ssh.master.MasterConstants;
import de.unipassau.isl.evs.ssh.master.R;

/**
 * @author Niko Fink
 */
public class MasterPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(MasterConstants.FILE_SHARED_PREFS);

        // Make sure default values are applied.  In a real app, you would
        // want this in a shared function that is used to retrieve the
        // SharedPreferences wherever they are needed.
        //TODO add defaults
        //PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
