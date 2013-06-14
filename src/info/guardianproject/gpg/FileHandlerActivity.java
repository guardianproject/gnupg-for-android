package info.guardianproject.gpg;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.freiheit.gnupg.GnuPGData;

public class FileHandlerActivity extends Activity {
	public static final String TAG = "FileHandlerActivity";

	public static final String[] extensions = { ".asc", ".gpg", ".pgp", ".sig" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();
		File incomingFile;
		try {
			incomingFile = new File(new URI(intent.getDataString()).getPath());
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			return;
		}
		Log.i(TAG, "action: " + action + "   MIME Type: " + type + "   data: "
				+ incomingFile);

		GnuPGData cipher;
		// getCanonicalFile() is a workaround for missing /storage/emulated/0
		try {
			byte[] contents = FileUtils.readFileToByteArray(incomingFile);
			cipher = GnuPG.context.createDataObject(contents);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		GnuPGData plain = GnuPG.context.createDataObject();
		GnuPG.context.decrypt(cipher, plain);
		cipher.destroy();

		String incomingFileName = incomingFile.getAbsolutePath();
		String plainFile = incomingFileName.substring(0, incomingFileName.length() - 4);
		String extension = MimeTypeMap.getFileExtensionFromUrl(plainFile);
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
				extension);
		Log.i(TAG, "plain file: " + plainFile + "  extention: " + extension
				+ "  MIME: " + mimeType);

		FileOutputStream out;
		// getCanonicalFile() is a workaround for missing /storage/emulated/0
		try {
			out = new FileOutputStream(new File(plainFile).getCanonicalFile());
			plain.write(out);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		Intent viewIntent = new Intent(Intent.ACTION_VIEW);
		viewIntent.setDataAndType(Uri.parse(plainFile), mimeType);
		startActivity(viewIntent);
	}
}
