
package info.guardianproject.gpg;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ImportFileActivity extends FragmentActivity {
    private static final String TAG = ImportFileActivity.class.getSimpleName();

    private FileDialogFragment mFileDialog;

    final String[] filetypes = {
            ".gpg", ".asc"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else {
                handleSendBinary(intent); // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            handleSendMultipleBinaries(intent);
        } else {
            // handle the basic case
            showImportFromFileDialog(new String());
        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Activity Result: " + requestCode + " " + resultCode);
        if (resultCode == RESULT_CANCELED || data == null) return;

        switch( requestCode ) {
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

    private boolean isSupportedFileType(Uri uri) {
        String path = uri.getLastPathSegment();
        String extension = path.substring(path.lastIndexOf('.'), path.length()).toLowerCase();
        for (String filetype : filetypes)
            if (extension.equals(filetype))
                return true;
        return false;
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            Toast.makeText(this, "handle send text", Toast.LENGTH_LONG).show();
        }
    }

    void handleSendBinary(Intent intent) {
        Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (isSupportedFileType(uri)) {
            Toast.makeText(this, "handle send binary: " + uri,
                    Toast.LENGTH_LONG).show();
            Log.v(TAG, "handle send binary: " + uri);
        }
    }

    void handleSendMultipleBinaries(Intent intent) {
        ArrayList<Uri> uris = intent
                .getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        for (Uri uri : uris)
            if (isSupportedFileType(uri)) {
                Toast.makeText(this, "handle multiple binaries: " + uri, Toast.LENGTH_LONG)
                        .show();
                Log.v(TAG, "handle multiple binaries: " + uri);
            }
    }

    /**
     * Show to dialog from where to import keys
     */
    public void showImportFromFileDialog(final String defaultFilename) {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_CANCELED) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                else if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    String importFilename = new File(
                            data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME))
                            .getAbsolutePath();
                    boolean deleteAfterImport = data
                            .getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED);

                    Log.d(TAG, "importFilename: " + importFilename);
                    Log.d(TAG, "deleteAfterImport: " + deleteAfterImport);

                    File keyFile = new File(importFilename);
                    try {
                        GnuPG.context.importKey(keyFile);
                        if (deleteAfterImport)
                            new File(importFilename).delete();
                        Log.d(TAG, "import complete");
                    } catch (GnuPGException e) {
                        Log.e(TAG, "File import failed: ");
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.e(TAG, "File import failed: ");
                        e.printStackTrace();
                    }
                    setResult(RESULT_OK);
                    notifyImportComplete();
                    finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        new Runnable() {
            @Override
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(messenger,
                        getString(R.string.title_import_keys),
                        getString(R.string.dialog_specify_import_file_msg), defaultFilename,
                        null, GpgApplication.FILENAME);

                mFileDialog.show(getSupportFragmentManager(), "fileDialog");
            }
        }.run();
    }

    private void notifyImportComplete() {
        Log.d(TAG, "import complete, sending broadcast");
        LocalBroadcastManager.getInstance(this).sendBroadcast( new Intent(KeyListFragment.BROADCAST_ACTION_KEYLIST_CHANGED));
    }

}
