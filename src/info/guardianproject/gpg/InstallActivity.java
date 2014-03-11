
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        new InstallTask(this).execute();
    }

    public class InstallTask extends AsyncTask<Void, Void, Void> {
        public static final String TAG = "InstallTask";
        private ProgressDialog dialog;

        private final Context context = getApplicationContext();
        private final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                dialog.setMessage(msg.getData().getString("message"));
            }
        };

        private void showProgressMessage(int resId) {
            Log.i(TAG, "showProgressMessage");
            String messageText = getString(resId);
            if (messageText == null)
                messageText = "(null)";
            if (dialog == null) {
                Log.e(TAG, "installDialog is null!");
                return;
            }
            dialog.setMessage(messageText);
            if (!dialog.isShowing())
                dialog.show();
        }

        private void hideProgressDialog() {
            dialog.dismiss();
        }

        public InstallTask(Context c) {
            Log.i(TAG, "InstallTask");
            dialog = new ProgressDialog(c);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setTitle(R.string.dialog_installing_title);
            dialog.setCancelable(false);
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
