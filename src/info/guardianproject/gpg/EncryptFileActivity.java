
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentManager = getSupportFragmentManager();

        Bundle extras = getIntent().getExtras();
        mFingerprint = extras.getString(Intent.EXTRA_TEXT);
        // this currently only supports sending to a single email/fingerprint
        String[] recipients = (String[]) extras.get(Intent.EXTRA_EMAIL);
        if (recipients != null && recipients.length > 0)
            mEmail = recipients[0];
        else
            Log.w(TAG, "receive no email address in Intent!");

        if (mFingerprint == null || mFingerprint.length() < 16) {
            Log.e(TAG, "received bunk fingerprint: " + mFingerprint);
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
                    sendEncryptedFile("");
                }
            }
        };

        // Create a new Messenger for the communication back
        mMessenger = new Messenger(mReturnHandler);
        showEncryptToFileDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Activity Result: " + requestCode + " " + resultCode);
        if (resultCode == RESULT_CANCELED || data == null)
            return;

        switch (requestCode) {
            case GpgApplication.FILENAME: { // file picker result returned
                if (resultCode == RESULT_OK) {
                    try {
                        String path = data.getData().getPath();
                        Log.d(TAG, "path=" + path);

                        // set filename used in export/import dialogs
                        mFileDialog.setFilename(path);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Nullpointer while retrieving path!", e);
                    }
                }
                return;
            }
        }
    }

    private void cancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Show to dialog from where to import keys
     */
    public void showEncryptToFileDialog() {
        new Runnable() {
            @Override
            public void run() {
                String defaultFilename = null;
                mFileDialog = FileDialogFragment.newInstance(mMessenger,
                        getString(R.string.title_encrypt_file),
                        getString(R.string.dialog_specify_encrypt_file),
                        defaultFilename,
                        null, GpgApplication.FILENAME);

                mFileDialog.show(mFragmentManager, "fileDialog");
            }
        }.run();
    }

    private void processFile(Message message) {
        Bundle data = message.getData();
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
                String encryptedFilename = plainFilename + ".gpg";
                String args = "--output " + encryptedFilename
                        + " --encrypt --recipient " + mFingerprint
                        + " " + plainFilename;
                GnuPG.gpg2(args);
                Log.d(TAG, "encrypt complete");
                sendEncryptedFile(encryptedFilename);
            } catch (Exception e) {
                String msg = String.format(
                        getString(R.string.error_encrypting_file_failed_format),
                        plainFile);
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                Log.e(TAG, "File encrypt failed: ");
                e.printStackTrace();
                cancel();
            }
        }
    }

    private void sendEncryptedFile(String encryptedFilename) {
        File encryptedFile = new File(encryptedFilename);
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
