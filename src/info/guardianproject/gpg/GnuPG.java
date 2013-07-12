package info.guardianproject.gpg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

	public static int gpg2(String args) {
		final String TAG = "gpg2";
		String command = NativeHelper.gpg2 + " " + args;
		Log.i(TAG, command);
		try {
            Process sh = Runtime.getRuntime().exec("/system/bin/sh",
                    NativeHelper.envp, NativeHelper.app_home);
            OutputStream stdin = sh.getOutputStream();
            InputStream stdout = sh.getInputStream();
            InputStream stderr = sh.getErrorStream();

            stdin.write((command + "\nexit\n").getBytes("ASCII"));
            sh.waitFor();

            Log.i("stdout", readResult(stdout));
            Log.w("stderr", readResult(stderr));
			Log.i(TAG, "finished: " + command + "  exit value: " + sh.exitValue());
			return sh.exitValue();
		} catch (Exception e) {
			Log.e(TAG, "FAILED: " + command, e);
		}
		return 1;
	}

    private static String readResult(InputStream i) {
        String ret = "";
        try {
            byte[] readBuffer = new byte[512];
            int readCount = -1;
            while ((readCount = i.read(readBuffer)) > 0) {
                ret += new String(readBuffer, 0, readCount);
            }
        } catch (IOException e) {
            Log.e("GnuPG", "readResult", e);
        }
        return ret;
    }

}
