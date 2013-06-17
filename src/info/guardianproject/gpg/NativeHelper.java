package info.guardianproject.gpg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Scanner;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.freiheit.gnupg.GnuPGContext;

public class NativeHelper {
	public static final String TAG = "NativeHelper";

	public static File app_opt; // an /opt tree for the UNIX cmd line tools
	public static File app_log; // a place to store logs
	public static File app_home; // dir for $HOME and ~/.gnupg
	public static File app_gnupghome; // dir for $GNUPGHOME for other apps like Terminal Emulator
	public static File versionFile; // version stamp for unpacked assets

	// full paths to key executables, with globally used flags
	public static String gpg2;
	public static String gpg_agent;
	public static String pinentry_android;
	public static String dirmngr;

	public static String sdcard;
	public static String[] envp; // environment variables

	protected static GnuPGContext gpgCtx;

	private static Context context;
	protected static StringBuffer log;

	public static void setup(Context c) {
		context = c;
		app_opt = context.getDir("opt", Context.MODE_WORLD_READABLE).getAbsoluteFile();
		app_log = context.getDir("log", Context.MODE_PRIVATE).getAbsoluteFile();
		app_home = context.getDir("home", Context.MODE_PRIVATE).getAbsoluteFile();
		app_gnupghome = context.getDir("gnupghome", Context.MODE_WORLD_WRITEABLE).getAbsoluteFile();
		versionFile = new File(app_opt, "VERSION");

		File bin = new File(app_opt, "bin");
		String logging = "--debug-level basic --log-file " + NativeHelper.app_log
				+ "/gpg2.log ";
		gpg2 = new File(bin, "gpg2").getAbsolutePath() + " --no-tty " + logging;
		gpg_agent = new File(bin, "gpg-agent").getAbsolutePath();
		pinentry_android = new File(bin, "pinentry-android").getAbsolutePath();
		dirmngr = new File(bin, "dirmngr").getAbsolutePath();

		sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
		String ldLibraryPath = System.getenv("LD_LIBRARY_PATH");
		String path = System.getenv("PATH");
		envp = new String[] { "HOME=" + NativeHelper.app_home,
				"GNUPGHOME=" + NativeHelper.app_home,
				"PINENTRY_USER_DATA=" + constructPinentryUserData(),
				"LD_LIBRARY_PATH=" + NativeHelper.app_opt + "/lib" + ":" + ldLibraryPath,
				"PATH=" + path + ":" + bin.getAbsolutePath(),
				"app_opt=" + app_opt.getAbsolutePath() };

		log = new StringBuffer();

		Log.i(TAG, "Finished NativeHelper.setup()");
	}

    private static String getUserNumber() {
        /* Getting the user number requires APIs not added till SDK 17
         * So we use reflection.
         * See https://bugzilla.mozilla.org/show_bug.cgi?id=811763#c11

        if (Build.VERSION.SDK_INT >= 17) {
            android.os.UserManager um = (android.os.UserManager) context
                    .getSystemService(Context.USER_SERVICE);
            if (um != null) {
                return um.getSerialNumberForUser(android.os.Process.myUserHandle());
            } else {
                Log.d(TAG, "Unable to obtain user manager service on a device with SDK version "
                        + Build.VERSION.SDK_INT);
            }
        }*/
        try {
            Object userManager = context.getSystemService("user");
            if (userManager != null) {
                Log.d(TAG, "got us a nonnull user manager!");
                // if userManager is non-null that means we're running on 4.2+
                // and so the rest of this
                // should just work
                Object userHandle = android.os.Process.class.getMethod(
                        "myUserHandle", (Class[]) null).invoke(null);
                Object userSerial = userManager
                        .getClass()
                        .getMethod("getSerialNumberForUser",
                                userHandle.getClass())
                        .invoke(userManager, userHandle);
                return userSerial.toString();
            }
        } catch (Exception e) {
            // we're not yet on 4.2
        }
        return new String();
    }

    /**
     * returns a string containing the
     *  "LD_LIBRARY_PATH;BOOTCLASSPATH;USERID;"
     * @return
     */
	private static String constructPinentryUserData() {
	    String pinentryUserData = new String();

	    pinentryUserData += System.getenv("LD_LIBRARY_PATH");
	    pinentryUserData += ";";

	    // we need to pass BOOTCLASSPATH for all versions
	    pinentryUserData += System.getenv("BOOTCLASSPATH");
	    pinentryUserData += ";";
	    /*
         * As per https://code.google.com/p/android/issues/detail?id=39801
         * on Android 4.2 we need to pass the userid to "am start"
         * with --user, because 4.2 supports multiple users.
         */
	    String u = getUserNumber();
	    if( !TextUtils.isEmpty(u) ) {
	        pinentryUserData += getUserNumber();
	    } else {
	        pinentryUserData += "-1";
	    }
	    pinentryUserData += ";";
	    return pinentryUserData;
	}

