package info.guardianproject.gpg.pinentry;

import java.io.IOException;
import java.io.InputStream;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

public class ServerSocketThread extends Thread {
	public String TAG = "ServerSocketThread";
	byte[] buffer;
	int bytesRead;
	LocalServerSocket server;
	InputStream input;
	private volatile boolean stopThread;

	public ServerSocketThread() {
		Log.d(TAG, "ctor");
		buffer = new byte[6];
		bytesRead = 0;
		stopThread = false;
		try {
			server = new LocalServerSocket(PINEntry.SOCKET_ADDRESS);
		} catch (IOException e) {
			Log.d(TAG, "Error encountered when creating the LocalServerSocket");
			e.printStackTrace();
		}
	}

	public void run() {
		LocalSocket socket = null;
		Log.d(TAG, "run()");
		while (!stopThread && server != null) {
			try {
				Log.d(TAG, "waiting for clients - accept()");

				socket = server.accept();
				Log.d(TAG, "client connected");
			} catch (IOException e) {
				Log.d(TAG, "accept failed");
				e.printStackTrace();
				continue;
			}
			try {
				input = socket.getInputStream();
			} catch (IOException e1) {
				Log.d(TAG, "client inputstream error");
				e1.printStackTrace();
			}
			while (socket != null && bytesRead >= 0) {
				try {
					bytesRead = input.read(buffer, 0,
							buffer.length);
				} catch (IOException e) {
					Log.d(TAG, "reading input socket failiure");
					e.printStackTrace();
					break;
				}
				if( bytesRead > 0 )
					Log.d(TAG, "Read " + bytesRead + " bytes: " + new String(buffer));
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
		if (server != null) {
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setStopThread(boolean value) {
		stopThread = value;
		Thread.currentThread().interrupt(); // TODO : Check
	}
}
