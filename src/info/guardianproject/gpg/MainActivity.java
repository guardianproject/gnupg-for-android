
package info.guardianproject.gpg;

import info.guardianproject.gpg.GpgApplication.Action;
import info.guardianproject.gpg.sync.GpgContactManager;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements TabListener,
        OnPageChangeListener, OnQueryTextListener, KeyListFragment.OnKeysSelectedListener {
    private final static String TAG = "MainActivity";

    private static final String TAB_POSITION = "position";
    private final int INSTALL_COMPLETE = 0x00000000;
    private final int SHOW_WIZARD = 0x00000001;

    // for sync timing
    public static final long MILLISECONDS_PER_SECOND = 1000L;
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long MINUTES_PER_HOUR = 60L;
    public static final long SYNC_INTERVAL_IN_HOURS = 72L;
    public static final long SYNC_INTERVAL =
            SYNC_INTERVAL_IN_HOURS *
                    MINUTES_PER_HOUR *
                    SECONDS_PER_MINUTE *
                    MILLISECONDS_PER_SECOND;

    private ViewPager mPager = null;

    static class Tabs {
        public static final int PUBLIC_KEYS = 0;
        public static final int SECRET_KEYS = 1;
        public static final int FIND_KEYS = 2;
        public static final int length = 3;
    }

    KeyListFragment tabFragments[] = new KeyListFragment[Tabs.length];
    int mCurrentTab;
    static String mCurrentSearchString;
    String mPreviousSearchString;
    SearchView mSearchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // run the installer if needed
        if (NativeHelper.installOrUpgradeNeeded()) {
            Log.i(TAG, "starting InstallActivity");
            Intent intent = new Intent(this, InstallActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivityForResult(intent, INSTALL_COMPLETE);
        } else
            createMainActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        if (requestCode == INSTALL_COMPLETE)
            createMainActivity();
    }

    private void createMainActivity() {
        Log.i(TAG, "createMainActivity");

        // show the first run wizard if necessary
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showWizard = prefs.getBoolean(FirstRunWelcomeActivity.PREFS_SHOW_WIZARD, true);
        if (showWizard) {
            showWizard();
        } else {
            // don't setup account syncing unless we've shown the wizard
            setupSyncAccount();
        }

        setContentView(R.layout.main_activity);
        mPager = (ViewPager) findViewById(R.id.main_pager);
        FragmentManager mgr = getSupportFragmentManager();
        if (mgr == null)
            Log.e(TAG, "getSupportFragmentManager returned null wtf!");
        mPager.setAdapter(new MainPagerAdapter(mgr));
        mPager.setOnPageChangeListener(this);

        ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab()
                .setText(R.string.title_public_keys)
                .setTabListener(this).setTag(0));
        bar.addTab(bar.newTab()
                .setText(R.string.title_secret_keys)
                .setTabListener(this).setTag(1));
        bar.addTab(bar.newTab()
                .setText(R.string.title_find_keys)
                .setTabListener(this).setTag(2));
    }

    @Override
    protected void onResume() {
        super.onResume();
        GpgApplication.startGpgAgent(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(item);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setIconifiedByDefault(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.debugMode) {
            Intent intent = new Intent(this, DebugLogActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.settings) {
            startActivity(new Intent(this, GpgPreferenceActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        if (mPager != null && state != null) {
            mPager.setCurrentItem(state.getInt(TAB_POSITION));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        if (mPager != null && state != null) {
            state.putInt(TAB_POSITION, mPager.getCurrentItem());
        }
    }

    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        Integer position = (Integer) tab.getTag();
        mPager.setCurrentItem(position);
    }

    @Override
    public void onKeySelectionCanceled() {
    }

    private void showWizard() {
        Log.i(TAG, "showWizard");
        Intent intent = new Intent(getBaseContext(), FirstRunWelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivityForResult(intent, SHOW_WIZARD);
    }

    private void setupSyncAccount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean contactIntegrationEnabled = prefs.getBoolean(
                getString(R.string.pref_contacts_integration), true);
        if (contactIntegrationEnabled) {
            AccountManager am = AccountManager.get(MainActivity.this);
            if (am == null) {
                Log.e(TAG, "AccountManager is null");
                return;
            }

            // the main account name, this should be the user's primary email
            String account_name = "gnupg-test";
            // password can always be something fake, we don't need it
            String password = "fake-password";
            Account[] accts = am.getAccountsByType(GpgContactManager.ACCOUNT_TYPE);
            Account account = null;
            for (Account a : accts) {
                Log.v(TAG, "account: " + a.name);
                if (a.name.equals(account_name)) {
                    Log.v(TAG, "Found our account (" + a.name + " == " + account_name);
                    account = a;
                    break;
                }
            }
            if (account == null) {
                Log.v(TAG, "addAccountExplicitly");
                account = new Account(account_name, GpgContactManager.ACCOUNT_TYPE);
                boolean result = am.addAccountExplicitly(account, password, null);
                if (result) {
                    Log.d(TAG, "Sync Account Added");
                } else {
                    Log.e(TAG, "Failed to add Sync Account");
                    return;
                }
            }
            ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, false);
            ContentResolver.addPeriodicSync(account, ContactsContract.AUTHORITY, new Bundle(),
                    SYNC_INTERVAL);
            GpgApplication.mSyncAccount = account;
        }
    }

    public class MainPagerAdapter extends FragmentPagerAdapter {
        public MainPagerAdapter(FragmentManager mgr) {
            super(mgr);
        }

        @Override
        public int getCount() {
            return (Tabs.length);
        }

        @Override
        public Fragment getItem(int position) {
            Log.v(TAG, "getItem " + position);
            KeyListFragment frag = new KeyListFragment();
            Bundle args = new Bundle();
            Bundle extras = new Bundle();
            final String VERSION = "1";
            final String EXTRA_INTENT_VERSION = "intentVersion";
            extras.putString(EXTRA_INTENT_VERSION, VERSION);
            switch (position) {
                case Tabs.PUBLIC_KEYS:
                    args.putString("action", Action.SHOW_PUBLIC_KEYS);
                    break;
                case Tabs.SECRET_KEYS:
                    args.putString("action", Action.SHOW_SECRET_KEYS);
                    break;
                case Tabs.FIND_KEYS:
                    args.putString("action", Action.FIND_KEYS);
                    break;
                default:
                    return null;
            }
            args.putBundle("extras", extras);
            frag.setArguments(args);
            tabFragments[position] = frag;
            return frag;
        }
    }

    /* IGNORED EVENTS */

    @Override
    public void onPageScrollStateChanged(int arg0) {
        // no op
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        // no op
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        // no op
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // no op
    }

    @Override
    public void onKeySelected(long id, String userId) {
        // no op
    }

    @Override
    public void onKeysSelected(long[] selectedKeyIds, String[] selectedUserIds) {
        // no op
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // only FIND_KEYS really needs an actual Submit, PUBLIC_KEYS/SECRET_KEYS
        // are handled in onQueryTextChange()
        if (mPager.getCurrentItem() != Tabs.FIND_KEYS)
            return false;
        if (mCurrentSearchString.equals(mPreviousSearchString))
            return true;

        if (query.length() < 3)
            Toast.makeText(this, R.string.error_short_keyserver_query, Toast.LENGTH_LONG).show();
        else {
            KeyListFragment frag = tabFragments[Tabs.FIND_KEYS];
            frag.restartLoader();
            // hide the previous list and show the spinner:
            frag.setListShown(false);
        }
        mPreviousSearchString = mCurrentSearchString;

        return true;
    }

    // TODO
    // http://www.survivingwithandroid.com/2012/10/android-listview-custom-filter-and.html
    @Override
    public boolean onQueryTextChange(String newText) {
        mCurrentSearchString = !TextUtils.isEmpty(newText) ? newText : null;
        switch (mPager.getCurrentItem()) {
            case Tabs.PUBLIC_KEYS:
                // TODO mAdapter.getFilter().filter(mCurrentSearchString);
                // tabFragments[Tabs.PUBLIC_KEYS].mShowKeysAdapter.getFilter().filter(mCurrentSearchString);
                break;
            case Tabs.SECRET_KEYS:
                // TODO mAdapter.getFilter().filter(mCurrentSearchString);
                // tabFragments[Tabs.SECRET_KEYS].mShowKeysAdapter.getFilter().filter(mCurrentSearchString);
                break;
            case Tabs.FIND_KEYS:
                // TODO mAdapter.getFilter().filter(mCurrentSearchString);
                break;
            default:
        }
        return true;
    }
}
