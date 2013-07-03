
package info.guardianproject.gpg;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class EncryptFileActivity extends FragmentActivity {
    private static final String TAG = EncryptFileActivity.class.getSimpleName();

    private FileDialogFragment mFileDialog;
    private String mFingerprint;
    private String mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        mFingerprint = extras.getString(Intent.EXTRA_TEXT);
        // this currently only supports sending to a single email/fingerprint
        String[] recipients = (String[]) extras.get(Intent.EXTRA_EMAIL);
        if (recipients != null && recipients.length > 0)
            mEmail = recipients[0];
        else
            Log.w(TAG, "receive no email address in Intent!");

        if (mFingerprint != null && mFingerprint.length() > 15) {
            showEncryptToFileDialog();
            // finish(); is called in the above method
        } else {
            Log.e(TAG, "received bunk fingerprint: " + mFingerprint);
            finish();
        }
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

    /**
     * Show to dialog from where to import keys
     */
    public void showEncryptToFileDialog() {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_CANCELED) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                else if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    try {
                        Bundle data = message.getData();
                        File plainFile = new File(
                                data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME));
                        String plainFilename = plainFile.getCanonicalPath();
                        Log.d(TAG, "plainFilename: " + plainFilename);
                        String encryptedFilename = plainFilename + ".gpg";
                        String args = "--output " + encryptedFilename
                                + " --encrypt --recipient " + mFingerprint
                                + " " + plainFilename;
                        GnuPG.gpg2(args);
                        Log.d(TAG, "encrypt complete");
                    } catch (Exception e) {
                        Log.e(TAG, "File encrypt failed: ");
                        e.printStackTrace();
                    }
                    setResult(RESULT_OK);
                    finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        new Runnable() {
            @Override
            public void run() {
                String defaultFilename = null;
                mFileDialog = FileDialogFragment.newInstance(messenger,
                        getString(R.string.title_encrypt_file),
                        getString(R.string.dialog_specify_encrypt_file),
                        defaultFilename,
                        null, GpgApplication.FILENAME);

                mFileDialog.show(getSupportFragmentManager(), "fileDialog");
            }
        }.run();
    }

}
