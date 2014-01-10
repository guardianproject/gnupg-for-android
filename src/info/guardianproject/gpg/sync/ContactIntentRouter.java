
package info.guardianproject.gpg.sync;

import info.guardianproject.gpg.EncryptFileActivity;
import info.guardianproject.gpg.GnuPG;
import info.guardianproject.gpg.sync.SyncAdapter.EncryptFileTo;

import org.openintents.openpgp.keyserver.KeyServer.KeyInfo;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.freiheit.gnupg.GnuPGKey;

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
        Uri contactUri = intent.getData();

        String[] PROJECTION = new String[] {
                EncryptFileTo.FINGERPRINT,
        };
        Cursor c = getContentResolver().query(contactUri, PROJECTION, null, null, null);
        if (c.moveToFirst()) {
            int fingerprintIndex = c.getColumnIndex(EncryptFileTo.FINGERPRINT);
            String fingerprint = c.getString(fingerprintIndex);
            // in theory, we could get the email from the ContentProvider, but
            // this is far easier
            GnuPGKey key = GnuPG.context.getKeyByFingerprint(fingerprint);
            String[] emails = new String[] {
                    key.getEmail()
            };
            key.destroy();

            if (action.equals(Intent.ACTION_VIEW) && type != null) {
                if (type.equals(EncryptFileTo.CONTENT_ITEM_TYPE) && contactUri != null) {
                    Log.d(TAG, "got ACTION_VIEW for type =" + type + " and data =" + contactUri);
                    Intent i = new Intent(this, EncryptFileActivity.class);
                    i.putExtra(Intent.EXTRA_UID, new long[] {
                        KeyInfo.keyIdFromFingerprint(fingerprint)
                    });
                    i.putExtra(Intent.EXTRA_EMAIL, emails);
                    startActivity(i);
                }
            }
        } else
            Log.e(TAG, "There should never be multiple results here!");

        finish();
    }
}
