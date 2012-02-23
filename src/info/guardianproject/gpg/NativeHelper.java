package info.guardianproject.gpg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

public class NativeHelper {
	public static int STARTING_INSTALL = 12345;

	public static File app_opt; // an /opt tree for the UNIX cmd line tools
	public static File app_log; // a place to store logs
	public static File app_home; // dir for $HOME and ~/.gnupg
	public static String sdcard;
	private static Context context;

	public static void setup(Context c) {
		context = c;
		app_opt = context.getDir("opt", Context.MODE_WORLD_READABLE).getAbsoluteFile();
		app_log = new File(app_opt, "var/log");
		app_home = context.getDir("home", Context.MODE_PRIVATE).getAbsoluteFile();
		sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
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
	        Log.e(GnuPrivacyGuard.TAG, "I/O Exception", ex);
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
	        Log.e(GnuPrivacyGuard.TAG, filename + ": " + e.getMessage());
	    }

	}

	/*
	 * since we are using the whole gnupg package of programs and libraries, we
	 * are setting up the whole UNIX directory tree that it expects 
	 */
	private static void setupEmptyDirs() {
		new File(app_opt, "etc/gnupg").mkdirs();
		new File(app_opt, "var/run/gnupg").mkdirs();
		new File(app_opt, "var/cache/gnupg").mkdirs();
		// /home is outside of this tree, in app_home
	}

	public static void unpackAssets(Context context) {
		setupEmptyDirs();

		AssetManager am = context.getAssets();
		final String[] assetList;
		try {
			assetList = am.list("");
		} catch (IOException e) {
			Log.e(GnuPrivacyGuard.TAG, "cannot get asset list", e);
			return;
		}

		for (String asset : assetList) {
			if (asset.equals("images") || asset.equals("sounds")
					|| asset.equals("webkit"))
				continue;
			Log.i(GnuPrivacyGuard.TAG, "copying asset: " + asset);
			copyFileOrDir(asset, app_opt);
			if (asset.equals("bin") || asset.equals("libexec") || asset.equals("sbin") ) {
				File[] files = new File(app_opt, asset).listFiles();
				for (File f : files) {
					chmod(0755, f);
				}
			}
		}
	}

	public static void chmod(int mode, File path) {
		Log.i(GnuPrivacyGuard.TAG, "chmod " + Integer.toOctalString(mode) + " "  + path.getAbsolutePath());
		try {
			Class<?> fileUtils = Class.forName("android.os.FileUtils");
			Method setPermissions = fileUtils.getMethod("setPermissions", String.class,
					int.class, int.class, int.class);
			int a = (Integer) setPermissions.invoke(null, path.getAbsolutePath(), mode,
					-1, -1);
			if (a != 0) {
				Log.i(GnuPrivacyGuard.TAG, "ERROR: android.os.FileUtils.setPermissions() returned " + a
						+ " for '" + path + "'");
			}
		} catch (ClassNotFoundException e) {
			Log.i(GnuPrivacyGuard.TAG, "android.os.FileUtils.setPermissions() failed - ClassNotFoundException.");
		} catch (IllegalAccessException e) {
			Log.i(GnuPrivacyGuard.TAG, "android.os.FileUtils.setPermissions() failed - IllegalAccessException.");
		} catch (InvocationTargetException e) {
			Log.i(GnuPrivacyGuard.TAG, "android.os.FileUtils.setPermissions() failed - InvocationTargetException.");
		} catch (NoSuchMethodException e) {
			Log.i(GnuPrivacyGuard.TAG, "android.os.FileUtils.setPermissions() failed - NoSuchMethodException.");
		}
	}

	public static boolean isSdCardPresent() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public static int doShellCommand(String[] cmds, StringBuilder log, boolean runAsRoot, boolean waitFor) throws Exception
	{
		Log.i(GnuPrivacyGuard.TAG, "executing shell cmds: " + cmds[0] + "; runAsRoot=" + runAsRoot);
		
		 	
		Process proc = null;
		int exitCode = -1;
		
            
        	if (runAsRoot)
        		proc = Runtime.getRuntime().exec("su");
        	else
        		proc = Runtime.getRuntime().exec("sh");
        	
        	
        	OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());
            
            for (int i = 0; i < cmds.length; i++)
            {
            	out.write(cmds[i]);
            	out.write("\n");
            }
            
            out.flush();
			out.write("exit\n");
			out.flush();
		
			if (waitFor)
			{
				
				final char buf[] = new char[10];
				
				// Consume the "stdout"
				InputStreamReader reader = new InputStreamReader(proc.getInputStream());
				int read=0;
				while ((read=reader.read(buf)) != -1) {
					if (log != null) log.append(buf, 0, read);
				}
				
				// Consume the "stderr"
				reader = new InputStreamReader(proc.getErrorStream());
				read=0;
				while ((read=reader.read(buf)) != -1) {
					if (log != null) log.append(buf, 0, read);
				}
				
				exitCode = proc.waitFor();
				log.append("process exit code: ");
				log.append(exitCode);
				log.append("\n");
				
				Log.i(GnuPrivacyGuard.TAG, "command process exit value: " + exitCode);
			}
        
        
        return exitCode;

	}

}
