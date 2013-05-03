package info.guardianproject.gpg;

import java.io.File;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnCreateContextMenuListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.freiheit.gnupg.GnuPGContext;
import com.freiheit.gnupg.GnuPGKey;

public class GnuPrivacyGuard extends Activity implements OnCreateContextMenuListener {
	public static final String TAG = "GnuPrivacyGuard";

	private ScrollView consoleScroll;
	private TextView consoleText;

	public static final String LOG_UPDATE = "LOG_UPDATE";
	public static final String COMMAND_FINISHED = "COMMAND_FINISHED";

	private CommandThread commandThread;
	private StringBuffer log;
	private BroadcastReceiver logUpdateReceiver;
	private BroadcastReceiver commandFinishedReceiver;
	public String command;

	boolean mIsBound;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NativeHelper.setup(getApplicationContext());

		new InstallTask(this).execute();

		setContentView(R.layout.main);
		consoleScroll = (ScrollView) findViewById(R.id.consoleScroll);
		consoleText = (TextView) findViewById(R.id.consoleText);

		log = new StringBuffer();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceivers();
		doBindService();
	}

	@Override
	protected void onPause() {
		super.onPause();
		doUnbindService();
		unregisterReceivers();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Set a popup EditText view to get user input
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		alert.setView(input);

		switch (item.getItemId()) {
	    case R.id.menu_settings_key:
	        startActivity(new Intent(this, GPGPreferenceActivity.class));
	        return true;
		case R.id.menu_list_keys:
			startActivity(new Intent(this, ListKeysActivity.class));
			return true;
		case R.id.menu_search_keys:
			alert.setTitle("Search Keys");
			alert.setPositiveButton("Search", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
			        GnuPGKey[] keylist;
			        String query = input.getText().toString();
			        keylist = NativeHelper.gpgCtx.searchKeys(query);
			        if( keylist == null ) {
			        	Log.i(TAG, "menu_search_keys: null");
			        } else {
				        for(GnuPGKey key : keylist){
				        	Log.i(TAG, "menu_search_keys: " + key.toString());
				        }
			        }
				}
			});
			alert.show();
			return true;
		case R.id.menu_receive_key:
			alert.setTitle("Receive Key");
			alert.setPositiveButton("Receive", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					command = NativeHelper.gpg2
							+ " --keyserver 200.144.121.45 --recv-keys " + input.getText().toString();
					commandThread = new CommandThread();
					commandThread.start();
				}
			});
			alert.show();
			return true;
		case R.id.menu_run_test:
