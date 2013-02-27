package info.guardianproject.gpg.pinentry;

import info.guardianproject.gpg.AgentsService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

public class ServerSocketThread extends Thread {
	public String TAG = "ServerSocketThread";

	public static String SOCKET_ADDRESS = "info.guardianproject.gpg.pinentryhelper";

	private static String CMD_START = "START";
	private static String CMD_PING = "PING";

	AgentsService mService;
	byte[] buffer;
	int bytesRead;
	LocalServerSocket serverSocket;
	InputStream input;
	private volatile boolean stopThread;

	public ServerSocketThread(AgentsService service) {
		Log.d(TAG, "ctor");
		buffer = new byte[6];
		bytesRead = 0;
		stopThread = false;
		mService = service;
		try {
			serverSocket = new LocalServerSocket(SOCKET_ADDRESS);
		} catch (IOException e) {
			Log.d(TAG, "Error encountered when creating the LocalServerSocket");
			e.printStackTrace();
		}
	}

	public void run() {
		LocalSocket socket = null;
		Log.d(TAG, "run()");
		while (!stopThread && serverSocket != null) {
			try {
				Log.d(TAG, "waiting for clients - accept()");

				socket = serverSocket.accept();
				Log.d(TAG, "client connected");
			} catch (IOException e) {
				Log.d(TAG, "accept failed");
				e.printStackTrace();
				continue;
			}

			try {
				BufferedReader reader = null;
				OutputStream out = socket.getOutputStream();
				input = socket.getInputStream();
				reader = new BufferedReader(new InputStreamReader(input));

				Log.d(TAG, "reading command");
				String cmd = reader.readLine();
				if( cmd == null) {
					Log.d(TAG, "conection closed");
				} else if(cmd.length() == 0 ) {
					Log.d(TAG, "received 0 length command");
					out.write(0);
				} else {
					Log.d(TAG, "received command: '" + cmd +"'");
					int data = handleCommand(cmd);
					out.write(data);

				}
			} catch (IOException e1) {
				Log.d(TAG, "server reading error");
				e1.printStackTrace();
			}

		}
		Log.d(TAG, "stopping..");
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private int handleCommand(String cmd) {
		if( cmd.equals(CMD_START)) {
			Log.d(TAG, "Starting pinentry");
			mService.startPinentry();
			return 1;
		} else if( cmd.equals(CMD_PING) ) {
			Log.d(TAG, "Ping received!");
			return 2;
		} else {
			Log.d(TAG, "unknown command " + cmd);
		}
		return 0;
	}

	public void setStopThread(boolean value) {
		Log.d(TAG, "setStopthread called value="+value);
		stopThread = value;

		try {
			if( serverSocket != null )
				serverSocket.close();
		} catch (IOException e) {
			Log.d(TAG, "closing server socket exception");
			e.printStackTrace();
		}
		Thread.currentThread().interrupt(); // TODO : Check
	}
}
