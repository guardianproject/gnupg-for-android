
package info.guardianproject.gpg;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class DecryptActivity extends Activity {
    private static final String TAG = "DecryptActivity";

    private File mEncryptedFile;
    private File mPlainFile;

    private static int DECRYPTED_DATA_VIEWED = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String mimeType = intent.getType();
        String scheme = intent.getScheme();
        Uri uri = intent.getData();
        Log.i(TAG, "action: " + action + "   MIME Type: " + mimeType + "   uri: " + uri);
        if (uri == null) {
            String msg = String.format(getString(R.string.error_cannot_read_incoming_file_format),
                    "null");
            showError(msg);
            return;
        }

        if (!scheme.equals("file"))
            showError("Only file:// URIs are currently supported!");

        String encryptedFilename = uri.getPath();
        mEncryptedFile = new File(encryptedFilename);
        final String extension = FilenameUtils.getExtension(encryptedFilename);
        if (extension.equals("asc") || extension.equals("gpg") || extension.equals("pgp")
                || extension.equals("bin")) {
            mPlainFile = new File(getFilesDir(),
                    mEncryptedFile.getName().replaceAll("\\.(asc|bin|gpg|pgp)$", ""));
            if (mEncryptedFile.exists())
                new DecryptFileTask(this).execute();
            else {
                String msg = String.format(getString(R.string.error_file_does_not_exist_format),
                        mEncryptedFile);
                showError(msg);
            }
        } else {
            showError(extension + " not handled yet: " + mEncryptedFile);
        }
        // TODO verify content:// and streams by using GnuPG.context.verify()
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(" + requestCode + ", " + resultCode);
        if (requestCode == DECRYPTED_DATA_VIEWED) {
            Log.v(TAG, "Deleting " + mPlainFile + " from the files cache");
            mPlainFile.delete();
            if (mEncryptedFile.getParentFile().equals(getFilesDir())) {
                Log.v(TAG, "Deleting " + mEncryptedFile + " from the files cache");
                mEncryptedFile.delete();
            }
        }
        finish();
    }

    private void decryptComplete(Integer result) {
        Log.d(TAG, "decrypt complete");
        setResult(result);
        if (result == RESULT_OK) {
            showSuccess();
        } else {
            String msg = String.format(getString(R.string.error_decrypting_file_failed_format),
                    mEncryptedFile);
            showError(msg);
        }
    }

    private void showSuccess() {
        String msg = String.format(getString(R.string.dialog_decrypt_succeeded_view_file_format),
                mEncryptedFile);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.signature_verified)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent view = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.parse(PrivateFilesProvider.FILES_URI + mPlainFile.getName());
                        view.setData(uri);
                        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Intent intent = Intent.createChooser(view,
                                getString(R.string.dialog_view_file_using));
                        startActivityForResult(intent, DECRYPTED_DATA_VIEWED);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        builder.show();
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error_decrypting_failed)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        builder.show();
    }

    public class DecryptFileTask extends AsyncTask<Void, String, Integer> {
        private ProgressDialog dialog;
        private Context context;

        public DecryptFileTask(Context c) {
            context = c;
            dialog = new ProgressDialog(context);
            dialog.setIndeterminate(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setTitle(R.string.title_decrypt_file);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            Log.i(TAG, "doInBackground: " + mEncryptedFile);
            String msg = String.format(getString(R.string.decrypting_file_format), mEncryptedFile);
            publishProgress(msg);
            try {
                mPlainFile.delete();
                String args = "--output '" + mPlainFile + "' --decrypt '" + mEncryptedFile + "'";
                int exitvalue = GnuPG.gpg2(args);
                if (exitvalue != 0) {
                    // TODO does the POSIX exit value match the GPGME decrypt
                    // error codes?
                    Log.e(TAG, "gpg2 exited with " + exitvalue);
                }
                if (mPlainFile.exists())
                    return RESULT_OK;
            } catch (Exception e) {
                Log.e(TAG, "decrypting " + mEncryptedFile + " failed!");
                e.printStackTrace();
            }
            return RESULT_CANCELED;
        }

        @Override
        protected void onProgressUpdate(String... messages) {
            super.onProgressUpdate(messages);
            dialog.setMessage(messages[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            Log.i(TAG, "onPostExecute");
            // if the view changes too quickly, this seems to happen sometimes
            try {
                if (dialog.isShowing())
                    dialog.dismiss();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            decryptComplete(result);
        }
    }
}
