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
import java.util.StringTokenizer;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

public class NativeHelper implements Constants {
	public static final String TAG = "NativeHelper";

	public static File app_opt; // an /opt tree for the UNIX cmd line tools
	public static File app_log; // a place to store logs
	public static File app_home; // dir for $HOME and ~/.gnupg

	// full paths to key executables, with globally used flags
	public static String gpg2;
	public static String gpg_agent;
	public static String dirmngr;

	public static String sdcard;
	public static String[] envp; // environment variables

	private static Context context;

	public static void setup(Context c) {
		context = c;
		app_opt = context.getDir("opt", Context.MODE_WORLD_READABLE).getAbsoluteFile();
		app_log = context.getDir("log", Context.MODE_PRIVATE).getAbsoluteFile();
		app_home = context.getDir("home", Context.MODE_PRIVATE).getAbsoluteFile();
		
		File bin = new File(app_opt, "bin");
		String logging = "--debug-level advanced --log-file " + NativeHelper.app_log
				+ "/gpg2.log ";
		gpg2 = new File(bin, "gpg2").getAbsolutePath() + " --no-tty " + logging;
		gpg_agent = new File(bin, "gpg-agent").getAbsolutePath();
		dirmngr = new File(bin, "dirmngr").getAbsolutePath();

		sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
		envp = new String[] { "HOME=" + NativeHelper.app_home,
				"LD_LIBRARY_PATH=$LD_LIBRARY_PATH:" + NativeHelper.app_opt + "/lib",
				"PATH=$PATH:" + bin.getAbsolutePath(),
				"app_opt=" + app_opt.getAbsolutePath() };
		Log.i(TAG, "Finished NativeHelper.setup()");
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
			Log.e(LOG_NH, "I/O Exception", ex);
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
			Log.e(LOG_NH, filename + ": " + e.getMessage());
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
			Log.e(LOG_NH, "Cannot write file: ", e);
		}
	}

	public static void unpackAssets(Context context) {
		Log.i(TAG, "Setting up assets in " + app_opt);
		setupEmptyDirs();
		writeShProfile();

		AssetManager am = context.getAssets();
		final String[] assetList;
		try {
			assetList = am.list("");
		} catch (IOException e) {
			Log.e(LOG_NH, "cannot get asset list", e);
			return;
		}
		// unpack the assets to app_opt
		for (String asset : assetList) {
			if (asset.equals("images") || asset.equals("sounds")
					|| asset.equals("webkit"))
				continue;
			Log.i(LOG_NH, "copying asset: " + asset);
			copyFileOrDir(asset, app_opt);
		}

		chmod("0755", app_opt, true);
	}

	public static void chmod(String modestr, File path) {
		Log.i(LOG_NH, "chmod " + modestr + " " + path.getAbsolutePath());
		try {
			Class<?> fileUtils = Class.forName("android.os.FileUtils");
			Method setPermissions = fileUtils.getMethod("setPermissions", String.class,
					int.class, int.class, int.class);
			int mode = Integer.parseInt(modestr, 8);
			int a = (Integer) setPermissions.invoke(null, path.getAbsolutePath(), mode,
					-1, -1);
			if (a != 0) {
				Log.i(LOG_NH, "ERROR: android.os.FileUtils.setPermissions() returned " + a
						+ " for '" + path + "'");
			}
		} catch (ClassNotFoundException e) {
			Log.i(LOG_NH, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (IllegalAccessException e) {
			Log.i(LOG_NH, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (InvocationTargetException e) {
			Log.i(LOG_NH, "android.os.FileUtils.setPermissions() failed:", e);
		} catch (NoSuchMethodException e) {
			Log.i(LOG_NH, "android.os.FileUtils.setPermissions() failed:", e);
		}
	}

	public static void chmod(String mode, File path, boolean recursive) {
		chmod(mode, path);
		if (recursive) {
			File[] files = path.listFiles();
			for (File d : files) {
				if (d.isDirectory()) {
					Log.i(LOG_NH, "chmod recurse: " + d.getAbsolutePath());
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
			Log.w(LOG_NH, "No running process found for " + command);
			return;
		}
		Log.d(LOG_NH, "killing " + command + " at " + pid.toString());
		try {
			Runtime.getRuntime().exec("kill " + pid.toString()).waitFor();
			Thread.sleep(1000);
			Runtime.getRuntime().exec("kill -9 " + pid.toString()).waitFor();
		} catch (Exception e) {
			Log.e(LOG_NH, "Unable to kill " + command + " at pid " + pid.toString(), e);
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
				Log.w(LOG_NH, "Unable to get proc id for: " + command, e2);
			}
		}
		return pid;
	}

	public static boolean isSdCardPresent() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public static int doShellCommand(String[] cmds, StringBuilder log, boolean runAsRoot,
			boolean waitFor) throws Exception {
		Log.i(LOG_NH, "executing shell cmds: " + cmds[0] + "; runAsRoot=" + runAsRoot);

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
			try {
				// this line should just be the process id
				pid = Integer.parseInt(line.trim());
				break;
			} catch (NumberFormatException e) {
				Log.i(LOG_NH, "unable to parse process pid: " + line, e);
			}
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
