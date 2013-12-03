
package info.guardianproject.gpg.apg_compat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGException;
import com.freiheit.gnupg.GnuPGKey;

import info.guardianproject.gpg.GnuPG;
import info.guardianproject.gpg.R;

import java.util.ArrayList;

public class EncryptActivity extends Activity {
    public static final String TAG = "EncryptActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals(Apg.Intent.ENCRYPT_AND_RETURN)) {
            Bundle extras = intent.getExtras();
            // add empty content so we don't have to check for null again
            if (extras == null)
                extras = new Bundle();

            if (extras.containsKey(Apg.EXTRA_ASCII_ARMOUR))
                GnuPG.context.setTextmode(extras.getBoolean(Apg.EXTRA_ASCII_ARMOUR, true));

            int i;
            long encryptionKeyIdValues[] = extras
                    .getLongArray(Apg.EXTRA_ENCRYPTION_KEY_IDS);
            String[] encryptionKeyIds = new String[encryptionKeyIdValues.length];
            for (i = 0; i < encryptionKeyIds.length; i++)
                encryptionKeyIds[i] = Long.toHexString(encryptionKeyIdValues[i]);
            ArrayList<GnuPGKey> recipientsKeyList = new ArrayList<GnuPGKey>();
            GnuPGKey key = null;
            for (i = 0; i < encryptionKeyIds.length; i++) {
                key = GnuPG.context.getKeyByFingerprint(encryptionKeyIds[i]);
                if (key != null && key.canEncrypt() && !key.isDisabled() && !key.isRevoked())
                    recipientsKeyList.add(key);
            }
            // TODO perhaps prompt the user to select a key?
            if (recipientsKeyList.size() == 0)
                finishWithError("no valid encryption keys!");

            long signatureKeyId = extras.getLong(Apg.EXTRA_SIGNATURE_KEY_ID);
            if (signatureKeyId != 0) {
                key = GnuPG.context.getSecretKeyByFingerprint(Long.toHexString(signatureKeyId));
                if (key != null && key.canSign() && !key.isDisabled() && !key.isRevoked())
                    GnuPG.context.addSigner(key);
            }

            byte[] extraData = extras.getByteArray(Apg.EXTRA_DATA);
            final GnuPGKey[] recipients = recipientsKeyList.toArray(new GnuPGKey[0]);
            Bundle data = new Bundle();
            data.putInt(Constants.extras.status, Id.message.done);
            Intent resultIntent = new Intent();
            try {
                if (extraData == null) {
                    String extraText = extras.getString(Apg.EXTRA_TEXT);
                    data.putString(Apg.EXTRA_ENCRYPTED_MESSAGE,
                            GnuPG.context.encryptToAscii(recipients, extraText));
                } else {
                    data.putByteArray(Apg.EXTRA_ENCRYPTED_DATA,
                            GnuPG.context.encryptToBinary(recipients, extraData));
                }
                resultIntent.putExtras(data);
                setResult(RESULT_OK, resultIntent);
            } catch (GnuPGException e) {
                Toast.makeText(this, R.string.error_encrypting_failed, Toast.LENGTH_LONG).show();
                e.printStackTrace();
                setResult(RESULT_CANCELED, resultIntent);
            }
        }
        finish();
    }

    private void finishWithError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }
}
