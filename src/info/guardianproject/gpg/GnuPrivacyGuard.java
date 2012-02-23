package info.guardianproject.gpg;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnCreateContextMenuListener;

public class GnuPrivacyGuard extends Activity implements OnCreateContextMenuListener {
	public static final String TAG = "gpg";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		NativeHelper.setup(getApplicationContext());
		NativeHelper.unpackAssets(getApplicationContext());
		// TODO figure out how to manage upgrades, etc.

        setContentView(R.layout.main);
    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_list_keys:
			return true;
		case R.id.menu_run_test:
//			command = "./test.sh " + NativeHelper.args;
//			commandThread = new CommandThread();
//			commandThread.start();
			return true;
		}
		return false;
	}
}
