
package info.guardianproject.gpg;

import android.util.Log;

import com.freiheit.gnupg.GnuPGContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GnuPG {
    public static GnuPGContext context = null;

    private static Pattern PGP_MESSAGE = null;
    private static Pattern PGP_PRIVATE_KEY_BLOCK = null;
    private static Pattern PGP_PUBLIC_KEY_BLOCK = null;
    private static Pattern PGP_SIGNATURE = null;
    private static Pattern PGP_SIGNED_MESSAGE = null;

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

    public static Matcher getPgpMessageMatcher(CharSequence input) {
        if (PGP_MESSAGE == null)
            PGP_MESSAGE = Pattern
                    .compile(
                            ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*",
                            Pattern.DOTALL);
        return PGP_MESSAGE.matcher(input);
    }

    public static Matcher getPgpSignatureMatcher(CharSequence input) {
        if (PGP_SIGNATURE == null)
            PGP_SIGNATURE = Pattern
                    .compile(
                            ".*?(-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                            Pattern.DOTALL);
        return PGP_SIGNATURE.matcher(input);
    }

    public static Matcher getPgpSignedMessageMatcher(CharSequence input) {
        if (PGP_SIGNED_MESSAGE == null)
            PGP_SIGNED_MESSAGE = Pattern
                    .compile(
                            ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                            Pattern.DOTALL);
        return PGP_SIGNED_MESSAGE.matcher(input);
    }

    public static Matcher getPgpPrivateKeyBlockMatcher(CharSequence input) {
        if (PGP_PRIVATE_KEY_BLOCK == null)
            PGP_PRIVATE_KEY_BLOCK = Pattern
                    .compile(
                            ".*?(-----BEGIN PGP PRIVATE KEY BLOCK-----.*?-----END PGP PRIVATE KEY BLOCK-----).*",
                            Pattern.DOTALL);
        return PGP_PRIVATE_KEY_BLOCK.matcher(input);
    }

    public static Matcher getPgpPublicKeyBlockMatcher(CharSequence input) {
        if (PGP_PUBLIC_KEY_BLOCK == null)
            PGP_PUBLIC_KEY_BLOCK = Pattern
                    .compile(
                            ".*?(-----BEGIN PGP PUBLIC KEY BLOCK-----.*?-----END PGP PUBLIC KEY BLOCK-----).*",
                            Pattern.DOTALL);
        return PGP_PUBLIC_KEY_BLOCK.matcher(input);
    }
}
