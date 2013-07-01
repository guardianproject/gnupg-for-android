
package info.guardianproject.gpg.sync;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * This class catches intents from our contact integration. It then forwards
 * these intents to the actual Activities that can should handle the requested
 * action. This activity has no actual UI.
 */
public class ContactIntentRouter extends Activity {
    private static final String TAG = ContactIntentRouter.class.getSimpleName();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleIntent();
    }

    void handleIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri data = intent.getData();

        if (action.equals(Intent.ACTION_VIEW) && type != null) {
            if (type.equals(SyncAdapterColumns.MIME_ENCRYPT_FILE_TO) && data != null) {
                Log.d(TAG, "got ACTION_VIEW for typ=" + type + " and dat=" + data);
                // TODO do stuff here
            }
        }
        // if we get here that means we didn't handle the activity
        finish();
    }
}
