package info.guardianproject.gpg;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AgentsService extends Service {
	public static final String TAG = "AgentsService";

	/** For showing and hiding our notification. */
	NotificationManager mNM;

	private LaunchAgentsThread launchAgentsThread;

	private void startDaemons() {
		Log.i(TAG, "start daemons in " + NativeHelper.app_opt.getAbsolutePath());
		synchronized (this) {
			launchAgentsThread = new LaunchAgentsThread();
			launchAgentsThread.start();
		}
	}

	@Override
	public void onCreate() {
		// since this service is a separate process, it has its own instance of
		// NativeHelper
		NativeHelper.setup(this);

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(R.string.remote_service_started);
		Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
	}

	public class LocalBinder extends Binder {
		public AgentsService getService() {
			return AgentsService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		startDaemons();
		return START_STICKY;
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.remote_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.icon, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				GnuPrivacyGuard.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.remote_service_label),
				text, contentIntent);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to
		// cancel.
		mNM.notify(R.string.remote_service_started, notification);
	}

	class LaunchAgentsThread extends Thread {

		@Override
		public void run() {
			if (NativeHelper.app_opt == null) {
				Log.i(TAG, "bailing app_opt == null");
				return;
			}
			String gpgAgentCmd = NativeHelper.app_opt.getAbsolutePath()
					+ "/bin/gpg-agent --daemon --write-env-file";
			Log.i(TAG, gpgAgentCmd);
			String dirmngrCmd = NativeHelper.app_opt.getAbsolutePath()
					+ "/bin/dirmngr --no-detach";
			Log.i(TAG, dirmngrCmd);
			try {
				// gpg-agent --daemon detaches and lives on, dirmngr stays
				// attached
				Runtime.getRuntime()
						.exec(gpgAgentCmd, NativeHelper.envp, NativeHelper.app_home)
						.waitFor();
				Runtime.getRuntime()
						.exec(dirmngrCmd, NativeHelper.envp, NativeHelper.app_home)
						.waitFor();
			} catch (Exception e) {
				Log.e(TAG, "Could not start gpg-agent or dirmngr", e);
			} finally {
				stopSelf();
				synchronized (AgentsService.this) {
					launchAgentsThread = null;
				}
			}
		}
	}

}
