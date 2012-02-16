package info.guardianproject.gnupg;

import android.app.Activity;
import android.os.Bundle;

public class GnupgActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}