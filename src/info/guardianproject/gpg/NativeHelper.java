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
	public static File app_log;
	public static String sdcard;

	public static void setup(Context context) {
		app_opt = context.getDir("opt", Context.MODE_WORLD_READABLE).getAbsoluteFile();
		app_log = context.getDir("log", Context.MODE_PRIVATE).getAbsoluteFile();
		sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	public static void unzipFiles(Context context) {
		try {
			AssetManager am = context.getAssets();
			final String[] assetList = am.list("");

			for (String asset : assetList) {
				if (asset.equals("images") || asset.equals("sounds")
						|| asset.equals("webkit"))
					continue;

				int BUFFER = 2048;
				Log.i(GnuPrivacyGuard.TAG, "opening asset: " + asset);
				final File file = new File(NativeHelper.app_opt, asset);
				final InputStream assetIS = am.open(asset);

				if (file.exists()) {
					file.delete();
					Log.i(GnuPrivacyGuard.TAG, "NativeHelper.unzipFiles() deleting "
							+ file.getAbsolutePath());
				}

				FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

				int count;
				byte[] data = new byte[BUFFER];

				while ((count = assetIS.read(data, 0, BUFFER)) != -1) {
					// System.out.write(x);
					dest.write(data, 0, count);
				}

				dest.flush();
				dest.close();

				assetIS.close();
			}
		} catch (IOException e) {
			Log.e(GnuPrivacyGuard.TAG, "Can't unzip", e);
		}
		chmod(0755, new File(app_opt, "dirmngr"));
		chmod(0755, new File(app_opt, "dirmngr-client"));
		chmod(0755, new File(app_opt, "gpg2"));
		chmod(0755, new File(app_opt, "gpg-agent"));
	}

	public static void chmod(int mode, File path) {
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
