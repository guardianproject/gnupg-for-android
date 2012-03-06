package info.guardianproject.gpg;

import info.guardianproject.gpg.Constants.Overlay;
import info.guardianproject.gpg.adapters.GPGScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MyKeysGroup  extends ActivityGroup implements Constants {
	ArrayList<GPGScreen> history;
	LocalActivityManager lam;
	
	public ArrayList<GPGScreen> views;
	public static MyKeysGroup root;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setAssets();
	}
	
	public void setAssets() {
		history = new ArrayList<GPGScreen>();
		views = new ArrayList<GPGScreen>();
		
		int i = 0;
		for(String s : MyKeys.VIEWS)
			views.add(new GPGScreen(s, new Intent(this, MyKeys.TARGETS[i++])));
		
		lam = getLocalActivityManager();
		
		root = this;
		
		changeView(GPGScreen.getViewFromGroup(views, MyKeys.VIEWS[0]));
	}
	
	public void changeView(GPGScreen screen) {
		if(history.size() == 0)
			history.add(screen);
		else {
			if(screen.label.compareTo(history.get(history.size() - 1).label) != 0)
				history.add(screen);
		}
		
		for(GPGScreen screens : history)
			Log.d(LOG, screens.label + " : " + screens.intent);
			
		View newView = lam.startActivity(screen.label, screen.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)).getDecorView();
		setContentView(newView);
	}
	
	@Override
	public void onBackPressed() {
		if(history.size() > 1) {
			history.remove(history.get(history.size() - 1));
			changeView(history.get(history.size() - 1)); 
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK && requestCode == Overlay.REQUEST_CODE) {
			Log.d(LOG,"retuned from activity with data");
			HashMap<String, Object> returnedValues = (HashMap<String, Object>) data.getExtras().getSerializable(Overlay.RESULT_DATA);
			Iterator<Entry<String, Object>> i = returnedValues.entrySet().iterator();
			while(i.hasNext()) {
				Entry<String, Object> returnedValue = i.next();
				if(returnedValue.getKey().compareTo(MyKeys.ACTION) == 0) {
					
					switch(Integer.parseInt(returnedValue.getValue().toString())) {
					case MyKeys.Actions.GO_TO_EDITOR:
						changeView(GPGScreen.getViewFromGroup(views, KeyEditor.TAG));
						break;
					}
					
				}
				Log.d(LOG, returnedValue.toString());
				
				// do something with returned values
			}
			
			
			//putExtra(Keys.KEY_ID, "TESTKEYIDABC1234");
			
			
		}
	}

}