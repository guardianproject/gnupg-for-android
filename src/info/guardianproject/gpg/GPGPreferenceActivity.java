package info.guardianproject.gpg;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class GPGPreferenceActivity extends PreferenceActivity {

    public final static String PREF_START_BOOT = "pref_start_boot";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
