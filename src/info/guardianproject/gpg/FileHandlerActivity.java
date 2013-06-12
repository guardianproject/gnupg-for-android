package info.guardianproject.gpg;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGData;
import com.freiheit.gnupg.GnuPGException;

public class FileHandlerActivity extends Activity {
	public static final String TAG = "FileHandlerActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();
		String data = intent.getDataString();
		Log.i(TAG, "action: " + action + "   MIME Type: " + type + "   data: "
				+ data);
	}

}
