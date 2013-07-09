package info.guardianproject.gpg;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGData;
import com.freiheit.gnupg.GnuPGKey;

public class FileHandlerActivity extends Activity {
	public static final String TAG = "FileHandlerActivity";

	public static final String[] extensions = { ".asc", ".gpg", ".pgp", ".pkr", ".skr", ".sig" };

	private MimeTypeMap mMap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMap = MimeTypeMap.getSingleton();

		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		String mimeType = intent.getType();
		File incomingFile;
		try {
			incomingFile = new File(new URI(intent.getDataString()).getPath());
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			return;
		}
		Log.i(TAG, "action: " + action + "   MIME Type: " + mimeType + "   data: "
				+ incomingFile);

		final String incomingFilename = incomingFile.getAbsolutePath();
		final String extension = MimeTypeMap.getFileExtensionFromUrl(incomingFilename);
		if (mimeType.equals("application/octet-stream")) {
			decryptStream();
		} else 	if (incomingFile.canRead()) {
			if (extension.equals("gpg") || extension.equals("pgp")) {
				decryptFile(incomingFile);
			} else	if (extension.equals("pkr") || extension.equals("skr")) {
			    Intent importIntent = new Intent(this, ImportFileActivity.class);
			    importIntent.setType(mimeType);
			    importIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(incomingFile));
			    startActivity(importIntent);
			} else	if (extension.equals("asc")) {
				//TODO prompt user whether this is keys, sigs, or encrypted data
			} else	if (extension.equals("sig")) {
				//TODO verify signature
			} else { // this is a file type that gpg does not recognize, so encrypt it
				encryptFile(incomingFile);
			}
		} else {
			Toast.makeText(this, getString(R.string.error_cannot_read_incoming_file),
					Toast.LENGTH_LONG).show();
		}
		finish();
	}

	private class EncryptTask extends AsyncTask<File, Void, Void> {

		@Override
		protected Void doInBackground(File... params) {
			return null;
		}
		
	}

	private void encryptFile(File incomingFile) {
		try {
			GnuPGData plain;
			plain = GnuPG.context.createDataObject(incomingFile);
			GnuPGData cipher = GnuPG.context.createDataObject();
			GnuPGKey[] recipients = new GnuPGKey[1];
			recipients[0] = GnuPG.context.getKeyByFingerprint("9F0FE587374BBE81");
			GnuPG.context.encrypt(recipients, plain, cipher);
			plain.destroy();

			String outputFilename = incomingFile.getAbsolutePath() + ".gpg";
			File outputFile = new File(outputFilename);
			FileOutputStream out;
			out = new FileOutputStream(outputFile);
			cipher.write(out);
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent.setType("*/*");
			intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile));
			intent.putExtra(Intent.EXTRA_SUBJECT, outputFile.getName());
			startActivity(Intent.createChooser(intent, getString(R.string.dialog_share_file_using)));
		} catch (Exception e) {
			String msg = String.format(getString(R.string.error_encrypting_file_failed_format),
					incomingFile);
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
			e.printStackTrace();
			finish();
			return;
		}
	}

	private class DecryptTask extends AsyncTask<File, Void, Void> {

		@Override
		protected Void doInBackground(File... params) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	private void decryptStream() {
		// TODO implement decrypting a stream
	}

	private void decryptFile(File incomingFile) {
/* this should work, but it doesn't, yet another gpgme issue...
		GnuPGData cipher;
		try {
			byte[] contents = FileUtils.readFileToByteArray(incomingFile);
			cipher = GnuPG.context.createDataObject(contents);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		GnuPGData plain = GnuPG.context.createDataObject();
		try {
			GnuPG.context.decrypt(cipher, plain);
		} catch (GnuPGException e) {
			String msg = String.format(getString(R.string.error_decrypting_file_failed),
					incomingFile);
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
			e.printStackTrace();
			finish();
			return;
		}
		cipher.destroy();

		FileOutputStream out;
		// getCanonicalFile() is a workaround for missing /storage/emulated/0
		try {
			out = new FileOutputStream(new File(plainFilename).getCanonicalFile());
			plain.write(out);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
*/
		final String incomingFilename = incomingFile.getAbsolutePath();
		final int lastPeriodPos = incomingFilename.lastIndexOf('.');
        String outputFilename = incomingFilename.substring(0, lastPeriodPos);

		File outputFile = new File(outputFilename);
		if (outputFile.exists()) {
			String format = getString(R.string.error_output_file_exists_skipping_format);
			String msg = String.format(format, outputFilename);
			Log.e(TAG, msg);
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		String args = "--output " + outputFile + " --decrypt " + incomingFile;
		GnuPG.gpg2(args);

		if (outputFile.exists()) {
			String extension = MimeTypeMap.getFileExtensionFromUrl(outputFilename);
			String mimeType = mMap.getMimeTypeFromExtension(extension);
			Log.i(TAG, "Launching VIEW Intent for " + outputFilename + " of type " + mimeType);
			Intent viewIntent = new Intent(Intent.ACTION_VIEW);
			viewIntent.setDataAndType(Uri.parse(outputFilename), mimeType);
			startActivity(viewIntent);
		} else {
			String format = getString(R.string.error_decrypting_file_failed_format);
			String msg = String.format(format, outputFilename);
			Log.e(TAG, msg);
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
		}
		finish();
	}

	private void verifyFile(File incoming) {
		
	}

	private void importFile(File incoming) {
		
	}
}
