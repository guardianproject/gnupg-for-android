package info.guardianproject.gpg.screens;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.Constants;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

public class KeyEditorActivity extends Activity implements OnClickListener, Constants {
	Button saveKey, addEmail, changeExpirationDate, revoke;
	ImageButton addSubkey;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.keyeditoractivity);
	}
	
	@Override
	public void onClick(View v) {
		if(v == saveKey) {
			
		}
		
	}
	
	public void setAssets() {
		saveKey = (Button) findViewById(R.id.saveKey);
		saveKey.setOnClickListener(this);
	}

}
