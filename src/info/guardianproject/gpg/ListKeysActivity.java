package info.guardianproject.gpg;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGContext;
import com.freiheit.gnupg.GnuPGKey;

public class ListKeysActivity extends ListActivity {
	public static final String TAG = "ListKeysActivity";
	GnuPGContext ctx = NativeHelper.gpgCtx;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if( ctx == null )
			Log.e(TAG, "GPG context is null!");

		GnuPGKey[] keyArray = ctx.listKeys();
		String[] keys = new String[0];

		if (keyArray == null) {
			Log.e(TAG, "keyArray is null");
		} else {
			keys = new String[keyArray.length];
			for (int i = 0; i < keys.length; i++) {
				keys[i] = keyArray[i].toString();
			}
		}
		setListAdapter(new ArrayAdapter<String>(this, R.layout.keys, keys));

		ListView listView = getListView();
		listView.setTextFilterEnabled(true);

		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				Toast.makeText(getApplicationContext(),
						((TextView) view).getText(), Toast.LENGTH_SHORT).show();
			}
		});

	}
}
