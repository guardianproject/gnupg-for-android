package info.guardianproject.gpg;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.freiheit.gnupg.GnuPGGenkeyResult;

public class CreateKeyActivity extends Activity {
	public static final String TAG = "CreateKeyActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.create_key);

		setNameAndEmail();

		registerForContextMenu(findViewById(R.id.keySize));
		registerForContextMenu(findViewById(R.id.keyExpire));

		Button createKeyButton = (Button) findViewById(R.id.createKeyButton);
		createKeyButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String params = "<GnupgKeyParms format=\"internal\">\n";
				String keyName = ((EditText) findViewById(R.id.keyName))
						.getText().toString();
				params += "Name-Real: " + keyName + "\n";
				String keyEmail = ((EditText) findViewById(R.id.keyEmail))
						.getText().toString();
				params += "Name-Email: " + keyEmail + "\n";
				String keyComment = ((EditText) findViewById(R.id.keyComment))
						.getText().toString();
				params += "Name-Comment: " + keyComment + "\n";
				// TODO figure out key/subkey, for now default to RSA+RSA
				params += "Key-Type: RSA\n";
				String keySize = ((TextView) findViewById(R.id.keySize))
						.getText().toString();
				params += "Key-Length: " + keySize + "\n";
				// TODO the subkeys should be configurable
				params += "Subkey-Type: RSA\n";
				params += "Subkey-Length: " + keySize + "\n";
				String keyExpire = ((TextView) findViewById(R.id.keyExpire))
						.getText().toString();
				if (keyExpire.equals(getString(R.string.key_expire_one_month)))
					keyExpire = "1m";
				else if (keyExpire
						.equals(getString(R.string.key_expire_one_year)))
					keyExpire = "1y";
				else if (keyExpire
						.equals(getString(R.string.key_expire_two_years)))
					keyExpire = "2y";
				else if (keyExpire
						.equals(getString(R.string.key_expire_five_years)))
					keyExpire = "5y";
				else if (keyExpire
						.equals(getString(R.string.key_expire_ten_years)))
					keyExpire = "10y";
				else if (keyExpire.equals(getString(R.string.key_expire_never)))
					keyExpire = "0";
				params += "Expire-Date: " + keyExpire + "\n";
				params += "</GnupgKeyParms>\n";
				new CreateKeyTask(v.getContext()).execute(params);
			}
		});
	}

	private void setNameAndEmail() {
		String email = null;

		// get email address from first system account that looks like an email
		AccountManager manager = AccountManager.get(this);
		for (Account account : manager.getAccounts())
			if (account.name.contains("@") && account.name.contains(".")) {
				email = account.name;
				EditText keyEmail = (EditText) findViewById(R.id.keyEmail);
				keyEmail.setText(email);
				break;
			}
		if (email == null)
			return;

		// use that email to look up the name in Contacts
		final String[] projection = { Contacts.DISPLAY_NAME,
				CommonDataKinds.Email.DATA, };
		Cursor cursor = getContentResolver().query(
				CommonDataKinds.Email.CONTENT_URI, projection,
				CommonDataKinds.Email.DATA + " = ?", new String[] { email },
				null);
		if (cursor != null && cursor.getCount() > 0 && cursor.moveToNext()) {
			String name = cursor.getString(cursor
					.getColumnIndex(Contacts.DISPLAY_NAME));
			EditText keyName = (EditText) findViewById(R.id.keyName);
			keyName.setText(name);
			// set the focus on the layout so the keyboard doesn't pop up
			findViewById(R.id.createKeyLayout).requestFocus();
		} else
			// keyName is blank, so put the keyboard focus there
			findViewById(R.id.keyName).requestFocus();
		// TODO we might want to use Profile.DISPLAY_NAME_PRIMARY on API >= 11
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		switch (v.getId()) {
		case R.id.keySize:
			inflater.inflate(R.menu.key_size, menu);
			break;
		case R.id.keyExpire:
			inflater.inflate(R.menu.key_expire, menu);
			break;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int groupId = item.getGroupId();
		if (groupId == R.id.groupKeySize) {
			TextView keySize = ((TextView) findViewById(R.id.keySize));
			switch (item.getItemId()) {
			case R.id.keySize1024:
				keySize.setText(R.string.key_size_1024);
				return true;
			case R.id.keySize2048:
				keySize.setText(R.string.key_size_2048);
				return true;
			case R.id.keySize4096:
				keySize.setText(R.string.key_size_4096);
				return true;
			case R.id.keySize8192:
				keySize.setText(R.string.key_size_8192);
				return true;
			}
		} else if (groupId == R.id.groupKeyExpire) {
			TextView keyExpire = ((TextView) findViewById(R.id.keyExpire));
			switch (item.getItemId()) {
			case R.id.keyExpire1Month:
				keyExpire.setText(R.string.key_expire_one_month);
				return true;
			case R.id.keyExpire1Year:
				keyExpire.setText(R.string.key_expire_one_year);
				return true;
			case R.id.keyExpire2Years:
				keyExpire.setText(R.string.key_expire_two_years);
				return true;
			case R.id.keyExpire5Years:
				keyExpire.setText(R.string.key_expire_five_years);
				return true;
			case R.id.keyExpire10Years:
				keyExpire.setText(R.string.key_expire_ten_years);
				return true;
			case R.id.keyExpireNever:
				keyExpire.setText(R.string.key_expire_never);
				return true;
			}
		}
		return super.onContextItemSelected(item);
	}

	public class CreateKeyTask extends AsyncTask<String, Void, Void> {
		private ProgressDialog dialog;
		private Context context;

		public CreateKeyTask(Context c) {
			context = c;
			dialog = new ProgressDialog(context);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setTitle(R.string.dialog_generating_new_key_title);
			dialog.setMessage(getString(R.string.dialog_generating_new_key_msg));
		}

		@Override
		protected Void doInBackground(String... params) {
			Log.i(TAG, params[0]);
			GnuPG.context.genPgpKey(params[0]);
			GnuPGGenkeyResult result = GnuPG.context.getGenkeyResult();
			String fpr = result.getFpr();
			String sdcard = Environment.getExternalStorageDirectory()
					.getAbsolutePath();
			if (((CheckBox) findViewById(R.id.keyRevokeGen)).isChecked()) {
				// TODO update ProgressDialog to say
				// "generating revokation certificate"
				GnuPG.gpg2(" --output " + sdcard + "/revoke-" + fpr
						+ ".asc --gen-revoke " + fpr);
			}
			if (((CheckBox) findViewById(R.id.keyUpload)).isChecked()) {
				// TODO update ProgressDialog to say
				// "Uploading to Keyserver"
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(context);
				String ks = prefs.getString(
						GPGPreferenceActivity.PREF_KEYSERVER, "200.144.121.45");
				GnuPG.gpg2(" --keyserver " + ks + " --send-keys " + fpr);
			}
			if (((CheckBox) findViewById(R.id.keyMakeBackup)).isChecked()) {
				// TODO update ProgressDialog to say
				// "Backing up to SDCard"
				GnuPG.gpg2(" --output " + sdcard + NativeHelper.app_home
						+ "/gpgSecreyKey-" + fpr + ".asc --export-secret-keys "
						+ fpr);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void r) {
			if (dialog.isShowing())
				dialog.dismiss();
			if (getCallingActivity() != null) {
				// we were called by another activity, so lets give
				// control back to them.
				setResult(RESULT_OK);
				finish();
			}
		}
	}
}
