package info.guardianproject.gpg;

import android.app.Activity;
import android.os.Bundle;

public class PinEntryActivity extends Activity {

	static {
		System.load("pinentry");
	}

    private native void startPinentryLoop();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
}
