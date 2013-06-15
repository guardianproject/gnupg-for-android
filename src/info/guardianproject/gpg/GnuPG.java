package info.guardianproject.gpg;

import android.util.Log;

import com.freiheit.gnupg.GnuPGContext;

public class GnuPG {
	public static GnuPGContext context = null;

	public static void createContext() {
		context = new GnuPGContext();
		// set the homeDir option to our custom home location
		context.setEngineInfo(context.getProtocol(), context.getFilename(),
				NativeHelper.app_home.getAbsolutePath());
	}

	public static void gpg2(String args) {
		final String TAG = "gpg2";
		String command = NativeHelper.gpg2 + " " + args;
		Log.i(TAG, command);
		try {
			Runtime.getRuntime()
					.exec(command, NativeHelper.envp, NativeHelper.app_home)
					.waitFor();
			Log.i(TAG, "finished: " + command);
		} catch (Exception e) {
			Log.e(TAG, "FAILED: " + command, e);
		}

	}
}
