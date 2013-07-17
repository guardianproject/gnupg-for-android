
package info.guardianproject.gpg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class FileHandlerActivity extends Activity {
    public static final String TAG = "FileHandlerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // TODO rename to IncomingContentHandlerActivity

        // Get intent, action and MIME type
        Intent intent = getIntent();
        Uri uri = intent.getData();
        Log.v(TAG, "onCreate: " + uri);
        String scheme = uri.getScheme();
        try {
            if (scheme.equals("file"))
                handleFileScheme(intent);
            else if (scheme.equals("content"))
                handleContentScheme(intent);
        } catch (Exception e) {
            e.printStackTrace();
            showError(R.string.app_name, e.getMessage());
        }
        finish();
    }

    private void handleFileScheme(Intent intent) throws IOException {
        String action = intent.getAction();
        String mimeType = intent.getType();
        Uri uri = intent.getData();
        File incomingFile = null;
        try {
            incomingFile = new File(uri.getPath());
            if (incomingFile == null || !incomingFile.exists())
                throw new Exception(incomingFile + " does not exist!");
        } catch (Exception e) {
            e.printStackTrace();
            String msg = String.format(getString(R.string.error_cannot_read_incoming_file_format),
                    incomingFile);
            showError(R.string.app_name, msg);
            return;
        }
        Log.i(TAG, "action: " + action + "   MIME Type: " + mimeType + "   data: " + incomingFile);

        final String incomingFilename = incomingFile.getAbsolutePath();
        final String extension = MimeTypeMap.getFileExtensionFromUrl(incomingFilename);
        if (incomingFile.canRead()) {
            if (extension.equals("gpg")) {
                String filename = incomingFile.getName();
                if (filename.equals("pubring.gpg") || filename.equals("secring.gpg"))
                    importFile(incomingFilename, getString(R.string.pgp_keys));
                else
                    decryptFile(incomingFilename, getString(R.string.pgp_encrypted));
            } else if (extension.equals("pkr") || extension.equals("skr") || extension.equals("pgp")) {
                importFile(incomingFilename, mimeType);
            } else if (extension.equals("asc")) {
                detectAsciiFileType(incomingFilename);
            } else if (extension.equals("sig")) {
                verifyFile(incomingFilename, getString(R.string.pgp_signature));
            } else {
                // this is a file type that gpg does not recognize, so encrypt and send
                encryptFile(incomingFilename);
            }
        } else {
            Toast.makeText(this, getString(R.string.error_cannot_read_incoming_file_format),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void handleContentScheme(Intent intent) throws IOException {
        String action = intent.getAction();
        String mimeType = intent.getType();
        Uri uri = intent.getData();
        Bundle extras = intent.getExtras();
        String incomingFilename = getContentName(getContentResolver(), uri);
        Log.i(TAG, "handleContentScheme: " + uri + "  MIME:" + mimeType + "  ACTION: " + action);
        Log.i(TAG, "incomingFilename: " + incomingFilename);
        // this was hacked together to support attachments to email programs
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null)
            throw new IOException("Cannot access mail attachment: " + uri);
        OutputStream out = new BufferedOutputStream(openFileOutput(incomingFilename, MODE_PRIVATE));
        byte[] buffer = new byte[8192];
        while (in.read(buffer) > 0)
            out.write(buffer);
        out.close();
        in.close();

        // now add the full path to the filename
        final File incomingFile = new File(getFilesDir(), incomingFilename);
        incomingFilename = incomingFile.getCanonicalPath();
        final String extension = MimeTypeMap.getFileExtensionFromUrl(incomingFilename);
        if (mimeType.equals(getString(R.string.pgp_keys))) {
            importFile(incomingFilename, mimeType);
        } else if (mimeType.equals(getString(R.string.pgp_signature))) {
            verifyFile(incomingFilename, mimeType);
        } else if (mimeType.equals(getString(R.string.pgp_encrypted))) {
            decryptFile(incomingFilename, mimeType);
        } else if (mimeType.equals("application/octet-stream")) {
            String filename = incomingFile.getName();
            if (filename.equals("pubring.gpg") || filename.equals("secring.gpg") || extension.equals("pgp"))
                importFile(incomingFilename, getString(R.string.pgp_keys));
            else
                decryptFile(incomingFilename, getString(R.string.pgp_encrypted));
            // TODO how else to detect binary crypto data? this could be:
            // - binary encrypted and/or signed data (application/pgp-encrypted)
            // - a binary detached signature (application/pgp-signature)
            // - a public/secret keyring (application/pgp-keys)
        } else if (mimeType.equals("text/plain")) {
            detectAsciiFileType(incomingFilename);
        } else {
            /*
             * TODO this is a file type that we don't handle, so assume the user
             * wants to do something to it, like sign and/or encrypt it
             */
            String msg = String.format(
                    getString(R.string.error_cannot_detect_file_type_format),
                    uri);
            showError(R.string.app_name, msg);
        }
    }

    private void showError(int resId, String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(resId)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        builder.show();
    }

    /* this works for standard apps, like Gmail. but not apps like K-9 */
    private String getContentName(ContentResolver resolver, Uri uri) {
        final String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME };
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        cursor.moveToFirst();
        // Gmail and K-9's attachment providers give the filename in this column
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex > -1) {
            return cursor.getString(nameIndex);
        } else {
            return null;
        }
    }

    private void detectAsciiFileType(String incomingFilename) throws IOException {
        // TODO this could be a .asc file, which can be many things:
        // - "--armor --export" keyrings
        // - "--armor --sign --encrypt" encrypted data
        // - "--armor --sign" signed data
        // - "--clearsign" signed message
        // - "--armor --detach-sign" signature
        Matcher matcher = null;
        String textData = FileUtils.readFileToString(new File(incomingFilename));
        if (textData == null || textData.length() == 0) {
            String msg = String.format(getString(R.string.error_cannot_read_incoming_file_format),
                    incomingFilename);
            showError(R.string.app_name, msg);
            return;
        }

        matcher = GnuPG.getPgpSignatureMatcher(textData);
        if (matcher.matches()) {
            Log.d(TAG, "PGP_SIGNATURE matched");
            verifyFile(incomingFilename, getString(R.string.pgp_signature));
            return;
        }
        matcher = GnuPG.getPgpPrivateKeyBlockMatcher(textData);
        if (matcher.matches()) {
            Log.d(TAG, "PGP_PRIVATE_KEY_BLOCK matched");
            importFile(incomingFilename, getString(R.string.pgp_keys));
            return;
        }
        matcher = GnuPG.getPgpPublicKeyBlockMatcher(textData);
        if (matcher.matches()) {
            Log.d(TAG, "PGP_PUBLIC_KEY_BLOCK matched");
            importFile(incomingFilename, getString(R.string.pgp_keys));
            return;
        }
        matcher = GnuPG.getPgpMessageMatcher(textData);
        if (matcher.matches()) {
            Log.d(TAG, "PGP_MESSAGE matched");
            decryptFile(incomingFilename, getString(R.string.pgp_encrypted));
            return;
        }
        matcher = GnuPG.getPgpSignedMessageMatcher(textData);
        if (matcher.matches()) {
            Log.d(TAG, "PGP_SIGNED_MESSAGE matched");
            decryptFile(incomingFilename, getString(R.string.pgp_encrypted));
            return;
        }
        String msg = String.format(getString(R.string.error_no_pgp_content_found_format),
                incomingFilename);
        showError(R.string.app_name, msg);
    }

    private void encryptFile(String incomingFilename) {
        Log.v(TAG, "encryptFile(" + incomingFilename + ")");
        Intent intent = new Intent(this, EncryptFileActivity.class);
        intent.setData(Uri.fromFile(new File(incomingFilename)));
        startActivity(intent);
    }

    private void decryptFile(String incomingFilename, String mimeType) {
        Log.v(TAG, "decryptFile(" + incomingFilename + ", " + mimeType + ")");
        Intent intent = new Intent(this, DecryptActivity.class);
        intent.setType(mimeType);
        intent.setData(Uri.fromFile(new File(incomingFilename)));
        startActivity(intent);
    }

    private void verifyFile(String incomingFilename, String mimeType) {
        Log.v(TAG, "verifyFile(" + incomingFilename + ", " + mimeType + ")");
        Intent intent = new Intent(this, VerifyActivity.class);
        intent.setType(mimeType);
        intent.setData(Uri.fromFile(new File(incomingFilename)));
        startActivity(intent);
    }

    private void importFile(String incomingFilename, String mimeType) {
        Log.v(TAG, "importFile(" + incomingFilename + ", " + mimeType + ")");
        Intent intent = new Intent(this, ImportFileActivity.class);
        intent.setType(mimeType);
        intent.setData(Uri.fromFile(new File(incomingFilename)));
        startActivity(intent);
    }
}
