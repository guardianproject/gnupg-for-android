
package info.guardianproject.gpg;

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGKey;

public class SignFileActivity extends ActionBarActivity {
    private static final String TAG = "SignFileActivity";

    private FragmentManager mFragmentManager;
    private FileDialogFragment mFileDialog;
    private Messenger mMessenger;
    private String mSigningKeyFingerprint;
    private File mSignatureFile;
    private File mPlainFile;
    private String mDefaultFilename;

    // used to find any existing instance of the fragment, in case of rotation,
    static final String GPG2_TASK_FRAGMENT_TAG = TAG;

    private static final int SIGNATURE_DATA_SENT = 4321;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentManager = getSupportFragmentManager();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create a new Messenger for the communication back
        // Message is received after file is selected
        mMessenger = new Messenger(new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_CANCELED) {
                    cancel();
                } else if (message.what == FileDialogFragment.MESSAGE_OK) {
                    processFile(message);
                } else if (message.what == Gpg2TaskFragment.GPG2_TASK_FINISHED) {
                    sendSignedFile();
                }
            }
        });

        Intent intent = getIntent();
        Uri uri = intent.getData();
        Log.v(TAG, "onCreate: " + uri);
        String scheme = null;
        if (uri != null)
            scheme = uri.getScheme();
        if (scheme != null && scheme.equals("file") && new File(uri.getPath()).canRead())
            mDefaultFilename = uri.getPath();
        else
            mDefaultFilename = null;

        // set signing key to first/default secret key on device
        GnuPGKey key = null;
        GnuPGKey keys[] = GnuPG.context.listSecretKeys();
        if (keys != null && keys.length > 0) {
            key = keys[0];
        } else {
            showError(R.string.error_no_secret_key);
            return;
        }
        mSigningKeyFingerprint = key.getFingerprint();
        String text = getString(R.string.no_key_specified_using_this_key);
        text += String.format(" %s <%s> (%s) %s",
                key.getName(), key.getEmail(), key.getComment(),
                key.getFingerprint());
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();

        showSignFileDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode + " " + resultCode + " " + data);

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
            case SIGNATURE_DATA_SENT:
                Log.i(TAG, "SIGNATURE_DATA_SENT");
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
        if (mSignatureFile != null && mSignatureFile.getParentFile().equals(getFilesDir())) {
            Log.v(TAG, "Deleting " + mSignatureFile + " from the files cache");
            mSignatureFile.delete();
        }
    }

    /**
     * Show to dialog from where to import keys
     */
    private void showSignFileDialog() {
        new Runnable() {
            @Override
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(mMessenger,
                        getString(R.string.title_sign_file),
                        getString(R.string.dialog_specify_sign_file),
                        mDefaultFilename,
                        getString(R.string.ascii_signature),
                        GpgApplication.FILENAME);

                mFileDialog.show(mFragmentManager, "fileDialog");
            }
        }.run();
    }

    private void showError(int msgId) {
        showError(getString(msgId));
    }

    private void showError(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sign_file)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "showError setPositiveButton onClick");
                        setResult(RESULT_CANCELED);
                        cancel();
                    }
                });
        builder.show();
    }

    private void processFile(Message message) {
        Bundle data = message.getData();
        boolean asciiSignature = data.getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED);
        mPlainFile = new File(data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME));
        if (!mPlainFile.exists()) {
            String errorMsg = String.format(
                    getString(R.string.error_file_does_not_exist_format),
                    mPlainFile);
            showError(errorMsg);
        } else {
            try {
                Log.i(TAG, "processFile -----------------------------");
                Log.i(TAG, "key: " + mSigningKeyFingerprint);
                String plainFilename = mPlainFile.getCanonicalPath();
                Log.d(TAG, "plainFilename: " + plainFilename);
                String extension;
                if (asciiSignature)
                    extension = ".asc";
                else
                    extension = ".sig";
                if (mPlainFile.canWrite())
                    mSignatureFile = new File(plainFilename + extension);
                else
                    mSignatureFile = new File(getFilesDir(), mPlainFile.getName() + extension);
                String args = "--output '" + mSignatureFile + "'";
                args += " --default-key " + mSigningKeyFingerprint;
                if (asciiSignature)
                    args += " --armor ";
                args += " --detach-sign '" + plainFilename + "'";
                Gpg2TaskFragment gpg2Task = new Gpg2TaskFragment();
                gpg2Task.configTask(mMessenger, new Gpg2TaskFragment.Gpg2Task(), args);
                gpg2Task.show(mFragmentManager, GPG2_TASK_FRAGMENT_TAG);
            } catch (Exception e) {
                e.printStackTrace();
                String msg = String.format(
                        getString(R.string.error_signing_file_failed_format),
                        mPlainFile);
                showError(msg);
            }
        }
    }

    private void sendSignedFile() {
        Log.i(TAG, "sendSignedFile");
        if (mSignatureFile.exists()) {
            Posix.chmod("644", mSignatureFile);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.putExtra(Intent.EXTRA_SUBJECT, mSignatureFile.getName());
            Uri uri = Uri.fromFile(mSignatureFile);
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.setType(getString(R.string.pgp_signature));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent intent = Intent.createChooser(send,
                    getString(R.string.dialog_share_signature_file_using));
            startActivityForResult(intent, SIGNATURE_DATA_SENT);
        } else {
            String msg = String.format(getString(R.string.error_file_does_not_exist_format),
                    mSignatureFile);
            Log.i(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            cancel();
        }
    }
}
