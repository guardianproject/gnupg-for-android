package info.guardianproject.gpg.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.Constants;
import info.guardianproject.gpg.OverlayActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class SearchForKeyActivity extends Activity implements Constants, OnClickListener {
	Button searchForKey_button;
	EditText searchForKey_editText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searchforkeyactivity);
		
		setAssets();
	}
	
	@Override
	public void onClick(View v) {
		if(v == searchForKey_button) {
			if(searchForKey_editText.getText().toString().compareTo("") != 0) {
				ArrayList<HashMap<String, Object>> actions = new ArrayList<HashMap<String, Object>>();
				
				HashMap<String, Object> action = new HashMap<String, Object>();
				action.put(GPGProgressDialog.Actions.THREAD, SearchForKey.Actions.SEARCH_FOR_KEY);
				action.put(GPGProgressDialog.Actions.PARAMETERS, searchForKey_editText.getText().toString());
				actions.add(action);
				
				Map<String, Intent> spinnerIntent = OverlayActivity.group.getNextIntent();
				spinnerIntent.get(spinnerIntent.keySet().iterator().next())
					.putExtra(GPGProgressDialog.INHERITED_TITLE, getResources().getString(R.string.searching))
					.putExtra(GPGProgressDialog.ACTION, actions);
				
				InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE); 
				inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); 
					
				finish();
			} else
				OverlayActivity.group.alert(getResources().getString(R.string.searchForKey_noInput));
		}
		
	}
	
	private void setAssets() {
		searchForKey_button = (Button) findViewById(R.id.searchForKey_button);
		searchForKey_button.setOnClickListener(this);
		
		searchForKey_editText = (EditText) findViewById(R.id.searchForKey_editText);
	}
	
	@Override
	public void onBackPressed() {
		OverlayActivity.group.cancelGroup();
	}
}
