package info.guardianproject.gpg;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import android.util.Log;

public class Posix {
	public static final String TAG = "Posix";

    static {
        System.loadLibrary("posix");
    }

    public static native int umask(int mask);

	public static void kill9(String command) {
		Integer pid = pidof(command);
		if (pid == -1) {
			Log.w(TAG, "No running process found for " + command);
			return;
		}
		Log.d(TAG, "killing " + command + " at " + pid.toString());
		try {
			Runtime.getRuntime().exec("kill " + pid.toString()).waitFor();
			Thread.sleep(1000);
			Runtime.getRuntime().exec("kill -9 " + pid.toString()).waitFor();
		} catch (Exception e) {
			Log.e(TAG, "Unable to kill " + command + " at pid " + pid.toString(), e);
		}
	}

	public static int pidof(String command) {
		int pid = -1;
		try {
			pid = findProcessIdWithPIDOF(command);
			if (pid == -1)
				pid = findProcessIdWithPS(command);
		} catch (Exception e) {
			try {
				pid = findProcessIdWithPS(command);
			} catch (Exception e2) {
				Log.w(TAG, "Unable to get proc id for: " + command, e2);
			}
		}
		return pid;
	}

	// use 'pidof' command
	public static int findProcessIdWithPIDOF(String command) throws Exception {
		int pid = -1;
		String baseName = new File(command).getName();
		Process procPs = Runtime.getRuntime().exec(new String[] { "pidof", baseName });
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				procPs.getInputStream()));
		String line = null;
		while ((line = reader.readLine()) != null) {
			/* this line should just be the process id, but sometimes its
			 * multiple. so we just return the first one found, for now */
			for (String pidString : line.split(" ")) {
				try {
					pid = Integer.parseInt(pidString);
					break;
				} catch (NumberFormatException e) {
					Log.i(TAG, "unable to parse process pid: " + pidString, e);
				}
			}
			if (pid > -1) break;
		}
		return pid;
	}

	// use 'ps' command
	public static int findProcessIdWithPS(String command) throws Exception {
		int pid = -1;
		Process procPs = Runtime.getRuntime().exec("ps");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				procPs.getInputStream()));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.indexOf(' ' + command) != -1) {
				StringTokenizer st = new StringTokenizer(line, " ");
				st.nextToken(); // proc owner
				pid = Integer.parseInt(st.nextToken().trim());
				break;
			}
		}
		return pid;
	}
}
