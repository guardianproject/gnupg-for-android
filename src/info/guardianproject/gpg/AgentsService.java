package info.guardianproject.gpg;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

public class AgentsService extends Service {
    /** For showing and hiding our notification. */
    NotificationManager mNM;

	static final int MSG_START = 1;
	static final int MSG_STOP = 2;

	private void startDaemons() {
		Log.i(GnuPrivacyGuard.TAG, "start daemons");
		// TODO start gpg-agent and dirmngr from here
	}

	private void stopDaemons() {
		Log.i(GnuPrivacyGuard.TAG, "stop daemons");
		// TODO start gpg-agent and dirmngr from here
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_START:
				startDaemons();
				break;
			case MSG_STOP:
				stopDaemons();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting.
		showNotification();
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(R.string.remote_service_started);
		// Tell the user we stopped.
		Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
	}

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
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
}
