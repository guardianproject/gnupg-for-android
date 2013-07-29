
package info.guardianproject.gpg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
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

        Intent intent = getIntent();

        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            handleExtraText(intent);
            finish();
            return;
        }

        Uri uri = intent.getData();
        Log.v(TAG, "onCreate: " + uri);
        if (uri == null) {
            Bundle extras = intent.getExtras();
            if (extras == null) {
                String msg = String.format(
                        getString(R.string.error_cannot_read_incoming_file_format),
                        "null");
                showError(R.string.app_name, msg);
                return;
            }
            uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        Log.i(TAG, "uri: " + uri);
        if (uri == null) {
            showError(R.string.app_name, "Incoming URI is null!");
            return;
        }

        /*
         * handle receiving images from various galleries, it comes in as
         * content://, but you can't get a InputStream from them, so you have to
         * query for the MediaColumns.DATA column to get the file path. Picasa
         * gallery needs its own special hack, because it shows images that are
         * only online, but then doesn't treat them like normal files, and other
         * apps can't open them.
         */
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme.equals("content")) {
            if (host.equals("media")) {
                String path = getContentColumn(getContentResolver(), uri, MediaColumns.DATA);
                scheme = "file";
                uri = Uri.parse("file://" + path);
                Log.i(TAG, "constructed a path from a content://media/ URI: " + uri);
            } else if (host.equals("com.google.android.gallery3d.provider")
                    || host.equals("com.android.gallery3d.provider")) {
                String path = getContentColumn(getContentResolver(), uri, MediaColumns.DATA);
                scheme = "file";
                if (path == null || !(new File(path).canRead())) {
                    String msg = String.format(
                            getString(R.string.error_cannot_read_incoming_file_format),
                            uri);
                    showError(R.string.title_activity_encrypt, msg);
                    return;
                } else {
                    uri = Uri.parse("file://" + path);
                    Log.i(TAG, "constructed a path from online Picasa content:// URI: " + uri);
                }
            }
        }

        try {
            if (scheme.equals("file"))
                handleFileScheme(intent, uri);
            else if (scheme.equals("content"))
                handleContentScheme(intent, uri);
        } catch (Exception e) {
            e.printStackTrace();
            showError(R.string.app_name, e.getMessage());
        }
        finish();
    }

    private void handleExtraText(Intent intent) {
        try {
            String data = intent.getStringExtra(Intent.EXTRA_TEXT);
            String fingerprint = findFingerprint(data);
            if (fingerprint != null)
                receiveKeyByFingerprint(fingerprint);
            else {
                File file = File.createTempFile("extra_text", ".txt", getFilesDir());
                FileUtils.writeStringToFile(file, data);
                encryptFile(file.getCanonicalPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError(R.string.app_name, e.getMessage());
        }
    }

    private void handleFileScheme(Intent intent, Uri uri) throws IOException {
        String action = intent.getAction();
        String mimeType = intent.getType();
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
            if (extension.equals("gpg") || extension.equals("bin")) {
                String filename = incomingFile.getName();
                if (filename.equals("pubring.gpg") || filename.equals("secring.gpg"))
                    importFile(incomingFilename, getString(R.string.pgp_keys));
                else
                    decryptFile(incomingFilename, getString(R.string.pgp_encrypted));
            } else if (extension.equals("pkr") || extension.equals("skr")
                    || extension.equals("pgp") || extension.equals("key")) {
                importFile(incomingFilename, getString(R.string.pgp_keys));
            } else if (extension.equals("asc")) {
                // K-9 turns PGP/MIME into a file called "encrypted.asc"
                if (incomingFilename.equals("encrypted.asc"))
                    decryptFile(incomingFilename, getString(R.string.pgp_encrypted));
                else
                    detectAsciiFileType(incomingFilename);
            } else if (extension.equals("sig")) {
                verifyFile(incomingFilename, getString(R.string.pgp_signature));
            } else {
                /*
                 * This is a file type that gpg does not recognize or support,
                 * so it must be a file that is intended to be encrypted and
                 * sent
                 */
                encryptFile(incomingFilename);
            }
        } else {
            Toast.makeText(this, getString(R.string.error_cannot_read_incoming_file_format),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void handleContentScheme(Intent intent, Uri uri) throws IOException {
        String action = intent.getAction();
        String mimeType = intent.getType();
        String incomingFilename = getContentName(getContentResolver(), uri);
        // TODO get mimeType from incoming URI
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
            if (filename.equals("pubring.gpg") || filename.equals("secring.gpg")
                    || extension.equals("pgp"))
                importFile(incomingFilename, getString(R.string.pgp_keys));
            else
                decryptFile(incomingFilename, getString(R.string.pgp_encrypted));
            // TODO how else to detect binary crypto data? this could be:
            // - binary encrypted and/or signed data (application/pgp-encrypted)
            // - a binary detached signature (application/pgp-signature)
            // - a public/secret keyring (application/pgp-keys)
            // - K-9 turns PGP/MIME into noname.pgp file
        } else if (mimeType.equals("text/plain")) {
            if (incomingFilename.equals("encrypted.asc")) // K-9 turns PGP/MIME
                                                          // into this file
                decryptFile(incomingFilename, getString(R.string.pgp_encrypted));
            else
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
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
        builder.show();
    }

    private String findFingerprint(String text) {
        if (text.length() > 200) {
            Log.w(TAG, "Text too long, not looking for a fingerprint.");
            return null;
        }
        Pattern fpr = Pattern
                .compile(
                        ".*?([a-fA-F0-9: ]*[a-fA-F0-9]{4} {0,3}:?[a-fA-F0-9]{4} {0,3}:?[a-fA-F0-9]{4} {0,3}:?[a-fA-F0-9]{4} {0,3}:?).*",
                        Pattern.MULTILINE | Pattern.DOTALL);
        Matcher matcher = fpr.matcher(text);
        if (matcher.matches()) {
            Log.i(TAG, "matches: " + matcher.groupCount());
            // group(0) is entire match, group(1) is first match of parens
            String match = matcher.group(1);
            Log.i(TAG, "found: " + match);
            match = match.replaceAll("[ :]", "");
            if (match.length() > 40)
                match = match.substring(match.length() - 40);
            return match;
        }
        return null;
    }

    /* this works for standard apps, like Gmail. but not apps like K-9 */
    private String getContentName(ContentResolver resolver, Uri uri) {
        // Gmail and K-9's attachment providers give the filename in this column
        String displayName = getContentColumn(resolver, uri, MediaColumns.DISPLAY_NAME);
        String data = getContentColumn(resolver, uri, MediaColumns.DATA);
        String mimeType = getContentColumn(resolver, uri, MediaColumns.MIME_TYPE);
        Log.i(TAG, "DISPLAY_NAME: " + displayName + "   DATA: " + data + "  MIME_TYPE: " + mimeType);
        if (displayName != null)
            return displayName;
        if (data != null)
            return data;
        try {
            return File.createTempFile("incoming", "-tmp").getName();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "all-else-failed-lets-use-this-name-lol";
    }

    private String getContentColumn(ContentResolver resolver, Uri uri, String columnName) {
        try {
            Cursor cursor = resolver.query(uri, new String[] {
                    columnName
            }, null, null, null);
            if (cursor == null)
                return null;
            Log.i(TAG, uri.toString() + " columns: " + cursor.getColumnNames());
            cursor.moveToFirst();
            int nameIndex = cursor.getColumnIndex(columnName);
            if (nameIndex > -1) {
                Log.i(TAG,
                        "Column " + columnName + " (" + nameIndex + ") is '"
                                + cursor.getString(nameIndex) + "'");
                return cursor.getString(nameIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    private void receiveKeyByFingerprint(String fingerprint) {
        Log.v(TAG, "receiveKeyByFingerprint(" + fingerprint + ")");
        Intent intent = new Intent(this, ReceiveKeyActivity.class);
        intent.setData(Uri.parse("openpgp4fpr:" + fingerprint));
        startActivity(intent);
    }
}
