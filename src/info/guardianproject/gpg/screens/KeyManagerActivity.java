package info.guardianproject.gpg.screens;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.Constants;
import info.guardianproject.gpg.FileManagerActivity;
import info.guardianproject.gpg.OverlayActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

public class KeyManagerActivity extends Activity implements Constants, OnClickListener {
	ImageButton importKey, lookupContact, searchForKey;
	ListView keyManagerList;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keymanageractivity);
        
        setAssets();
    }

	@Override
	public void onClick(View v) {
		if(v == importKey) {
			Intent i = new Intent(this, FileManagerActivity.class)
				.putExtra(FileManager.ACTION, FileManager.Actions.IMPORT_KEY);
			startActivityForResult(i, FileManager.REQUEST_CODE);
		} else if(v == lookupContact) {
			
		} else if(v == searchForKey) {
			Intent i = new Intent(this, OverlayActivity.class)
				.putExtra(Overlay.TARGET, Overlay.Targets.SEARCH_FOR_KEY);
			startActivityForResult(i, Overlay.REQUEST_CODE);
		}
	}
	
	private void setAssets() {
		importKey = (ImageButton) findViewById(R.id.importKey);
		importKey.setOnClickListener(this);
		
		lookupContact = (ImageButton) findViewById(R.id.lookupContact);
		lookupContact.setOnClickListener(this);
		
		searchForKey = (ImageButton) findViewById(R.id.searchForKey);
		searchForKey.setOnClickListener(this);
		
		keyManagerList = (ListView) findViewById(R.id.keyManagerList);
	}
	
	@SuppressWarnings({"unchecked"})
	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		if(result == Activity.RESULT_OK) {
			switch(request) {
			case FileManager.REQUEST_CODE:
				break;
			case Overlay.REQUEST_CODE:
				HashMap<String, Object> returnedValues = (HashMap<String, Object>) data.getExtras().getSerializable(Overlay.RESULT_DATA);
				Iterator<Entry<String, Object>> i = returnedValues.entrySet().iterator();
				while(i.hasNext()) {
					Entry<String, Object> returnedValue = i.next();
					Log.d(LOG, returnedValue.toString());
				}
					
				break;
			}
		}
	}

}