	private static void copyFileOrDir(String path, File dest) {
		AssetManager assetManager = context.getAssets();
		String assets[] = null;
		try {
			assets = assetManager.list(path);
			if (assets.length == 0) {
				copyFile(path, dest);
			} else {
				File destdir = new File(dest, new File(path).getName());
				if (!destdir.exists())
					destdir.mkdirs();
				for (int i = 0; i < assets.length; ++i) {
					copyFileOrDir(new File(path, assets[i]).getPath(), destdir);
				}
			}
		} catch (IOException ex) {
			Log.e(TAG, "I/O Exception", ex);
		}
	}

	private static void copyFile(String filename, File dest) {
		AssetManager assetManager = context.getAssets();

		InputStream in = null;
		OutputStream out = null;
		try {
			in = assetManager.open(filename);
			out = new FileOutputStream(new File(app_opt, filename).getAbsolutePath());

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		} catch (Exception e) {
			Log.e(TAG, filename + ": " + e.getMessage());
		}

	}

	/*
	 * since we are using the whole gnupg package of programs and libraries, we
	 * are setting up the whole UNIX directory tree that it expects
	 */
	private static void setupEmptyDirs() {
		new File(app_opt, "etc/gnupg/trusted-certs").mkdirs();
		new File(app_opt, "share/gnupg/extra-certs").mkdirs();
		new File(app_opt, "var/run/gnupg").mkdirs();
		new File(app_opt, "var/lib/gnupg").mkdirs();
		new File(app_opt, "var/cache/gnupg").mkdirs();
		// /home is outside of this tree, in app_home
	}

	/* write out a sh .profile file to ease testing in the terminal */
	private static void writeShProfile() {
		File etc_profile = new File(app_opt, "etc/profile");
		String global = "";
		for (String s : envp) {
			global += "export " + s + "\n";
		}
		File home_profile = new File(app_home, ".profile");
		String local = ". " + etc_profile.getAbsolutePath() + "\n. "
				+ new File(app_home, ".gpg-agent-info").getAbsolutePath() + "\n"
				+ "export GPG_AGENT_INFO\n" + "export SSH_AUTH_SOCK\n";
		try {
			FileWriter outFile = new FileWriter(etc_profile);
			PrintWriter out = new PrintWriter(outFile);
			out.println(global);
			out.close();
			outFile = new FileWriter(home_profile);
			out = new PrintWriter(home_profile);
			out.println(local);
			out.close();
		} catch (Exception e) {
			Log.e(TAG, "Cannot write file: ", e);
		}
	}

	static void showMessageInDialog(String message, Handler handler) {
		Message msg = handler.obtainMessage();
		Bundle b = new Bundle();
		b.putString("message", message);
		msg.setData(b);
		handler.sendMessage(msg);
	}

	public static void unpackAssets(Context context, Handler handler) {
		Log.i(TAG, "Setting up assets in " + app_opt);
		setupEmptyDirs();
		writeShProfile();

		AssetManager am = context.getAssets();
		final String[] assetList;
		try {
			assetList = am.list("");
		} catch (IOException e) {
			Log.e(TAG, "cannot get asset list", e);
			return;
		}
		// unpack the assets to app_opt
		for (String asset : assetList) {
			if (asset.equals("images")
					|| asset.equals("sounds")
					|| asset.equals("webkit")
					|| asset.equals("databases")  // Motorola
					|| asset.equals("kioskmode")) // Samsung
				continue;
			Log.i(TAG, "copying asset: " + asset);
			showMessageInDialog("unpacking '" + asset + "'...", handler);
			copyFileOrDir(asset, app_opt);
		}

		chmod("0755", app_opt, true);
		writeVersionFile(context);
	}


	private static int readVersionFile() {
		if (! versionFile.exists()) return 0;
		Scanner in;
		int versionCode = 0;
		try {
			in = new Scanner(versionFile);
			versionCode = Integer.parseInt(in.next());
			in.close();
		} catch (Exception e) {
			log.append("Can't read app version file: " + e.getLocalizedMessage() + "\n");
		}
		return versionCode;
	}

