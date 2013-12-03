
package info.guardianproject.gpg;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import info.guardianproject.gpg.apg_compat.Apg;

public class KeyListActivity extends SherlockFragmentActivity implements
        KeyListFragment.OnKeysSelectedListener {

    KeyListFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.key_list_activity);

        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        KeyListFragment frag = new KeyListFragment();
        Intent i = getIntent();
        Bundle args = new Bundle();
        args.putString("action", i.getAction());
        args.putBundle("extras", i.getExtras());
        frag.setArguments(args);

        // adding the fragment programmatically so I can set the arguments
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        trans.add(R.id.activity_main_layout, frag);
        trans.commit();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        mFragment.handleIntent(i.getAction(), i.getExtras());
    }

    @Override
    public void onKeySelected(long id, String userId) {
        Intent data = new Intent();
        data.putExtra(Apg.EXTRA_KEY_ID, id);
        data.putExtra(Apg.EXTRA_USER_ID, userId);
        setResult(RESULT_OK, data);
        finish();

    }

    @Override
    public void onKeysSelected(long[] selectedKeyIds, String[] selectedUserIds) {
        Intent data = new Intent();
        data.putExtra(Apg.EXTRA_SELECTION, selectedKeyIds);
        data.putExtra(Apg.EXTRA_USER_IDS, selectedUserIds);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onKeySelectionCanceled() {
        setResult(RESULT_CANCELED, null);
        finish();
    }

    // @Override
    // public boolean onCreateOptionsMenu(Menu menu) {
    // menu.add(0, Id.menu.option.search, 0, R.string.menu_search)
    // .setIcon(android.R.drawable.ic_menu_search);
    // return true;
    // }
}
