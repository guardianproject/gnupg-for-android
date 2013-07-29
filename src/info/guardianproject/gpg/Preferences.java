
package info.guardianproject.gpg;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

    public static boolean startOnBoot(Context c) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        return sharedPref.getBoolean(GPGPreferenceActivity.PREF_START_BOOT, true);
    }

}
