
package info.guardianproject.gpg;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGException;
import com.freiheit.gnupg.GnuPGKey;

public class ReceiveKeyActivity extends FragmentActivity {
    private static final String TAG = "ReceiveKeyActivity";

    private FragmentManager mFragmentManager;
    private FileDialogFragment mFileDialog;
    private Handler mReturnHandler;
    private Messenger mMessenger;

    // used to find any existing instance of the fragment, in case of rotation,
    static final String GPG2_TASK_FRAGMENT_TAG = TAG;

    @Override
    @SuppressLint("DefaultLocale")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        // scheme should only ever be pure ASCII:
        String scheme = intent.getScheme().toLowerCase(Locale.ENGLISH);
        Uri uri = intent.getData();

        mFragmentManager = getSupportFragmentManager();
        // Message is received after the fingerprints are selected
        mReturnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_CANCELED) {
                    cancel();
                } else if (message.what == FileDialogFragment.MESSAGE_OK) {
                    Bundle data = message.getData();
                    String fingerprints = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);
                    Log.d(TAG, "fingerprints: " + fingerprints);
                    runRecvKey(fingerprints, false);
                } else if (message.what == Gpg2TaskFragment.GPG2_TASK_FINISHED) {
                    notifyRecvKeyComplete();
                }
            }
        };
        // Create a new Messenger for the communication back
        mMessenger = new Messenger(mReturnHandler);

        if (uri == null) {
            finish();
            return;
        }
        if (scheme.equals("openpgp4fpr")) {
            String fingerprint = uri.toString().split(":")[1];
            GnuPGKey key = null;
            // if the fingerprint is too short, show a warning but prompt them
            // to download if they want
            if (fingerprint.length() < 16) {
                String msg = String.format(getString(R.string.error_fingerprint_too_short_format),
                        fingerprint);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            } else {
                try {
                    key = GnuPG.context.getKeyByFingerprint(fingerprint);
                } catch (GnuPGException e) {
                    e.printStackTrace(); // this gets thrown if the key doesn't
                                         // exist
                }
            }
            if (key == null)
                showReceiveKeyByFingerprintDialog(fingerprint);
            else {
                String msg = String.format(getString(R.string.key_already_exists_format),
                        fingerprint);
                msg += String.format(" %s <%s> (%s)",
                        key.getName(), key.getEmail(), key.getComment());
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                showKeyAfterRecvKey();
            }
        }
    }

    /**
     * Show dialog with the key fingerprints to receive from the keyservers
     */
    public void showReceiveKeyByFingerprintDialog(final String fingerprints) {

        new Runnable() {
            @Override
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(mMessenger,
                        getString(R.string.title_receive_keys),
                        getString(R.string.dialog_specific_keys_to_receive),
                        fingerprints,
                        null,
                        0);

                mFileDialog.show(mFragmentManager, "fileDialog");
            }
        }.run();
    }

    private void showKeyAfterRecvKey() {
        setResult(RESULT_OK);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void cancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void runRecvKey(String fingerprint, boolean ignored) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String ks = prefs.getString(GpgPreferenceActivity.PREF_KEYSERVER, "200.144.121.45");
            String args = " --keyserver " + ks + " --recv-key " + fingerprint;
            Gpg2TaskFragment gpg2Task = new Gpg2TaskFragment();
            gpg2Task.configTask(mMessenger, new Gpg2TaskFragment.Gpg2Task(), args);
            gpg2Task.show(mFragmentManager, GPG2_TASK_FRAGMENT_TAG);
            Log.d(TAG, "recv-key complete");
        } catch (GnuPGException e) {
            Log.e(TAG, "recv-key failed: " + fingerprint);
            e.printStackTrace();
        }
        setResult(RESULT_OK);
        finish();

    }

    private void notifyRecvKeyComplete() {
        Log.d(TAG, "recv-key complete, sending broadcast");
        GpgApplication.sendKeylistChangedBroadcast(this);
        showKeyAfterRecvKey();
    }
}
