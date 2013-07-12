
package info.guardianproject.gpg;

import java.io.File;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class VerifyActivity extends Activity {
    private static final String TAG = "VerifyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String mimeType = intent.getType();
        Uri uri = intent.getData();
        Log.i(TAG, "action: " + action + "   MIME Type: " + mimeType + "   uri: " + uri);

        String signatureFilename = uri.getPath();
        String extension = MimeTypeMap.getFileExtensionFromUrl(signatureFilename);
        if (extension.equals("asc")) {
            new VerifyFileTask(this).execute(signatureFilename);
        } else if (extension.equals("sig")) {
            String signedFilename = signatureFilename.replaceAll("\\.sig$", "");
            if (new File(signedFilename).exists())
                new VerifyFileTask(this).execute(signatureFilename);
            else
                Toast.makeText(this, signedFilename + " does not exist to verify!",
                        Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, uri + " not handled yet");
        }
        // TODO verify content:// and streams by using GnuPG.context.verify()
        finish();
    }

    private void verifyComplete(Integer result) {
        Log.d(TAG, "verify complete");
        setResult(result);
        if (result != RESULT_OK)
            Toast.makeText(this, "VERIFY FAILED!", Toast.LENGTH_LONG).show();
        finish();
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
                String args = "--verify " + signatureFilename;
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
            if (dialog.isShowing())
                dialog.dismiss();
            verifyComplete(result);
        }
    }
}
