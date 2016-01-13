package de.unipassau.isl.evs.ssh.master.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
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
        if (!isSwitching()) { //TODO Niko: check if MasterPreferenceActivity can be displayed after setup (Niko, 2016-01-13)
            setContentView(R.layout.activity_master_preference);
            findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (allPreferencesSet()) {
                        getSharedPreferences().edit()
                                .putBoolean(PREF_PREFERENCES_SET, true)
                                .commit();
                        doBind();
                    } else {
                        Toast.makeText(MasterPreferenceActivity.this, "Please set all preferences before continuing", Toast.LENGTH_SHORT)
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

    public boolean allPreferencesSet() {
        final SharedPreferences prefs = getSharedPreferences();
        final SharedPreferences.Editor edit = prefs.edit();

        try {
            return validatePort(prefs, edit, getResources().getString(R.string.master_port_local), Server.PREF_SERVER_LOCAL_PORT) &&
                    validatePort(prefs, edit, getResources().getString(R.string.master_port_intern), Server.PREF_SERVER_LOCAL_PORT) &&
                    validatePort(prefs, edit, getResources().getString(R.string.master_port_extern), null) &&
                    !Strings.isNullOrEmpty(prefs.getString(getResources().getString(R.string.master_city_name), null));
        } catch (ClassCastException e) {
            return false;
        } finally {
            edit.commit();
        }
    }

    private boolean validatePort(SharedPreferences prefs, SharedPreferences.Editor edit, String stringPref, String intPref) {
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