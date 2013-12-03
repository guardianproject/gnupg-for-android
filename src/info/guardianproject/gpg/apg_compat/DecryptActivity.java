
package info.guardianproject.gpg.apg_compat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGData;

import info.guardianproject.gpg.GnuPG;
import info.guardianproject.gpg.R;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DecryptActivity extends Activity {
    public static final String TAG = "DecryptActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        String action = intent.getAction();
        if (action.equals(Apg.Intent.DECRYPT_AND_RETURN)) {
            if (intent.getData() != null)
                Log.w(TAG, Apg.Intent.DECRYPT_AND_RETURN
                        + " " + getString(R.string.error_files_not_supported));
            Bundle extras = intent.getExtras();
            // add empty content so we don't have to check for null again
            if (extras == null)
                extras = new Bundle();

            boolean asciiArmor = extras.getBoolean(Apg.EXTRA_ASCII_ARMOUR, true);
            GnuPG.context.setArmor(asciiArmor);
            GnuPGData plain = null;
            if (extras.getBoolean(Apg.EXTRA_BINARY, false))
                plain = GnuPG.context.decrypt(extras.getByteArray(Apg.EXTRA_DATA));
            else
                plain = GnuPG.context.decrypt(extras.getString(Apg.EXTRA_TEXT));

            Bundle data = new Bundle();
            data.putInt(Constants.extras.status, Id.message.done);
            if (asciiArmor) {
                data.putString(Apg.EXTRA_DECRYPTED_MESSAGE, plain.toString());
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(plain.size());
                BufferedOutputStream out = new BufferedOutputStream(baos, 8192);
                try {
                    plain.write(out);
                } catch (IOException e) {
                    e.printStackTrace();
                    finishWithError(getString(R.string.error_decrypting_failed));
                    return;
                }
                data.putByteArray(Apg.EXTRA_DECRYPTED_DATA, baos.toByteArray());
            }
            if (plain != null)
                plain.destroy();

            Intent resultIntent = new Intent();
            resultIntent.putExtras(data);
            setResult(RESULT_OK, resultIntent);
        }
        finish();
    }

    private void finishWithError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }
}
