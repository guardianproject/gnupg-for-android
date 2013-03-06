package info.guardianproject.gpg;

import info.guardianproject.gpg.pinentry.PinentryStruct;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class PinEntryActivity extends Activity {

	static final String TAG = "PinEntryActivity";

	private PinentryStruct pinentry;

	static {
		System.load("/data/data/info.guardianproject.gpg/lib/libpinentry.so");
	}

//    private native void startPinentryLoop();
    private native void connectToGpgAgent();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		NativeHelper.setup(this);
		Log.d("PinEntryActivity", "PinEntryActivity::onCreate");

		new Thread( new Runnable() {

			@Override
			public void run() {
				connectToGpgAgent();
			}

		}).start();

	}

	static void setPinentryStruct(PinentryStruct s) {
		if( s == null)
			Log.d(TAG, "pinentry struct is null :(");
		else {
			Log.d(TAG, "PinentryStruct.title: " + s.title);
			Log.d(TAG, "PinentryStruct.description: " + s.description);

	void setPinentryStruct(PinentryStruct s) {

		synchronized (this) {
			pinentry = s;
		}
		pinentry = s;
	}
}