	private static void writeVersionFile(Context context) {
		try {
			FileOutputStream fos = new FileOutputStream(versionFile);
			OutputStreamWriter out = new OutputStreamWriter(fos);
			out.write(String.valueOf(GpgApplication.VERSION_CODE) + "\n");
			out.close();
			fos.close();
		} catch (Exception e) {
			log.append("Can't write app version file: " + e.getLocalizedMessage() + "\n");
		}
	}

	public static boolean installOrUpgradeNeeded() {
		if (versionFile.exists() && GpgApplication.VERSION_CODE == readVersionFile())
			return false;
		else
			return true;
	}

	public static boolean installOrUpgradeAppOpt(Context context) {
		if (versionFile.exists()) {
			if (GpgApplication.VERSION_CODE > readVersionFile()) {
				Log.i(TAG, "Upgrading '" + app_opt + "'\n");
				// upgrade: rename current app_opt, then return true to trigger unpack
				renameOldAppOpt();
				return true;
			} else {
				Log.i(TAG, "Not upgrading '" + app_opt + "'\n");
			}
		} else {
			File[] list = app_opt.listFiles();
			if (list == null || list.length > 0) {
				Log.i(TAG, "Old, unversioned app_opt dir, upgrading.\n");
				renameOldAppOpt();
			} else {
				Log.i(TAG, "Fresh app_opt install.\n");
			}
			return true;
		}
		return false;
	}

	private static void renameOldAppOpt() {
		String moveTo = app_opt.toString();
		Calendar now = Calendar.getInstance();
		int version = readVersionFile();
		if (version == 0) {
			moveTo += ".old";
		} else {
			moveTo += ".build" + String.valueOf(version);
		}
		moveTo += "." + String.valueOf(now.getTimeInMillis());
		log.append("Moving '" + app_opt + "' to '" + moveTo + "'\n");
		app_opt.renameTo(new File(moveTo));
		app_opt.mkdir(); // Android normally creates this at onCreate()
	}

	public static void chmod(String modestr, File path) {
		Log.i(TAG, "chmod " + modestr + " " + path.getAbsolutePath());
		try {
			Class<?> fileUtils = Class.forName("android.os.FileUtils");
			Method setPermissions = fileUtils.getMethod("setPermissions", String.class,
					int.class, int.class, int.class);
			int mode = Integer.parseInt(modestr, 8);
			int a = (Integer) setPermissions.invoke(null, path.getAbsolutePath(), mode,
					-1, -1);
			if (a != 0) {
				Log.i(TAG, "ERROR: android.os.FileUtils.setPermissions() returned " + a
						+ " for '" + path + "'");
			}
		} catch (ClassNotFoundException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (IllegalAccessException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (InvocationTargetException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (NoSuchMethodException e) {
			Log.i(TAG, "android.os.FileUtils.setPermissions() failed:", e);
		}
	}

	public static void chmod(String mode, File path, boolean recursive) {
		chmod(mode, path);
		if (recursive) {
			File[] files = path.listFiles();
			for (File d : files) {
				if (d.isDirectory()) {
					Log.i(TAG, "chmod recurse: " + d.getAbsolutePath());
					chmod(mode, d, true);
				} else {
					chmod(mode, d);
				}
			}
		}
	}

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

	public static boolean isSdCardPresent() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public static int doShellCommand(String[] cmds, StringBuilder log, boolean runAsRoot,
			boolean waitFor) throws Exception {
		Log.i(TAG, "executing shell cmds: " + cmds[0] + "; runAsRoot=" + runAsRoot);

		Process proc = null;
		int exitCode = -1;

		if (runAsRoot)
			proc = Runtime.getRuntime().exec("su");
		else
			proc = Runtime.getRuntime().exec("sh");

		OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());

		for (int i = 0; i < cmds.length; i++) {
			out.write(cmds[i]);
			out.write("\n");
		}

		out.flush();
		out.write("exit\n");
		out.flush();

		if (waitFor) {

			final char buf[] = new char[10];

			// Consume the "stdout"
			InputStreamReader reader = new InputStreamReader(proc.getInputStream());
			int read = 0;
			while ((read = reader.read(buf)) != -1) {
				if (log != null)
					log.append(buf, 0, read);
			}

			// Consume the "stderr"
			reader = new InputStreamReader(proc.getErrorStream());
			read = 0;
			while ((read = reader.read(buf)) != -1) {
				if (log != null)
					log.append(buf, 0, read);
			}

			exitCode = proc.waitFor();
			log.append("process exit code: ");
			log.append(exitCode);
			log.append("\n");

			Log.i(TAG, "command process exit value: " + exitCode);
		}

		return exitCode;
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
