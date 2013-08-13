
package info.guardianproject.gpg;

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGKey;

public class EncryptFileActivity extends FragmentActivity {
    private static final String TAG = EncryptFileActivity.class.getSimpleName();

    private FragmentManager mFragmentManager;
    private FileDialogFragment mFileDialog;
    private Handler mReturnHandler;
    private Messenger mMessenger;
    private String mFingerprint;
    private String mEmail;
    private File mEncryptedFile;
    private File mPlainFile;

    // used to find any existing instance of the fragment, in case of rotation,
    static final String GPG2_TASK_FRAGMENT_TAG = TAG;

    private static final int ENCRYPTED_DATA_SENT = 4321;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentManager = getSupportFragmentManager();

        Intent intent = getIntent();
        Uri uri = intent.getData();
        Log.v(TAG, "onCreate: " + uri);
        String scheme = null;
        if (uri != null)
            scheme = uri.getScheme();
        Bundle extras = intent.getExtras();
        String[] recipients = null;
        if (extras != null) {
            mFingerprint = extras.getString(Intent.EXTRA_TEXT);
            // currently only supports sending to a single email/fingerprint
            recipients = (String[]) extras.get(Intent.EXTRA_EMAIL);
        }
        if (recipients != null && recipients.length > 0)
            mEmail = recipients[0];
        else
            Log.w(TAG, "receive no email address in Intent!");

        if (mFingerprint == null) {
            // didn't receive one in intent, set to default key on device
            GnuPGKey key = GnuPG.context.listSecretKeys()[0];
            if (key == null)
                key = GnuPG.context.listKeys()[0];
            if (key != null) {
                mFingerprint = key.getFingerprint();
                mEmail = key.getEmail();
                String text = getString(R.string.no_key_specified_using_this_key);
                text += String.format(" %s <%s> (%s) %s",
                        key.getName(), key.getEmail(), key.getComment(), key.getFingerprint());
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            }
        }

        if (mFingerprint == null || mFingerprint.length() < 16) {
            String msg = String.format(getString(R.string.error_fingerprint_too_short_format),
                    mFingerprint);
            Log.d(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            cancel();
            return;
        }

        // Message is received after file is selected
        mReturnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_CANCELED) {
                    cancel();
                } else if (message.what == FileDialogFragment.MESSAGE_OK) {
                    processFile(message);
                } else if (message.what == Gpg2TaskFragment.GPG2_TASK_FINISHED) {
                    sendEncryptedFile();
                }
            }
        };

        // Create a new Messenger for the communication back
        mMessenger = new Messenger(mReturnHandler);
        if (scheme != null && scheme.equals("file") && new File(uri.getPath()).canRead())
            showEncryptToFileDialog(uri.getPath());
        else
            showEncryptToFileDialog(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Activity Result: " + requestCode + " " + resultCode);

        switch (requestCode) {
            case GpgApplication.FILENAME: // file picker result returned
                Log.i(TAG, "GpgApplication.FILENAME " + GpgApplication.FILENAME);
                if (resultCode != RESULT_OK || data == null)
                    return;
                try {
                    String path = data.getData().getPath();
                    Log.d(TAG, "path=" + path);

                    // set filename used in export/import dialogs
                    mFileDialog.setFilename(path);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Nullpointer while retrieving path!", e);
                }
                return;
            case ENCRYPTED_DATA_SENT:
                Log.i(TAG, "ENCRYPTED_DATA_SENT");
                deleteFilesFromCache();
                setResult(RESULT_OK);
                finish();
                return;
        }
    }

    private void cancel() {
        deleteFilesFromCache();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void deleteFilesFromCache() {
        if (mPlainFile != null && mPlainFile.getParentFile().equals(getFilesDir())) {
            Log.v(TAG, "Deleting " + mPlainFile + " from the files cache");
            mPlainFile.delete();
        }
        if (mEncryptedFile != null && mEncryptedFile.getParentFile().equals(getFilesDir())) {
            Log.v(TAG, "Deleting " + mEncryptedFile + " from the files cache");
            mEncryptedFile.delete();
        }
    }

    /**
     * Show to dialog from where to import keys
     */
    public void showEncryptToFileDialog(final String defaultFilename) {
        new Runnable() {
            @Override
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(mMessenger,
                        getString(R.string.title_encrypt_file),
                        getString(R.string.dialog_specify_encrypt_file),
                        defaultFilename,
                        getString(R.string.sign_file),
                        GpgApplication.FILENAME);

                mFileDialog.show(mFragmentManager, "fileDialog");
            }
        }.run();
    }

    private void processFile(Message message) {
        Bundle data = message.getData();
        boolean signFile = data.getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED);
        mPlainFile = new File(data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME));
        if (!mPlainFile.exists()) {
            String errorMsg = String.format(
                    getString(R.string.error_file_does_not_exist_format),
                    mPlainFile);
            Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_LONG).show();
            cancel();
        } else {
            try {
                String plainFilename = mPlainFile.getCanonicalPath();
                Log.d(TAG, "plainFilename: " + plainFilename);
                mEncryptedFile = new File(plainFilename + ".gpg");
                String args = "--output '" + mEncryptedFile + "'";
                if (signFile)
                    args += " --sign ";
                args += " --encrypt --recipient " + mFingerprint + " '" + plainFilename + "'";
                Gpg2TaskFragment gpg2Task = new Gpg2TaskFragment();
                gpg2Task.configTask(mMessenger, new Gpg2TaskFragment.Gpg2Task(), args);
                gpg2Task.show(mFragmentManager, GPG2_TASK_FRAGMENT_TAG);
            } catch (Exception e) {
                String msg = String.format(
                        getString(R.string.error_encrypting_file_failed_format),
                        mPlainFile);
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "File encrypt failed: " + mPlainFile);
                e.printStackTrace();
                cancel();
            }
        }
    }

    private void sendEncryptedFile() {
        Log.i(TAG, "sendEncryptedFile");
        if (mEncryptedFile.exists()) {
            Posix.chmod("644", mEncryptedFile);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.putExtra(Intent.EXTRA_SUBJECT, mEncryptedFile.getName());
            if (mEmail != null && mEmail.length() > 3)
                send.putExtra(Intent.EXTRA_EMAIL, new String[] {
                        mEmail
                });
            Uri uri = Uri.fromFile(mEncryptedFile);
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.setType(getString(R.string.pgp_encrypted));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent intent = Intent.createChooser(send,
                    getString(R.string.dialog_share_file_using));
            startActivityForResult(intent, ENCRYPTED_DATA_SENT);
        } else {
            String msg = String.format(getString(R.string.error_file_does_not_exist_format),
                    mEncryptedFile);
            Log.i(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            cancel();
        }
    }
}
