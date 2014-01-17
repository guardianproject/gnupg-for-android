
package info.guardianproject.gpg;

import info.guardianproject.gpg.GpgApplication.Action;

import java.io.File;

import org.openintents.openpgp.keyserver.KeyServer.KeyInfo;

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

public class EncryptFileActivity extends ActionBarActivity {
    private static final String TAG = EncryptFileActivity.class.getSimpleName();

    private FragmentManager mFragmentManager;
    private FileDialogFragment mFileDialog;
    private Messenger mMessenger;
    private long[] mRecipientKeyIds;
    private String[] mRecipientEmails;
    private File mEncryptedFile;
    private File mPlainFile;
    private String mDefaultFilename;

    // used to find any existing instance of the fragment, in case of rotation,
    static final String GPG2_TASK_FRAGMENT_TAG = TAG;

    private static final int ENCRYPTED_DATA_SENT = 4321;
    private static final int ENCRYPT_FILE_TO = 0x1231;

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
                    sendEncryptedFile();
                }
            }
        });

        Intent intent = getIntent();
        Uri uri = intent.getData();
        String scheme = null;
        if (uri != null)
            scheme = uri.getScheme();
        if (scheme != null && scheme.equals("file") && new File(uri.getPath()).canRead())
            mDefaultFilename = uri.getPath();
        else
            mDefaultFilename = null;

        Bundle extras = intent.getExtras();
        if (extras != null) {
            mRecipientKeyIds = extras.getLongArray(Intent.EXTRA_UID);
            mRecipientEmails = extras.getStringArray(Intent.EXTRA_EMAIL);
        }

        if (mRecipientKeyIds == null || mRecipientKeyIds.length == 0) {
            Intent i = new Intent(this, SelectKeysActivity.class);
            i.setAction(Action.SELECT_PUBLIC_KEYS);
            startActivityForResult(i, ENCRYPT_FILE_TO);
        } else {
            showEncryptToFileDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + requestCode + " " + resultCode + " " + data);

        switch (requestCode) {
            case ENCRYPT_FILE_TO:
                Bundle extras = data.getExtras();
                if (extras != null) {
                    mRecipientKeyIds = extras.getLongArray(Intent.EXTRA_UID);
                    mRecipientEmails = extras.getStringArray(Intent.EXTRA_EMAIL);
                }
                if (mRecipientKeyIds == null || mRecipientKeyIds.length == 0) {
                    // didn't receive recipients, set to default key on device
                    GnuPGKey key = null;
                    GnuPGKey keys[] = GnuPG.context.listSecretKeys();
                    if (keys != null && keys.length > 0)
                        key = keys[0];
                    if (key == null) {
                        keys = GnuPG.context.listKeys();
                        if (keys != null && keys.length > 0)
                            key = keys[0];
                    }
                    if (key != null) {
                        mRecipientKeyIds = new long[] {
                                KeyInfo.keyIdFromFingerprint(key.getFingerprint())
                        };
                        mRecipientEmails = new String[] {
                                key.getEmail()
                        };
                        String text = getString(R.string.no_key_specified_using_this_key);
                        text += String.format(" %s <%s> (%s) %s",
                                key.getName(), key.getEmail(), key.getComment(),
                                key.getFingerprint());
                        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                    }
                }
                showEncryptToFileDialog();
                return;
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

    private void showError(String msg) {
        Log.i(TAG, msg);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.encrypt_file_to)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "showError setPositiveButton onClick");
                        cancel();
                    }
                });
        builder.show();
    }

    /**
     * Show to dialog from where to import keys
     */
    private void showEncryptToFileDialog() {
        new Runnable() {
            @Override
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(mMessenger,
                        getString(R.string.title_encrypt_file),
                        getString(R.string.dialog_specify_encrypt_file),
                        mDefaultFilename,
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
            String msg = String.format(
                    getString(R.string.error_file_does_not_exist_format),
                    mPlainFile);
            showError(msg);
        } else {
            try {
                String plainFilename = mPlainFile.getCanonicalPath();
                Log.d(TAG, "plainFilename: " + plainFilename);
                mEncryptedFile = new File(plainFilename + ".gpg");
                String args = "--output '" + mEncryptedFile + "'";
                if (signFile)
                    args += " --sign ";
                args += " --encrypt ";
                for (long keyId : mRecipientKeyIds)
                    args += " --recipient " + KeyInfo.hexFromKeyId(keyId);
                args += " '" + plainFilename + "'";
                Gpg2TaskFragment gpg2Task = new Gpg2TaskFragment();
                gpg2Task.configTask(mMessenger, new Gpg2TaskFragment.Gpg2Task(), args);
                gpg2Task.show(mFragmentManager, GPG2_TASK_FRAGMENT_TAG);
            } catch (Exception e) {
                e.printStackTrace();
                String msg = String.format(
                        getString(R.string.error_encrypting_file_failed_format),
                        mPlainFile);
                showError(msg);
            }
        }
    }

    private void sendEncryptedFile() {
        Log.i(TAG, "sendEncryptedFile");
        if (mEncryptedFile.exists()) {
            Posix.chmod("644", mEncryptedFile);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.putExtra(Intent.EXTRA_SUBJECT, mEncryptedFile.getName());
            send.putExtra(Intent.EXTRA_EMAIL, mRecipientEmails);
            Uri uri = Uri.fromFile(mEncryptedFile);
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.setType(getString(R.string.pgp_encrypted));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent intent = Intent.createChooser(send,
                    getString(R.string.dialog_share_encrypted_file_using));
            startActivityForResult(intent, ENCRYPTED_DATA_SENT);
        } else {
            String msg = String.format(getString(R.string.error_file_does_not_exist_format),
                    mEncryptedFile);
            showError(msg);
        }
    }
}
