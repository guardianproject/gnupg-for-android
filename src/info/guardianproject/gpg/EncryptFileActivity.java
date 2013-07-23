
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

public class EncryptFileActivity extends FragmentActivity {
    private static final String TAG = EncryptFileActivity.class.getSimpleName();

    private FragmentManager mFragmentManager;
    private FileDialogFragment mFileDialog;
    private Handler mReturnHandler;
    private Messenger mMessenger;
    private String mFingerprint;
    private String mEmail;
    private String mEncryptedFilename;

    // used to find any existing instance of the fragment, in case of rotation,
    static final String GPG2_TASK_FRAGMENT_TAG = TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentManager = getSupportFragmentManager();

        Intent intent = getIntent();
        Uri uri = intent.getData();
        Log.v(TAG, "onCreate: " + uri);
        String scheme = uri.getScheme();
        Bundle extras = intent.getExtras();
        String[] recipients = null;
        if (extras != null) {
            mFingerprint = extras.getString(Intent.EXTRA_TEXT);
            // this currently only supports sending to a single email/fingerprint
            recipients = (String[]) extras.get(Intent.EXTRA_EMAIL);
        }
        if (recipients != null && recipients.length > 0)
            mEmail = recipients[0];
        else
            Log.w(TAG, "receive no email address in Intent!");

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
        if (scheme.equals("file") && new File(uri.getPath()).canRead())
            showEncryptToFileDialog(uri.getPath());
        else
            showEncryptToFileDialog(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Activity Result: " + requestCode + " " + resultCode);
        if (resultCode != RESULT_OK || data == null)
            return;

        switch (requestCode) {
            case GpgApplication.FILENAME: // file picker result returned
                try {
                    String path = data.getData().getPath();
                    Log.d(TAG, "path=" + path);

                    // set filename used in export/import dialogs
                    mFileDialog.setFilename(path);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Nullpointer while retrieving path!", e);
                }
                return;
        }
    }

    private void cancel() {
        setResult(RESULT_CANCELED);
        finish();
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
        File plainFile = new File(
                data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME));
        if (!plainFile.exists()) {
            String errorMsg = String.format(
                    getString(R.string.error_file_does_not_exist_format),
                    plainFile);
            Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_LONG).show();
            cancel();
        } else {
            try {
                String plainFilename = plainFile.getCanonicalPath();
                Log.d(TAG, "plainFilename: " + plainFilename);
                mEncryptedFilename = plainFilename + ".gpg";
                String args = "--output " + mEncryptedFilename;
                if (signFile)
                    args += " --sign ";
                args += " --encrypt --recipient " + mFingerprint + " " + plainFilename;
                Gpg2TaskFragment gpg2Task = new Gpg2TaskFragment();
                gpg2Task.configTask(mMessenger, new Gpg2TaskFragment.Gpg2Task(), args);
                gpg2Task.show(mFragmentManager, GPG2_TASK_FRAGMENT_TAG);
            } catch (Exception e) {
                String msg = String.format(
                        getString(R.string.error_encrypting_file_failed_format),
                        plainFile);
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "File encrypt failed: " + plainFile);
                e.printStackTrace();
                cancel();
            }
        }
    }

    private void sendEncryptedFile() {
        Log.i(TAG, "sendEncryptedFile");
        File encryptedFile = new File(mEncryptedFilename);
        if (mEmail != null && mEmail.length() > 3 && encryptedFile.exists()) {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.putExtra(Intent.EXTRA_SUBJECT, encryptedFile.getName());
            send.putExtra(Intent.EXTRA_EMAIL, new String[] {
                    mEmail
            });
            send.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(encryptedFile));
            send.setType("application/octet-stream");
            startActivity(Intent.createChooser(send,
                    getString(R.string.dialog_share_file_using)));
        }
        setResult(RESULT_OK);
        finish();
    }
}
