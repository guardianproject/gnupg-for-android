package info.guardianproject.gpg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import info.guardianproject.gpg.utils.Constants;
import info.guardianproject.gpg.utils.G4APager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

public class GNUPGForAndroid extends FragmentActivity implements Constants, TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
	G4APager g4a;
	private ViewPager vp;
	private TabHost tabHost;
	private List<G4Broadcaster> g4broadcasters;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		g4broadcasters = new ArrayList<G4Broadcaster>();
		
		initTabHost(savedInstanceState);
		if(savedInstanceState != null)
			tabHost.setCurrentTabByTag(savedInstanceState.getString(CURRENT_TAB));
		
		initViewPager();		
	}
	
	private void initViewPager() {
		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this, KeyManagerRoot.class.getName()));
		fragments.add(Fragment.instantiate(this, MyKeysRoot.class.getName()));
		fragments.add(Fragment.instantiate(this, WebOfTrustRoot.class.getName()));
		
		g4a = new G4APager(super.getSupportFragmentManager(), fragments);
		vp = (ViewPager) super.findViewById(R.id.view_pager_root);
		vp.setAdapter(g4a);
		vp.setOnPageChangeListener(this);
	}
	
	private void initTabHost(Bundle args) {
		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();
		
		G4Tab g4tab = null;
		View g4indicator = null;
		
		addTab(this, tabHost, 
				tabHost.newTabSpec(KeyManager.TAG)
				.setIndicator((g4indicator = new G4Indicator(getString(R.string.indicator_keyManager), this).tab)),
				(g4tab = new G4Tab(KeyManager.TAG, KeyManagerRoot.class, args)));
		
		addTab(this, tabHost,
				tabHost.newTabSpec(MyKeys.TAG)
				.setIndicator((g4indicator = new G4Indicator(getString(R.string.indicator_myKeys), this).tab)),
				(g4tab = new G4Tab(MyKeys.TAG, MyKeysRoot.class, args)));
		
		addTab(this, tabHost,
				tabHost.newTabSpec(WebOfTrust.TAG)
				.setIndicator((g4indicator = new G4Indicator(getString(R.string.indicator_webOfTrust), this).tab)),
				(g4tab = new G4Tab(WebOfTrust.TAG, WebOfTrust.class, args)));
		
		tabHost.setOnTabChangedListener(this);
		
	}
	
	private static void addTab(GNUPGForAndroid activity, TabHost tabHost, TabHost.TabSpec tabSpec, G4Tab g4tab) {
		tabSpec.setContent(activity.new TabFactory(activity));
		tabHost.addTab(tabSpec);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURRENT_TAB, tabHost.getCurrentTab());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onTabChanged(String tabId) {
		vp.setCurrentItem(tabHost.getCurrentTab());
	}

	@Override
	public void onPageScrollStateChanged(int state) {}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {}

	@Override
	public void onPageSelected(int page) {
		tabHost.setCurrentTab(page);
	}
	
	private class G4Indicator {
		View tab;
		
		G4Indicator(String label, Context context) {
			tab = LayoutInflater.from(context).inflate(R.layout.tab_layout, null, false);
			TextView indicator = (TextView) tab.findViewById(R.id.tabIndicator);
			indicator.setText(label);
		}
	}
	
	private class G4Tab {
		private String tabId;
		private Class<?> clz;
		private Bundle args;
		public View tab;
		
		G4Tab(String tagId, Class<?> clz, Bundle args) {
			this.tabId = tagId;
			this.clz = clz;
			this.args = args;
			
		}
	}
	
	public class TabFactory implements TabHost.TabContentFactory {
		Context context;
		
		public TabFactory(Context context) {
			this.context = context;
		}
		
		@Override
		public View createTabContent(String tag) {
			View view = new View(context);
			view.setMinimumHeight(0);
			view.setMinimumWidth(0);
			return view;
		}
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		g4broadcasters.add(new G4Broadcaster(new IntentFilter(GenerateNewKey.Actions.GENERATE_NEW_KEY)));
		
		for(G4Broadcaster b : g4broadcasters)
			registerReceiver(b, ((G4Broadcaster) b).intentFilter);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		for(G4Broadcaster b : g4broadcasters)
			unregisterReceiver(b);
	}
	

	class G4Broadcaster extends BroadcastReceiver {
		IntentFilter intentFilter;
		
		public G4Broadcaster(IntentFilter intentFilter) {
			this.intentFilter = intentFilter;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(GenerateNewKey.Actions.GENERATE_NEW_KEY)) {
				// TODO: what do we do with the params?
				// obviously, launch pinentry screen to get password...
				HashMap<String, Object> newKeyPreferences = (HashMap<String, Object>) intent.getSerializableExtra(GenerateNewKey.Actions.GENERATE_NEW_KEY);
			}
				
			
		}

	}

}
