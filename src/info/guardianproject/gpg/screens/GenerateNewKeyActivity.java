package info.guardianproject.gpg.screens;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.Constants;
import info.guardianproject.gpg.OverlayActivity;
import info.guardianproject.gpg.render.DateParser;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public class GenerateNewKeyActivity extends Activity implements Constants, OnClickListener {
	Spinner keyType, keyLength;
	LinearLayout keyType_holder, keyLength_holder;
	EditText fullName, email, comment;
	ImageButton addPhoto;
	DatePicker expirationDate;
	Button generateKey;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.generatenewkeyactivity);
		
		setAssets();
	}
	
	@Override
	public void onClick(View v) {
		if(v == addPhoto) {
			
		} else if(v == generateKey) {
			if(
				fullName.getText().length() > 0 &&
				email.getText().length() > 0 &&
				keyType.getSelectedItemPosition() != Spinner.INVALID_POSITION &&
				keyLength.getSelectedItemPosition() != Spinner.INVALID_POSITION
			) {
				try {
					HashMap<String, Object> bootstrap = new HashMap<String, Object>();
					bootstrap.put(GPGProgressDialog.INHERITED_TITLE, getResources().getString(R.string.generating));
					
					ArrayList<HashMap<String, Object>> actions = new ArrayList<HashMap<String, Object>>();
					HashMap<String, Object> action = new HashMap<String, Object>();
					action.put(GPGProgressDialog.Actions.THREAD, GenerateNewKey.Actions.GENERATE_NEW_KEY);
					action.put(GPGProgressDialog.Actions.PARAMETERS, gatherInputs());
					actions.add(action);
					
					bootstrap.put(GPGProgressDialog.ACTION, actions);
					
					Intent i = new Intent(this, OverlayActivity.class)
						.putExtra(Overlay.BOOTSTRAPPED_DATA, bootstrap)
						.putExtra(Overlay.TARGET, Overlay.Targets.GENERATE_NEW_KEY);
					getParent().startActivityForResult(i, Overlay.REQUEST_CODE);
				} catch (ParseException e) {}
			} else
				Toast.makeText(this, getResources().getString(R.string.generateNewKey_error), Toast.LENGTH_LONG).show();
		}
		
	}
	
	private String gatherInputs() throws ParseException {
		String template = GPG.Commands.BATCH_GEN;
		template = template.replace(GPG.Replace.NAME_REAL, fullName.getText().toString());
		template = template.replace(GPG.Replace.NAME_EMAIL, email.getText().toString());
		template = template.replace(GPG.Replace.NAME_COMMENT, comment.getText().toString());
		template = template.replace(GPG.Replace.KEY_TYPE, keyType.getSelectedItem().toString());
		template = template.replace(GPG.Replace.KEY_LENGTH, keyLength.getSelectedItem().toString());
		template = template.replace(GPG.Replace.EXPIRE_DATE, Long.toString(
				DateParser.getDateAsMillis(expirationDate.getDayOfMonth(), expirationDate.getMonth(), expirationDate.getYear())));
		return template; 
	}
	
	public void setAssets() {
		fullName = (EditText) findViewById(R.id.fullName);
		email = (EditText) findViewById(R.id.email);
		comment = (EditText) findViewById(R.id.comment);
		
		keyType_holder = (LinearLayout) findViewById(R.id.keyType_holder);
		keyType = new Spinner(GenerateNewKeyActivity.this.getParent());
		keyType.setPrompt(getResources().getString(R.string.generateNewKey_keyType));
		keyType.setAdapter(new ArrayAdapter<String>(getParent(), R.layout.spinner_no_icon, R.id.spinnerItemText, getResources().getStringArray(R.array.keyType_names)));
		keyType.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		keyType_holder.addView(keyType);
			
		keyLength_holder = (LinearLayout) findViewById(R.id.keyLength_holder);
		keyLength = new Spinner(GenerateNewKeyActivity.this.getParent());
		keyLength.setPrompt(getResources().getString(R.string.generateNewKey_keyLength));
		keyLength.setAdapter(new ArrayAdapter<String>(getParent(), R.layout.spinner_no_icon, R.id.spinnerItemText, getResources().getStringArray(R.array.keyLength_names)));

		keyLength.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		keyLength_holder.addView(keyLength);
		
		expirationDate = (DatePicker) findViewById(R.id.expirationDate);
		
		addPhoto = (ImageButton) findViewById(R.id.addPhoto);
		addPhoto.setOnClickListener(this);
		
		generateKey = (Button) findViewById(R.id.generateKey);
		generateKey.setOnClickListener(this);
		
	}

}
