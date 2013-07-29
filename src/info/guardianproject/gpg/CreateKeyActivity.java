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
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGGenkeyResult;

public class CreateKeyActivity extends Activity {
	public static final String TAG = "CreateKeyActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.create_key_activity);

		setNameAndEmail();

		registerForContextMenu(findViewById(R.id.keySize));
		registerForContextMenu(findViewById(R.id.keyExpire));

		Button createKeyButton = (Button) findViewById(R.id.createKeyButton);
		createKeyButton.setOnClickListener(new OnClickListener() {

		    @Override
		    public void onClick(View v) {
		        String params = "<GnupgKeyParms format=\"internal\">\n"; // gpgme format
		        //String params = "%echo Generating basic OpenPGP key\n"; // "gpg2 --batch --gen-key" format
		        // TODO figure out key/subkey, for now default to RSA+RSA
		        params += "Key-Type: RSA\n";
		        params += getKeyLength();
		        // TODO the subkeys should be configurable
		        params += "Subkey-Type: RSA\n";
		        params += getSubkeyLength();
		        params += getNameReal();
		        params += getNameEmail();
		        params += getNameComment();
		        params += getExpireDate();
		        params += "</GnupgKeyParms>\n"; // gpgme format
		        //params += "%commit\n%echo done\n"; // "gpg2 --batch --gen-key" format
		        new CreateKeyTask(v.getContext()).execute(params);
		    }
		});
	}

	private String getKeyLength() {
	    String keySize = ((TextView) findViewById(R.id.keySize)).getText().toString();
	    return "Key-Length: " + keySize + "\n";
	}

	private String getSubkeyLength() {
	    String keySize = ((TextView) findViewById(R.id.keySize)).getText().toString();
	    return "Subkey-Length: " + keySize + "\n";
	}

	private String getNameReal() {
	    String keyName = ((EditText) findViewById(R.id.keyName)).getText().toString();
	    return "Name-Real: " + keyName + "\n";
	}

	private String getNameEmail() {
	    String keyEmail = ((EditText) findViewById(R.id.keyEmail)).getText().toString();
	    return "Name-Email: " + keyEmail + "\n";
	}

	private String getNameComment() {
	    String keyComment = ((EditText) findViewById(R.id.keyComment)).getText().toString();
	    // gpg2 --gen-key barfs on a blank Name-Comment, so omit if blank
	    if (keyComment == null || keyComment.equals(""))
	        return "";
	    else
	        return "Name-Comment: " + keyComment + "\n";
	}

	private String getExpireDate() {
	    String keyExpire = ((TextView) findViewById(R.id.keyExpire)).getText().toString();
	    if (keyExpire.equals(getString(R.string.key_expire_one_month)))
	        keyExpire = "1m";
	    else if (keyExpire.equals(getString(R.string.key_expire_one_year)))
	        keyExpire = "1y";
	    else if (keyExpire.equals(getString(R.string.key_expire_two_years)))
	        keyExpire = "2y";
	    else if (keyExpire.equals(getString(R.string.key_expire_five_years)))
	        keyExpire = "5y";
	    else if (keyExpire.equals(getString(R.string.key_expire_ten_years)))
	        keyExpire = "10y";
	    else if (keyExpire.equals(getString(R.string.key_expire_never)))
	        keyExpire = "0";
	    return "Expire-Date: " + keyExpire + "\n";
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

	private void createKeyComplete(Integer result) {
	    Log.d(TAG, "gen-key complete, sending broadcast");
	    GpgApplication.sendKeylistChangedBroadcast(this);
	    setResult(result);
	    if (result != RESULT_OK)
	        Toast.makeText(this, getString(R.string.error_gen_key_failed), Toast.LENGTH_LONG).show();
	    finish();
	}

	public class CreateKeyTask extends AsyncTask<String, String, Integer> {
		private ProgressDialog dialog;
		private Context context;

		public CreateKeyTask(Context c) {
			context = c;
			dialog = new ProgressDialog(context);
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setTitle(R.string.dialog_generating_new_key_title);
			dialog.setMessage(getString(R.string.generating_new_key_message));
		}

		@Override
		protected void onPreExecute() {
		    super.onPreExecute();
		    dialog.show();
		}

		@Override
		protected Integer doInBackground(String... params) {
		    Log.i(TAG, "doInBackground: " + params[0]);
		    try {
		        GnuPG.context.genPgpKey(params[0]);
		        GnuPGGenkeyResult result = GnuPG.context.getGenkeyResult();
		        String fpr = result.getFpr();
		        String sdcard = Environment.getExternalStorageDirectory().getCanonicalPath();
		        if (((CheckBox) findViewById(R.id.keyRevokeGen)).isChecked()) {
		            publishProgress(getString(R.string.generating_revoke_cert));
		            GnuPG.gpg2(" --output " + sdcard + "/revoke-" + fpr
		                    + ".asc --gen-revoke " + fpr);
		        }
		        if (((CheckBox) findViewById(R.id.keyUpload)).isChecked()) {
		            publishProgress(getString(R.string.uploading_to_keyserver));
		            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		            String ks = p.getString(GpgPreferenceActivity.PREF_KEYSERVER,"200.144.121.45");
		            GnuPG.gpg2(" --keyserver " + ks + " --send-keys " + fpr);
		        }
		        if (((CheckBox) findViewById(R.id.keyMakeBackup)).isChecked()) {
		            publishProgress(getString(R.string.backing_up_to_sdcard));
		            GnuPG.gpg2(" --output " + sdcard + "/gpgSecretKey-" + fpr
		                    + ".skr --export-secret-keys " + fpr);
		        }
		    } catch(Exception e) {
		        Log.e(TAG, "genPgpKey failed!");
		        e.printStackTrace();
		        return RESULT_CANCELED;
		    }
		    return RESULT_OK;
		}

		@Override
		protected void onProgressUpdate(String... messages) {
		    super.onProgressUpdate(messages);
		    dialog.setMessage(messages[0]);
		}

		@Override
		protected void onPostExecute(Integer result) {
		    Log.i(TAG, "onPostExecute");
            try {
                // if the view changes too quickly, this seems to happen sometimes
                if (dialog.isShowing())
                    dialog.dismiss();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
			createKeyComplete(result);
		}
	}
}
