
package info.guardianproject.gpg;

import info.guardianproject.gpg.GpgApplication.Action;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;

public class SelectKeysActivity extends ActionBarActivity {
    public static final String TAG = "SelectKeysActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_keys_activity);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        KeyListFragment frag = new KeyListFragment();
        Intent i = getIntent();
        Bundle args = new Bundle();
        args.putString("action", Action.SELECT_PUBLIC_KEYS);
        args.putBundle("extras", i.getExtras());
        frag.setArguments(args);

        // adding the fragment programmatically so I can set the arguments
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        trans.add(R.id.select_keys_activity_layout, frag);
        trans.commit();
    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {
        Log.i(TAG, "onSupportActionModeFinished");
        finish();
    }

}
