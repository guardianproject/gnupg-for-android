package info.guardianproject.gpg;

import info.guardianproject.gpg.R;
import android.app.Activity;
import android.os.Bundle;

public class GnuPrivacyGuard extends Activity {
	public static final String TAG = "gpg";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		NativeHelper.setup(getApplicationContext());
		
		// if(! DebiHelper.app_bin.exists())
		NativeHelper.unzipFiles(this);
		// TODO figure out how to manage the scripts on upgrades, etc.

        setContentView(R.layout.main);
    }
}
