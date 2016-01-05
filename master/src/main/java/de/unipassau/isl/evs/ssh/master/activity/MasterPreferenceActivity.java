package de.unipassau.isl.evs.ssh.master.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import java.util.List;

import de.unipassau.isl.evs.ssh.master.R;

/**
 * Preferences Activity for the Master Odroid
 *
 * @author Christoph Frädrich
 */
public class MasterPreferenceActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    /**
     * This fragment shows the preferences for the first header.
     */
    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure default values are applied.  In a real app, you would
            // want this in a shared function that is used to retrieve the
            // SharedPreferences wherever they are needed.
            //TODO add defaults
            //PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return fragmentName.equals(PrefsFragment.class.getName());
    }
}
