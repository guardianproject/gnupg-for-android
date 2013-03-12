package info.guardianproject.gpg;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class ImportFileActivity extends Activity {

	final String[] filetypes = { ".gpg", ".asc" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_SEND.equals(action) && type != null) {
			if ("text/plain".equals(type)) {
				handleSendText(intent); // Handle text being sent
			} else {
				handleSendBinary(intent); // Handle single image being sent
			}
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
			handleSendMultipleBinaries(intent);
		} else {
			// Handle other intents, such as being started from the home screen
			Toast.makeText(this, "unhandled other intents", Toast.LENGTH_LONG)
					.show();
		}
	}

	private boolean isSupportedFileType(Uri uri) {
		String path = uri.getLastPathSegment();
		String extension = path.substring(path.lastIndexOf('.'), path.length()).toLowerCase();
		for (String filetype : filetypes)
			if (extension.equals(filetype))
				return true;
		return false;
	}

	void handleSendText(Intent intent) {
		String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
		if (sharedText != null) {
			Toast.makeText(this, "handle send text", Toast.LENGTH_LONG).show();
		}
	}

	void handleSendBinary(Intent intent) {
		Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		if (isSupportedFileType(uri)) {
			Toast.makeText(this, "handle send binary: " + uri,
					Toast.LENGTH_LONG).show();
			Log.v(GnuPrivacyGuard.TAG, "handle send binary: " + uri);
		}
	}

	void handleSendMultipleBinaries(Intent intent) {
		ArrayList<Uri> uris = intent
				.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
		for (Uri uri : uris)
			if (isSupportedFileType(uri)) {
				Toast.makeText(this, "handle multiple binaries: " + uri, Toast.LENGTH_LONG)
				.show();
				Log.v(GnuPrivacyGuard.TAG, "handle multiple binaries: " + uri);
			}
	}

}
