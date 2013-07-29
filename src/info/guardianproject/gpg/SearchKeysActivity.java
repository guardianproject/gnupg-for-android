
package info.guardianproject.gpg;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGKey;

public class SearchKeysActivity extends ListActivity {
    public static final String TAG = "SearchKeysActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String query = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        if (GnuPG.context == null)
            Log.e(TAG, "GnuPG.context is null!");
        String[] keys = new String[0];
        GnuPGKey[] keyArray = GnuPG.context.searchKeys(query);
        if (keyArray == null) {
            Log.i(TAG, "menu_search_keys: null");
        } else {
            keys = new String[keyArray.length];
            for (int i = 0; i < keys.length; i++) {
                keys[i] = keyArray[i].toString();
            }
        }
        setListAdapter(new ArrayAdapter<String>(this, R.layout.search_keys_activity, keys));

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
