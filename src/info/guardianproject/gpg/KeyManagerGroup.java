package info.guardianproject.gpg;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.adapters.GPGScreen;

import java.util.ArrayList;

import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class KeyManagerGroup  extends ActivityGroup implements Constants {
	ArrayList<GPGScreen> history;
	LocalActivityManager lam;
	
	public ArrayList<GPGScreen> views;
	public static KeyManagerGroup root;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setAssets();
	}
	
	public void setAssets() {
		history = new ArrayList<GPGScreen>();
		views = new ArrayList<GPGScreen>();
		int i = 0;
		for(String s : KeyManager.VIEWS)
			views.add(new GPGScreen(s, new Intent(this, KeyManager.TARGETS[i++])));
		
		lam = getLocalActivityManager();
		
		root = this;
		
		changeView(GPGScreen.getViewFromGroup(views, KeyManager.VIEWS[0]));
		
		
	}
	
	public void changeView(GPGScreen screen) {
		history.add(screen);
		View newView = lam.startActivity(screen.label, screen.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)).getDecorView();
		setContentView(newView);
	}

}