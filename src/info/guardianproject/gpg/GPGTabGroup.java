package info.guardianproject.gpg;

import java.io.File;
import java.io.OutputStream;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.adapters.GPGScreen;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.Toast;

public class GPGTabGroup extends TabActivity implements Constants {
	TabHost tabs;
	TabHost.TabSpec spec;
	FrameLayout tabContent;
	Resources res;
	
	private CommandThread commandThread;
	private StringBuffer log;
	private BroadcastReceiver logUpdateReceiver;
	private BroadcastReceiver commandFinishedReceiver;
	public String command;

	boolean mIsBound;
		
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.tablayout);
		
		NativeHelper.setup(getApplicationContext());
		// TODO figure out how to manage upgrades to app_opt
		if (!new File(NativeHelper.app_opt, "bin").exists()) {
			Log.d(LOG, "fucking native shit doesnt exist");
			NativeHelper.unpackAssets(getApplicationContext());
		}
		
		log = new StringBuffer();

		Intent intent = new Intent(GPGTabGroup.this, AgentsService.class);
		startService(intent);
		
		res = getResources();
		tabs = getTabHost();
		
		setAssets();
		
		registerReceivers();
		doBindService();
	}
	
	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
		unregisterReceivers();
	}
	
	public void setAssets() {		
		GPGScreen myKeys = new GPGScreen(MyKeys.TAG, new Intent(this, MyKeys.ROOT));
		GPGScreen keyManager = new GPGScreen(KeyManager.TAG, new Intent(this, KeyManager.ROOT));
		GPGScreen webOfTrust = new GPGScreen(WebOfTrust.TAG, new Intent(this, WebOfTrust.ROOT));
		
		spec = tabs.newTabSpec(KeyManager.TAG)
				.setIndicator(res.getString(R.string.indicator_keyManager))
				.setContent(keyManager.intent);
		tabs.addTab(spec);
		
		spec = tabs.newTabSpec(MyKeys.TAG) 
					.setIndicator(res.getString(R.string.indicator_myKeys))
					.setContent(myKeys.intent);
		tabs.addTab(spec);
		
		spec = tabs.newTabSpec(WebOfTrust.TAG) 
				.setIndicator(res.getString(R.string.indicator_webOfTrust))
				.setContent(webOfTrust.intent);
		tabs.addTab(spec);
		
		tabs.setCurrentTab(0);
	}
	
	public void alert(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG);
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

				Log.i(LOG, command);
				writeCommand(os, command);
				writeCommand(os, "exit");

				sh.waitFor();
				Log.i(LOG, "Done!");
			} catch (Exception e) {
				Log.e(LOG, "Error!!!", e);
			} finally {
				synchronized (GPGTabGroup.this) {
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
	
	private void updateLog() {
		final String logContents = log.toString();
		if (logContents != null && logContents.trim().length() > 0)
			Log.d(LOG, logContents);
	}
	
	private void registerReceivers() {
		logUpdateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(LOG, "received broadcast");
				updateLog();
			}
		};
		registerReceiver(logUpdateReceiver, new IntentFilter(GPGTabGroup.LOG_UPDATE));

		commandFinishedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
			}
		};
		registerReceiver(commandFinishedReceiver, new IntentFilter(COMMAND_FINISHED));
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
		bindService(new Intent(GPGTabGroup.this, AgentsService.class), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
		Log.d(LOG, "service bound");
	}

	void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
			Log.d(LOG, "service UNbound");
		}
	}
}
