
package info.guardianproject.gpg;

import java.io.File;

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
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGData;

public class DecryptActivity extends Activity {
    private static final String TAG = "DecryptActivity";

    private String mEncryptedFilename;
    private String mPlainFilename;

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

        mEncryptedFilename = uri.getPath();
        final String extension = MimeTypeMap.getFileExtensionFromUrl(mEncryptedFilename);
        if (extension.equals("asc") || extension.equals("gpg") || extension.equals("pgp")) {
            mPlainFilename = mEncryptedFilename.replaceAll("\\.(asc|gpg|pgp)$", "");
            if (new File(mEncryptedFilename).exists())
                new DecryptFileTask(this).execute(mEncryptedFilename);
            else {
                String msg = String.format(getString(R.string.error_file_does_not_exist_format),
                        mEncryptedFilename);
                showError(msg);
            }
        } else {
            showError(extension + " not handled yet: " + mEncryptedFilename);
        }
        // TODO verify content:// and streams by using GnuPG.context.verify()
    }

    private void decryptComplete(Integer result) {
        Log.d(TAG, "decrypt complete");
        setResult(result);
        if (result == RESULT_OK) {
            showSuccess();
        } else {
            String msg = String.format(getString(R.string.error_decrypting_file_failed_format),
                    mEncryptedFilename);
            showError(msg);
        }
    }

    private void showSuccess() {
        String msg = String.format(getString(R.string.dialog_decrypt_succeeded_view_file_format),
                mPlainFilename);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.signature_verified)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent view = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.fromFile(new File(mPlainFilename));
                        view.setData(uri);
                        startActivity(Intent.createChooser(view,
                                getString(R.string.dialog_view_file_using)));
                        finish();
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

    public class DecryptFileTask extends AsyncTask<String, String, Integer> {
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
        protected Integer doInBackground(String... params) {
            Log.i(TAG, "doInBackground: " + params[0]);
            String encryptedFilename = params[0];
            String msg = String.format(getString(R.string.decrypting_file_format),
                    encryptedFilename);
            publishProgress(msg);
            try {
                File encryptedFile = new File(mEncryptedFilename);
                GnuPGData encryptedData = GnuPG.context.createDataObject(encryptedFile);
                File plainFile = new File(mPlainFilename);
                GnuPGData plainData = GnuPG.context.createDataObject(plainFile);
                GnuPG.context.decryptVerify(encryptedData, plainData);
                return RESULT_OK;
            } catch (Exception e) {
                Log.e(TAG, "decrypting " + encryptedFilename + " failed!");
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
            if (dialog.isShowing())
                dialog.dismiss();
            decryptComplete(result);
        }
    }
}
