
package info.guardianproject.gpg;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class InstallActivity extends Activity {
    public static final String TAG = "InstallActivity";

    private ProgressDialog mDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        new InstallTask(this).execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        /*
         * if the user goes to a different Activity, hide the dialog to prevent
         * crashes once the process is complete
         */
        hideProgressDialog();
    }

    private void hideProgressDialog() {
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
        mDialog = null;
    }

    public class InstallTask extends AsyncTask<Void, Void, Void> {
        public static final String TAG = "InstallTask";

        private final Context context = getApplicationContext();
        private final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (mDialog != null)
                    mDialog.setMessage(msg.getData().getString("message"));
            }
        };

        private void showProgressMessage(int resId) {
            Log.i(TAG, "showProgressMessage");
            String messageText = getString(resId);
            if (messageText == null)
                messageText = "(null)";
            if (mDialog == null) {
                Log.e(TAG, "installDialog is null!");
                return;
            }
            mDialog.setMessage(messageText);
            if (mDialog != null && !mDialog.isShowing())
                mDialog.show();
        }

        public InstallTask(Context c) {
            Log.i(TAG, "InstallTask");
            mDialog = new ProgressDialog(c);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mDialog.setTitle(R.string.dialog_installing_title);
        }

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "");
            showProgressMessage(R.string.dialog_installing_msg);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.i(TAG, "doInBackground");
            NativeHelper.unpackAssets(context, handler);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.i(TAG, "onPostExecute");
            hideProgressDialog();
            GpgApplication app = (GpgApplication) getApplication();
            app.setup();
            GpgApplication.requestContactsSync(true);
            finish();
        }
    }
}
