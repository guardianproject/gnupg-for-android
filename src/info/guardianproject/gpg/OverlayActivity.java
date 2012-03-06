package info.guardianproject.gpg;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.widgets.ConfirmNewKeyActivity;
import info.guardianproject.gpg.widgets.SearchForKeyActivity;
import info.guardianproject.gpg.widgets.GPGProgressActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

public class OverlayActivity extends ActivityGroup implements Constants {
	
	public ArrayList<Map<String, Intent>> activities;
	HashMap<String, Object> returnedValues;
	
	LocalActivityManager lam;
	Window window;
	LinearLayout root;
	int index = 0;
	
	public static OverlayActivity group;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.overlayactivity);

		setAssets();
	}
	
	@Override
	public void finishFromChild(Activity a) {
		root.removeAllViews();
		index++;
		if(index < activities.size())
			changeView();
		else {
			finishWithResults();
		}
	}
	
	public void cancelGroup() {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}
	
	public void finishWithResults() {
		Intent result = new Intent();
		if(returnedValues != null && returnedValues.size() > 0)
			result.putExtra(Overlay.RESULT_DATA, returnedValues);
		
		setResult(Activity.RESULT_OK, result);			
		finish();
	}
	
	public void finishWithResults(ArrayList<HashMap<String, Object>> extras) {
		Intent result = new Intent();			
		
		if(returnedValues == null)
			returnedValues = new HashMap<String, Object>();
		
		for(HashMap<String, Object> extra : extras) {
			Iterator<Entry<String, Object>> i = extra.entrySet().iterator();
			while(i.hasNext()) {
				Entry<String, Object> b = i.next();
				returnedValues.put(b.getKey(), b.getValue());
			}
		}
		
		result.putExtra(Overlay.RESULT_DATA, returnedValues);
		setResult(Activity.RESULT_OK, result);			
		finish();
	}
	
	public void changeView() {
		Map<String, Intent> activity = activities.get(index);
		String activityTag = activity.keySet().iterator().next();
		window = lam.startActivity(activityTag, activity.get(activityTag).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		root.addView(window.getDecorView());
		
		int v = root.getChildCount();
		if(v > 1) {
			for(int oldView=1; oldView < v; oldView++)
				root.removeViewAt(oldView);
		}
	}
	
	public void handleResult(Intent intent, HashMap<String, Object> results) {
		if(returnedValues == null)
			returnedValues = new HashMap<String, Object>();
		
		returnedValues.putAll(results);
	}
	
	public Map<String, Intent> getNextIntent() {
		return activities.get(index + 1);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setAssets() {
		activities = new ArrayList<Map<String, Intent>>();
		
		switch(getIntent().getIntExtra(Overlay.TARGET, 0)) {
		case Overlay.Targets.SEARCH_FOR_KEY:
			Map main_s = new HashMap<String, Intent>();
			main_s.put(SearchForKey.Views.MAIN, new Intent(this, SearchForKeyActivity.class));
			
			Map spinner_s = new HashMap<String, Intent>();
			spinner_s.put(SearchForKey.Views.SPINNER, new Intent(this, GPGProgressActivity.class));
			
			activities.add(main_s);
			activities.add(spinner_s);
			break;
		case Overlay.Targets.GENERATE_NEW_KEY:
			Map spinner_g = new HashMap<String, Intent>();
			spinner_g.put(GenerateNewKey.Views.SPINNER, new Intent(this, GPGProgressActivity.class));
			
			Map finish_g = new HashMap<String, Intent>();
			finish_g.put(GenerateNewKey.Views.FINISH, new Intent(this, ConfirmNewKeyActivity.class));
			
			activities.add(spinner_g);
			activities.add(finish_g);
			break;
		}
		
		if(getIntent().getSerializableExtra(Overlay.BOOTSTRAPPED_DATA) != null) {
			String activityTag = activities.get(0).keySet().iterator().next();
			HashMap<String, Object> bootstrap = (HashMap<String, Object>) getIntent().getSerializableExtra(Overlay.BOOTSTRAPPED_DATA);
			
			Iterator<Entry<String, Object>> i = bootstrap.entrySet().iterator();
			while(i.hasNext()) {
				Entry<String, Object> b = i.next();
				activities.get(0).get(activityTag).putExtra(b.getKey(), (Serializable) b.getValue());
			}
		}
		
		group = this;
		lam = getLocalActivityManager();
		root = (LinearLayout) findViewById(R.id.overlayRoot);
		changeView();
	}
	
	public void alert(String msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
	}

}
