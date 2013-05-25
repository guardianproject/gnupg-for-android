
package info.guardianproject.gpg.apg_compat;

import info.guardianproject.gpg.NativeHelper;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGContext;
import com.freiheit.gnupg.GnuPGKey;

public class EncryptActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GnuPGContext gnupg = null;
		Intent intent = getIntent();
		String action = intent.getAction();
		if (action.equals(Apg.Intent.ENCRYPT_AND_RETURN)) {
			Bundle extras = intent.getExtras();
			// add empty content so we don't have to check for null again
			if (extras == null)
				extras = new Bundle();

			gnupg = new GnuPGContext();
			// set the homeDir option to our custom home location
			gnupg.setEngineInfo(gnupg.getProtocol(), gnupg.getFilename(),
					NativeHelper.app_home.getAbsolutePath());
            if (extras.containsKey(Apg.EXTRA_ASCII_ARMOUR))
            	gnupg.setTextmode(extras.getBoolean(Apg.EXTRA_ASCII_ARMOUR, true));

			int i;
			long encryptionKeyIdValues[] = extras
					.getLongArray(Apg.EXTRA_ENCRYPTION_KEY_IDS);
			String[] encryptionKeyIds = new String[encryptionKeyIdValues.length];
			for (i = 0; i < encryptionKeyIds.length; i++)
				encryptionKeyIds[i] = Long.toHexString(encryptionKeyIdValues[i]);
			ArrayList<GnuPGKey> recipientsKeyList = new ArrayList<GnuPGKey>();
			GnuPGKey key = null;
			for (i = 0; i < encryptionKeyIds.length; i++) {
				key = gnupg.getKeyByFingerprint(encryptionKeyIds[i]);
				if (key != null && key.canEncrypt() && !key.isDisabled() && !key.isRevoked())
					recipientsKeyList.add(key);
			}
			// TODO perhaps prompt the user to select a key?
			if (recipientsKeyList.size() == 0)
				finishWithError("no valid encryption keys!");

			long signatureKeyId = extras.getLong(Apg.EXTRA_SIGNATURE_KEY_ID);
			GnuPGKey signatureKey = null;
			if (signatureKeyId != 0) {
				key = gnupg.getSecretKeyByFingerprint(Long.toHexString(signatureKeyId));
				if (key != null && key.canSign() && !key.isDisabled() && !key.isRevoked())
					signatureKey = key;
			}

			// TODO encrypt and sign!

            byte[] extraData = extras.getByteArray(Apg.EXTRA_DATA);
            final GnuPGKey[] recipients = recipientsKeyList.toArray(new GnuPGKey[0]);
			Bundle data = new Bundle();
            data.putInt(Constants.extras.status, Id.message.done);
			if (extraData == null) {
				String extraText = extras.getString(Apg.EXTRA_TEXT);
	    		data.putString(Apg.EXTRA_ENCRYPTED_MESSAGE,
	    				gnupg.encryptToAscii(recipients, extraText));
			} else {
	    		data.putByteArray(Apg.EXTRA_ENCRYPTED_DATA,
	    				gnupg.encryptToBinary(recipients, extraData));
			}

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