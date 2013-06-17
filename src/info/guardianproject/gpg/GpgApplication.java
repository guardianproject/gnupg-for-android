package info.guardianproject.gpg;

import java.io.File;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class GpgApplication extends Application {
	public static final String TAG = "GpgApplication";

	public static String PACKAGE_NAME = null;
	public static String VERSION_NAME = null;
	public static int VERSION_CODE = 0;

	Context mContext;

	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate");
		super.onCreate();

		makeVersions();

		mContext = getApplicationContext();
		NativeHelper.setup(mContext);

		if (NativeHelper.installOrUpgradeNeeded()) {
			Intent intent = new Intent(mContext, InstallActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			// InstallActivity runs setup() when its done
		} else {
			setup();
		}
	}

	/**
	 * This handles setting up stuff that is unpacked from the assets. The
	 * assets must have already been unpacked before this can be run.
	 */
	void setup() {
		// these need to be loaded before System.load("gnupg-for-java"); and
		// in the right order, since they have interdependencies.
		System.load(NativeHelper.app_opt + "/lib/libgpg-error.so.0");
		System.load(NativeHelper.app_opt + "/lib/libassuan.so.0");
		System.load(NativeHelper.app_opt + "/lib/libgpgme.so.11");

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

}
