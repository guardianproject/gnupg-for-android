package info.guardianproject.gpg;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

    public static boolean startOnBoot() {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(GPGApplication.getGlobalApplicationContext());
        return sharedPref.getBoolean(GPGPreferenceActivity.PREF_START_BOOT, true);
    }

}
