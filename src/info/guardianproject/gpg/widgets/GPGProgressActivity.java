package info.guardianproject.gpg.widgets;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.Constants;
import info.guardianproject.gpg.OverlayActivity;
import info.guardianproject.gpg.action.InterfaceActions;
import info.guardianproject.gpg.adapters.GPGHeaderTextView;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GPGProgressActivity extends Activity implements Constants {
	GPGHeaderTextView inheritedTitle;
	HashMap<String,Object> results;
	Handler finish;
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.spinneractivity);
		
		inheritedTitle = (GPGHeaderTextView) findViewById(R.id.inherited_title);
		if(getIntent().hasExtra(GPGProgressDialog.INHERITED_TITLE))
			inheritedTitle.setText(getIntent().getStringExtra(GPGProgressDialog.INHERITED_TITLE));
		else
			((LinearLayout) inheritedTitle.getParent()).removeView((View) inheritedTitle);
		
		if(getIntent().hasExtra(GPGProgressDialog.ACTION)) {
			results = new HashMap<String, Object>();
			ArrayList<HashMap<String, Object>> actions = (ArrayList<HashMap<String, Object>>) getIntent().getSerializableExtra(GPGProgressDialog.ACTION);
			
			for(HashMap<String, Object> action : actions) {
				runThread(action);
			}
			
			OverlayActivity.group.handleResult(getIntent(), results);
			finish();
		} else
			finish();
	}
	
	private void runThread(HashMap<String, Object> action) {
		String[] paramStrings = ((String) action.get(GPGProgressDialog.Actions.PARAMETERS)).split(";");
		Class<?>[] paramClasses = new Class<?>[paramStrings.length];
		Object[] params = new Object[paramStrings.length];
		
		int index = 0;
		
		for(String param : paramStrings) {			
			try{
				int p = Integer.parseInt(param);
				paramClasses[index] = int.class;
				params[index] = p;
			} catch(NumberFormatException i) {
				try {
					float p = Float.parseFloat(param);
					paramClasses[index] = float.class;
					params[index] = p;
				} catch(NumberFormatException f) {
					paramClasses[index] = String.class;
					params[index] = param;
				}
			}
			
			index++;
		}
		
		try {
			Method m = InterfaceActions.class.getDeclaredMethod((String) action.get(GPGProgressDialog.Actions.THREAD), paramClasses);
			m.setAccessible(true);
			
			Object res = Class.forName(m.getReturnType().getName()).newInstance();
			results.put((String) action.get(GPGProgressDialog.Actions.THREAD), m.invoke(res, params));
		} catch (SecurityException e) {
			Log.d(LOG, e.toString());
		} catch (NoSuchMethodException e) {
			Log.d(LOG, e.toString());
		} catch (IllegalArgumentException e) {
			Log.d(LOG, e.toString());
		} catch (IllegalAccessException e) {
			Log.d(LOG, e.toString());
		} catch (InvocationTargetException e) {
			Log.d(LOG, e.toString());
		} catch (ClassNotFoundException e) {
			Log.d(LOG, e.toString());
		} catch (InstantiationException e) {
			Log.d(LOG, e.toString());
		}
		
	}
	
	@Override
	public void onBackPressed() {
		OverlayActivity.group.cancelGroup();
	}
}
