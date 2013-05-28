package info.guardianproject.gpg;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class GpgAgentService extends Service {
	public static final String TAG = "GpgAgentService";

	private GpgAgentThread gpgAgentThread;

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		// Owner permissions only. Necessary on pre-4.1 SDKs.
		if( Build.VERSION.SDK_INT < 16 ) {
		    Posix.umask(00077);
		}
		// since this is a separate process, it has its own instance of NativeHelper
		NativeHelper.setup(this);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		startGpgAgent();
		return START_STICKY;
	}

	private void startGpgAgent() {
		Log.i(TAG, "starting " + NativeHelper.gpg_agent);
		synchronized (this) {
			gpgAgentThread = new GpgAgentThread();
			gpgAgentThread.start();
		}
	}

	class GpgAgentThread extends Thread {

		@Override
		public void run() {
			String gpgAgentCmd = NativeHelper.gpg_agent
                    + " --pinentry-program " + NativeHelper.pinentry_android
					+ " --daemon --write-env-file " + "--debug-level 3 --log-file "
					+ NativeHelper.app_log + "/gpg-agent.log";
			Log.i(TAG, gpgAgentCmd);
			try {
				Runtime.getRuntime()
						.exec(gpgAgentCmd, NativeHelper.envp, NativeHelper.app_home)
						.waitFor();
				Log.i(TAG, "gpgAgentCmd finished");
			} catch (Exception e) {
				Log.e(TAG, "Could not start gpg-agent", e);
			} finally {
				Log.i(TAG, "finally stopSelf()");
				stopSelf();
				synchronized (GpgAgentService.this) {
					gpgAgentThread = null;
				}
			}
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// we're not using bindService() at all, but onBind() is required
		return null;
	}
}
