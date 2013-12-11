
package info.guardianproject.gpg.pinentry;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.WindowManager;

import info.guardianproject.gpg.pinentry.PinentryDialog.PinentryCallback;

/**
 * Activity for communicating with the native pinentry. To achieve the nice
 * dialog overlay, this Activity should be started with
 * Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP |
 * Intent.FLAG_ACTIVITY_NO_HISTORY);
 * 
 * @author user
 */
public class PinEntryActivity extends FragmentActivity implements PinentryCallback {

    static final String TAG = "PinEntryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        if (getIntent().getExtras().getInt("uid", -1) < 0) {
            Log.e(TAG, "missing uid. aborting");
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        PinentryDialog dialog = new PinentryDialog();
        dialog.setArguments(getIntent().getExtras());
        dialog.show(fm, "fragment_pinentry");
    }

    @Override
    public void onPinentryDialogClosed() {
        Log.i(TAG, "onPinentryDialogClosed");
        finish();
    }
}
