
package info.guardianproject.gpg;

import java.io.File;

import android.accounts.Account;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class GpgApplication extends Application {
    public static final String TAG = "GpgApplication";

    public static String PACKAGE_NAME = null;
    public static String VERSION_NAME = null;
    public static int VERSION_CODE = 0;

    public static Account mSyncAccount = null;
    public static final String BROADCAST_ACTION_KEYLIST_CHANGED = "info.guardianproject.gpg.keylist";

    static Context mContext;

    /* request codes for intents */
    // must us only lowest 16 bits, otherwise you get (not sure under which
    // conditions exactly)
    // java.lang.IllegalArgumentException: Can only use lower 16 bits for
    // requestCode
    public static final int FILENAME = 0x00007006;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();

        makeVersions();

        mContext = getApplicationContext();
        NativeHelper.setup(mContext);

        /*
         * Setup the global environment for all processes launched from this
         * app. LD_LIBRARY_PATH is the only way Android finds shared libraries
         * when running things from the command line
         */
        Posix.setenv("LD_LIBRARY_PATH", NativeHelper.ldLibraryPath, true);

        if (NativeHelper.installOrUpgradeNeeded()) {
            /*
             * Currently InstallActivity is triggered in
             * MainActivity.onCreate(), since that's the LAUNCHER Activity. When
             * InstallActivity is complete, it runs setup().
             */
        } else {
            /*
             * setup() is run here so that we can be sure it was run before any
             * Activity has started, so things like GnuPG.context won't be null.
             */
            Posix.umask(00022); // set umask to make app_opt/ accessible by everyone
            setup();
        }
        // This umask allows owner access only. From here on out, the files that
        // the GPG app writes should have a restricted umask, this is files like
        // the UNIX sockets for gpg-agent. Also it is needed on SDK < 4.1
        // because otherwise all files will be 777
        Posix.umask(00077);
    }

    /**
     * This handles setting up stuff that is unpacked from the assets. The
     * assets must have already been unpacked before this can be run.
     */
    void setup() {
        Log.i(TAG, "setup");
        // these need to be loaded before System.load("gnupg-for-java"); and
        // in the right order, since they have interdependencies.
        System.loadLibrary("gpg-error");
        System.loadLibrary("assuan");
        System.loadLibrary("gpgme");
        GnuPG.createContext();
        startGpgAgent(mContext);
        startSharedDaemons(mContext);
    }

    private void makeVersions() {
        try {
            PACKAGE_NAME = getPackageName();
            PackageInfo pi = getPackageManager()
                    .getPackageInfo(PACKAGE_NAME, 0);
            VERSION_NAME = pi.versionName;
            VERSION_CODE = pi.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void onLowMemory() {
        // TODO kill dirmngr and maybe gpg-agent here
    }

    public static void startGpgAgent(Context context) {
        File gpgAgentSocket = new File(NativeHelper.app_home, "S.gpg-agent");
        if (!gpgAgentSocket.exists()) {
            // gpg-agent is not running, start it
            Intent service = new Intent(context, GpgAgentService.class);
            context.startService(service);
        }
    }

    public static void startSharedDaemons(Context context) {
        File dirmngrSocket = new File(NativeHelper.app_opt,
                "var/run/gnupg/S.dirmngr");
        if (!dirmngrSocket.exists()) {
            // dirmngr is not running, start it
            Intent service = new Intent(context, SharedDaemonsService.class);
            context.startService(service);
        }
    }

    protected static void sendKeylistChangedBroadcast(Context c) {
        LocalBroadcastManager.getInstance(c).sendBroadcast(
                new Intent(BROADCAST_ACTION_KEYLIST_CHANGED));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (prefs.getBoolean(mContext.getString(R.string.pref_contacts_integration), true))
            requestContactsSync();
    }

    /**
     * Request sync with the Contacts for the default account, authority. This
     * is set at a normal priority, so it might take a few seconds for the sync
     * to kick in.
     */
    static void requestContactsSync() {
        requestContactsSync(false);
    }

    /**
     * Request sync with the Contacts for the default account, authority. You
     * can set the sync to be expedited to try to force it to happen
     * immediately.
     * 
     * @param <code>expedited</code> force the sync to start immediately
     */
    static void requestContactsSync(boolean expedited) {
        if (mSyncAccount == null)
            return;
        Bundle settingsBundle = new Bundle();
        // set SYNC_EXTRAS_MANUAL to force when setSyncAutomatically is false
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, expedited);
        ContentResolver.requestSync(mSyncAccount, ContactsContract.AUTHORITY, settingsBundle);
    }

}
