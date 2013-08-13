
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

public class VerifyActivity extends Activity {
    private static final String TAG = "VerifyActivity";

    private String mSignatureFilename;
    private String mExtension;
    private String mSignedFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String mimeType = intent.getType();
        Uri uri = intent.getData();
        Log.i(TAG, "action: " + action + "   MIME Type: " + mimeType + "   uri: " + uri);

        mSignatureFilename = uri.getPath();
        mExtension = MimeTypeMap.getFileExtensionFromUrl(mSignatureFilename);
        /*
         * though a .asc file could contain data, we currently assume its a
         * detached sig from "gpg2 --armor --detach-sign". that will need to
         * change since its possible to include data in an .asc, like from
         * "gpg2 --clearsign" or maybe even "gpg2 --sign"
         */
        if (mExtension.equals("asc") || mExtension.equals("sig")) {
            mSignedFilename = mSignatureFilename.replaceAll("\\.(asc|sig)$", "");
            if (new File(mSignedFilename).exists())
                new VerifyFileTask(this).execute(mSignatureFilename);
            else {
                String msg = String.format(getString(R.string.error_file_does_not_exist_format),
                        mSignatureFilename);
                showError(msg);
            }
        } else {
            Log.d(TAG, uri + " not handled yet");
        }
        // TODO verify content:// and streams by using GnuPG.context.verify()
    }

    private void verifyComplete(Integer result) {
        Log.d(TAG, "verify complete");
        setResult(result);
        if (result == RESULT_OK) {
            showSuccess();
        } else {
            String msg = String.format(getString(R.string.error_file_verify_failed_format),
                    mSignatureFilename);
            showError(msg);
        }
    }

    private void showSuccess() {
        String msg = String.format(getString(R.string.dialog_verify_succeeded_view_file_format),
                mSignatureFilename);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.signature_verified)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent view = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.fromFile(new File(mSignedFilename));
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
        builder.setTitle(R.string.error_verify_failed)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        builder.show();
    }

    public class VerifyFileTask extends AsyncTask<String, String, Integer> {
        private ProgressDialog dialog;
        private Context context;

        public VerifyFileTask(Context c) {
            context = c;
            dialog = new ProgressDialog(context);
            dialog.setIndeterminate(true);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setTitle(R.string.verify_signature);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected Integer doInBackground(String... params) {
            Log.i(TAG, "doInBackground: " + params[0]);
            String signatureFilename = params[0];
            String msg = String.format(getString(R.string.verifying_file_format),
                    signatureFilename);
            publishProgress(msg);
            try {
                String args = "--verify '" + signatureFilename + "'";
                // check the POSIX exit value to see if it verified properly
                int exitvalue = GnuPG.gpg2(args);
                if (exitvalue == 0) {
                    return RESULT_OK;
                } else {
                    // TODO does the POSIX exit value match the GPGME verify
                    // error codes?
                    Log.e(TAG, "gpg2 exited with " + exitvalue);
                }
            } catch (Exception e) {
                Log.e(TAG, "verifying " + signatureFilename + " failed!");
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
            // if the view changes too quickly, this seems to happen sometimes:
            try {
                if (dialog.isShowing())
                    dialog.dismiss();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            verifyComplete(result);
        }
    }
}