//			command = NativeHelper.app_opt + "/tests/run-tests.sh";
			command = NativeHelper.gpg2 + " --import /data/data/info.guardianproject.gpg/app_opt/tests/pinentry/secret-keys.gpg";
			commandThread = new CommandThread();
			commandThread.start();
			Log.i(TAG, "finished " + command);
			return true;
		case R.id.menu_gen_key:
			command = NativeHelper.gpg2 + "-d /data/data/info.guardianproject.gpg/app_opt/tests/pinentry/secret.gpg";
			commandThread = new CommandThread();
			commandThread.start();
			Log.i(TAG, "finished " + command);
			return true;
			/*
			String batch = "Key-Type: DSA\nKey-Length: 1024\nSubkey-Type: ELG-E\nSubkey-Length: 1024\nName-Real: Test Key\nName-Comment: for testing only\nName-Email: test@gpg.guardianproject.info\nExpire-Date: 0\n%transient-key\n%no-protection\n%commit\n";
			File batchfile = new File(getCacheDir(), "batch.txt");
			try {
				FileWriter outFile = new FileWriter(batchfile);
				PrintWriter out = new PrintWriter(outFile);
				out.println(batch);
				out.close();
			} catch (Exception e) {
				Log.e(TAG, "Error!!!", e);
				return false;
			}
			command = NativeHelper.gpg2 + " --batch --gen-key "
					+ batchfile.getAbsolutePath();
			commandThread = new CommandThread();
			commandThread.start();
			return true;*/
		}
		return false;
	}

	private void updateLog() {
		final String logContents = log.toString();
		if (logContents != null && logContents.trim().length() > 0)
			consoleText.setText(logContents);
		consoleScroll.scrollTo(0, consoleText.getHeight());
	}

	class CommandThread extends Thread {
		private LogUpdate logUpdate;

		@Override
		public void run() {
			logUpdate = new LogUpdate();
			try {
				File dir = new File(NativeHelper.app_opt, "bin");
				Process sh = Runtime.getRuntime().exec("/system/bin/sh",
						NativeHelper.envp, dir);
				OutputStream os = sh.getOutputStream();

				StreamThread it = new StreamThread(sh.getInputStream(), logUpdate);
				StreamThread et = new StreamThread(sh.getErrorStream(), logUpdate);

				it.start();
				et.start();

				Log.i(TAG, command);
				writeCommand(os, command);
				writeCommand(os, "exit");

				sh.waitFor();
				Log.i(TAG, "Done!");
			} catch (Exception e) {
				Log.e(TAG, "Error!!!", e);
			} finally {
				synchronized (GnuPrivacyGuard.this) {
					commandThread = null;
				}
				sendBroadcast(new Intent(COMMAND_FINISHED));
			}
		}
	}

	class LogUpdate extends StreamThread.StreamUpdate {

		StringBuffer sb = new StringBuffer();

		@Override
		public void update(String val) {
			log.append(val);
			sendBroadcast(new Intent(LOG_UPDATE));
		}
	}

	public static void writeCommand(OutputStream os, String command) throws Exception {
		os.write((command + "\n").getBytes("ASCII"));
	}

	private void registerReceivers() {
		logUpdateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateLog();
			}
		};
		registerReceiver(logUpdateReceiver, new IntentFilter(GnuPrivacyGuard.LOG_UPDATE));

		commandFinishedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
			}
		};
		registerReceiver(commandFinishedReceiver, new IntentFilter(
				GnuPrivacyGuard.COMMAND_FINISHED));
	}

	private void unregisterReceivers() {
		if (logUpdateReceiver != null)
			unregisterReceiver(logUpdateReceiver);

		if (commandFinishedReceiver != null)
			unregisterReceiver(commandFinishedReceiver);
	}

	// TODO if GpgAgentService needs to send replies, then implement
	// MSG_REGISTER_CLIENT and IncomingHandler:
	// http://developer.android.com/reference/android/app/Service.html#RemoteMessengerServiceSample

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
		}

		public void onServiceDisconnected(ComponentName className) {
		}
	};

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		bindService(new Intent(GnuPrivacyGuard.this, GpgAgentService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	public class InstallTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog dialog;

		private final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				dialog.setMessage(msg.getData().getString("message"));
			}
		};

		private void showProgressMessage(int resId) {
			String messageText = getString(resId);
			if (messageText == null) messageText = "(null)";
			if (dialog == null) {
				Log.e(TAG, "installDialog is null!");
				return;
			}
			dialog.setMessage(messageText);
			if (!dialog.isShowing())
				dialog.show();
		}

		private void hideProgressDialog() {
			dialog.dismiss();
		}

		public InstallTask(Context c) {
			dialog = new ProgressDialog(c);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setTitle(R.string.dialog_installing_title);
		}

		@Override
		protected void onPreExecute() {
			showProgressMessage(R.string.dialog_installing_msg);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (!new File(NativeHelper.app_opt, "bin").exists()) {
				NativeHelper.unpackAssets(getApplicationContext(), handler);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			hideProgressDialog();

            // these need to be loaded before System.load("gnupg-for-java"); and in
            // the right order, since they have interdependencies.
            System.load(NativeHelper.app_opt + "/lib/libgpg-error.so.0");
            System.load(NativeHelper.app_opt + "/lib/libassuan.so.0");
            System.load(NativeHelper.app_opt + "/lib/libgpgme.so.11");

            Intent intent = new Intent(GnuPrivacyGuard.this, GpgAgentService.class);
            startService(intent);
            intent = new Intent(GnuPrivacyGuard.this, SharedDaemonsService.class);
            startService(intent);
            NativeHelper.gpgCtx = new GnuPGContext();
            // set the homeDir option to our custom home location
            NativeHelper.gpgCtx.setEngineInfo(NativeHelper.gpgCtx.getProtocol(), NativeHelper.gpgCtx.getFilename(), NativeHelper.app_home.getAbsolutePath());
        }
	}
}
