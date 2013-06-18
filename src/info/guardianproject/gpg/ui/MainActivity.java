package info.guardianproject.gpg.ui;

import info.guardianproject.gpg.GnuPrivacyGuard;
import info.guardianproject.gpg.GpgApplication;
import info.guardianproject.gpg.InstallActivity;
import info.guardianproject.gpg.NativeHelper;
import info.guardianproject.gpg.R;
import info.guardianproject.gpg.apg_compat.Apg;
import info.guardianproject.gpg.sync.SyncConstants;
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
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockFragmentActivity
                          implements TabListener,
                          OnPageChangeListener,
                          KeyListFragment.OnKeysSelectedListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    private static final String TAB_POSITION="position";
    private ViewPager pager = null;
    private final int INSTALL_COMPLETE = 0x00000000;
    private final int SHOW_WIZARD =      0x00000001;
    
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
    	SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(this);
    	boolean showWizard = prefs.getBoolean(FirstRunWelcome.PREFS_SHOW_WIZARD, true);
    	if( showWizard ) {
    		showWizard();
    	} else {
    		// don't setup account syncing unless we've shown the wizard
    		setupSyncAccount();
    	}

    	setContentView(R.layout.activity_main);
    	pager = (ViewPager) findViewById(R.id.main_pager);
        FragmentManager mgr = getSupportFragmentManager();
        if( mgr == null ) Log.e(TAG, "getSupportFragmentManager returned null wtf!");
        pager.setAdapter(new MainPagerAdapter(mgr));
        pager.setOnPageChangeListener(this);

        ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab()
                        .setText("Public Keys")
                        .setTabListener(this).setTag(0));
        bar.addTab(bar.newTab()
                .setText("Private Keys")
                .setTabListener(this).setTag(1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        GpgApplication.startGpgAgent(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if( item.getItemId() == R.id.debugMode) {
            Intent intent = new Intent(this, GnuPrivacyGuard.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
      super.onRestoreInstanceState(state);

      if( pager!= null && state != null ) {
          pager.setCurrentItem(state.getInt(TAB_POSITION));
      }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
      super.onSaveInstanceState(state);

      if(pager != null && state != null) {
    	  state.putInt(TAB_POSITION, pager.getCurrentItem());
      }
    }

    @Override
    public void onPageSelected(int position) {
        getSupportActionBar().setSelectedNavigationItem(position);
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        Integer position=(Integer) tab.getTag();
        pager.setCurrentItem(position);
    }


    @Override
    public void onKeySelectionCanceled() {
    }

    private void showWizard() {
    	Log.i(TAG, "showWizard");
    	Intent intent = new Intent(getBaseContext(), FirstRunWelcome.class);
    	intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    	startActivityForResult(intent, SHOW_WIZARD);
    }

    private void setupSyncAccount() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean contactIntegrationEnabled = prefs.getBoolean(
                SyncConstants.PREFS_INTEGRATE_CONTACTS, true);
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
            Account[] accts = am.getAccountsByType(SyncConstants.ACCOUNT_TYPE);
            for( Account a : accts ) {
                if( a.name == account_name )
                    return;
            }
            Account account = new Account(account_name, SyncConstants.ACCOUNT_TYPE);
            boolean result = am.addAccountExplicitly(account, password, null);
            if (result) {
                Log.d(TAG, "Account Added");
            } else {
                Log.e(TAG, "Account Add failed");
            }
            ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
            ContentResolver.requestSync(account, ContactsContract.AUTHORITY, new Bundle());
        }
    }

    public class MainPagerAdapter extends FragmentPagerAdapter {
        public MainPagerAdapter(FragmentManager mgr) {
          super(mgr);
        }

        @Override
        public int getCount() {
          return(2);
        }

        @Override
        public Fragment getItem(int position) {
            KeyListFragment frag = new KeyListFragment();
            Bundle args = new Bundle();
            Bundle extras = new Bundle();
            // TODO this should use GnuPG.context.listKeys() and .listSecretKeys()
    		final String VERSION = "1";
    		final String EXTRA_INTENT_VERSION = "intentVersion";
            switch (position) {
                case 0: // public keys
                {
                    args.putString("action", Apg.Intent.SELECT_PUBLIC_KEYS);
                    extras.putString(EXTRA_INTENT_VERSION, VERSION);
                    break;
                }
                case 1: //private keys
                {
                    args.putString("action", Apg.Intent.SELECT_SECRET_KEY);
                    extras.putString(EXTRA_INTENT_VERSION, VERSION);
                    break;
                }
                default:
                    return null;
            }
            frag.toggleButtons(false);
            args.putBundle("extras", extras);
            frag.setArguments(args);
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
}