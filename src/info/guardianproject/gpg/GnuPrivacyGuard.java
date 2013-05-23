package info.guardianproject.gpg;

import info.guardianproject.gpg.apg_compat.Apg;

import java.io.File;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGContext;


public class GnuPrivacyGuard extends Activity implements OnCreateContextMenuListener {
	public static final String TAG = "gpgcli";

	private ScrollView consoleScroll;
	private TextView consoleText;

	public static final String LOG_UPDATE = "LOG_UPDATE";
	public static final String COMMAND_FINISHED = "COMMAND_FINISHED";

	private CommandThread commandThread;
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

		wireTestButtons();
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
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		Log.i(GPGApplication.TAG, "Activity Result: " + requestCode + " " + resultCode);
		if (resultCode == RESULT_CANCELED || data == null) return;
		Bundle extras = data.getExtras();
		if (extras != null) {
			String text = "RESULT: ";
			switch (requestCode) {
			case ApgId.SELECT_SECRET_KEY:
				long keyId = extras.getLong(Apg.EXTRA_KEY_ID);
				String userId = extras.getString(Apg.EXTRA_USER_ID);
				text += userId + " " + Long.toHexString(keyId);
				break;
			case ApgId.SELECT_PUBLIC_KEYS:
				long[] selectedKeyIds = extras.getLongArray(Apg.EXTRA_SELECTION);
				String[] userIds = extras.getStringArray(Apg.EXTRA_USER_IDS);
				if (selectedKeyIds != null && userIds != null)
					for (int i = 0; i < selectedKeyIds.length && i < userIds.length; i++) {
						text += userIds[i] + " " + Long.toHexString(selectedKeyIds[i]) + " ";
						Log.i(GPGApplication.TAG, "received: " + userIds[i] + " " + Long.toHexString(selectedKeyIds[i]));
					}
				break;
			default:
				text += "unknown intent";
			}
			Toast.makeText(this, text, Toast.LENGTH_LONG).show();
		}
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
					Intent intent = new Intent(getApplicationContext(), SearchKeysActivity.class);
					intent.putExtra(Intent.EXTRA_TEXT, input.getText().toString());
					startActivity(intent);
				}
			});
			alert.show();
			return true;
		case R.id.menu_receive_key:
			alert.setTitle("Receive Key");
			alert.setPositiveButton("Receive", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
			        Context c = GPGApplication.getGlobalApplicationContext();
			        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
			        String ks = prefs.getString(GPGPreferenceActivity.PREF_KEYSERVER, "200.144.121.45");
					command = NativeHelper.gpg2
							+ " --keyserver " + ks + " --recv-keys " + input.getText().toString();
					commandThread = new CommandThread();
					commandThread.start();
				}
			});
			alert.show();
			return true;
		case R.id.menu_run_test:
			command = NativeHelper.app_opt + "/tests/run-tests.sh";
			commandThread = new CommandThread();
			commandThread.start();
			Log.i(TAG, "finished " + command);
			return true;
		case R.id.menu_import_test_key:
			command = NativeHelper.gpg2 + " --import /data/data/info.guardianproject.gpg/app_opt/tests/pinentry/secret-keys.gpg";
			commandThread = new CommandThread();
			commandThread.start();
			Log.i(TAG, "finished " + command);
			return true;
		case R.id.menu_share_log:
			shareTestLog();
			Log.i(TAG, "finished menu_share_log");
			return true;
		}
		return false;
	}

	private void updateLog() {
		final String logContents = NativeHelper.log.toString();
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
			NativeHelper.log.append(val);
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

    public static class ApgId {
        public static final String VERSION = "1";

        public static final String EXTRA_INTENT_VERSION = "intentVersion";

        public static final int DECRYPT = 0x21070001;
        public static final int ENCRYPT = 0x21070002;
        public static final int SELECT_PUBLIC_KEYS = 0x21070003;
        public static final int SELECT_SECRET_KEY = 0x21070004;
        public static final int GENERATE_SIGNATURE = 0x21070005;
    }

    private void setOnClick(Button button, final String intentName, final int intentId) {
		button.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Intent intent = new android.content.Intent(intentName);
                intent.putExtra(ApgId.EXTRA_INTENT_VERSION, ApgId.VERSION);
                try {
                    startActivityForResult(intent, intentId);
                    Toast.makeText(view.getContext(),
                            "started " + intentName + " " + intentId,
                            Toast.LENGTH_SHORT).show();
                    Log.i(GPGApplication.TAG, "started " + intentName + " " + intentId);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(view.getContext(),
                                   R.string.error_activity_not_found,
                                   Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

	private void wireTestButtons() {
		Button selectPublicKeysButton = (Button) findViewById(R.id.select_public_keys);
		setOnClick(selectPublicKeysButton, Apg.Intent.SELECT_PUBLIC_KEYS, ApgId.SELECT_PUBLIC_KEYS);

		Button selectSecretKeyButton = (Button) findViewById(R.id.select_secret_key);
		setOnClick(selectSecretKeyButton, Apg.Intent.SELECT_SECRET_KEY, ApgId.SELECT_SECRET_KEY);

		Button encryptButton = (Button) findViewById(R.id.encrypt);
		setOnClick(encryptButton, Apg.Intent.ENCRYPT, ApgId.ENCRYPT);

		Button encryptFileButton = (Button) findViewById(R.id.encrypt_file);
		setOnClick(encryptFileButton, Apg.Intent.ENCRYPT_FILE, ApgId.ENCRYPT);

		Button decryptButton = (Button) findViewById(R.id.decrypt);
		setOnClick(decryptButton, Apg.Intent.DECRYPT, ApgId.DECRYPT);

		Button decryptFileButton = (Button) findViewById(R.id.decrypt_file);
		setOnClick(decryptFileButton, Apg.Intent.DECRYPT_FILE, ApgId.DECRYPT);

		Button generateSignatureButton = (Button) findViewById(R.id.generate_signature);
		setOnClick(generateSignatureButton, Apg.Intent.GENERATE_SIGNATURE, ApgId.GENERATE_SIGNATURE);
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
		private boolean doInstall;

		private final Context context = getApplicationContext();
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
			doInstall = NativeHelper.installOrUpgradeAppOpt(context);
			if (doInstall)
				showProgressMessage(R.string.dialog_installing_msg);
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (doInstall)
				NativeHelper.unpackAssets(context, handler);
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

	protected void shareTestLog() {
		Intent i = new Intent(android.content.Intent.ACTION_SEND);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_SUBJECT, "test log from " + getString(R.string.app_name));
		i.putExtra(Intent.EXTRA_TEXT,
				"Attached is an log sent by " + getString(R.string.app_name)
						+ ".  For more info, see:\n"
						+ "https://github.com/guardianproject/gnupg-for-android\n\n"
						+ "manufacturer: " + Build.MANUFACTURER + "\n"
						+ "model: " + Build.MODEL + "\n"
						+ "product: " + Build.PRODUCT + "\n"
						+ "brand: " + Build.BRAND + "\n"
						+ "device: " + Build.DEVICE + "\n"
						+ "board: " + Build.BOARD + "\n"
						+ "ID: " + Build.ID + "\n"
						+ "CPU ABI: " + Build.CPU_ABI + "\n"
						+ "release: " + Build.VERSION.RELEASE + "\n"
						+ "incremental: " + Build.VERSION.INCREMENTAL + "\n"
						+ "codename: " + Build.VERSION.CODENAME + "\n"
						+ "SDK: " + Build.VERSION.SDK_INT + "\n"
						+ "\n\nlog:\n----------------------------------\n"
						+ consoleText.getText().toString()
						);
		startActivity(Intent.createChooser(i, "How do you want to share?"));
	}
}
