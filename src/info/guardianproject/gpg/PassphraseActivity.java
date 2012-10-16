package info.guardianproject.gpg;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class PassphraseActivity extends Activity {

	TextView passphraseAgainTextView;
	EditText passphraseAgainEditText;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.passphrase_activity);
		passphraseAgainTextView = (TextView) findViewById(R.id.passphraseAgainTextView);
		passphraseAgainEditText = (EditText) findViewById(R.id.passphraseAgain);

	}
}
