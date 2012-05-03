package info.guardianproject.gpg.screens;

import java.util.HashMap;
import java.util.Map;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.mods.G4DatePicker;
import info.guardianproject.gpg.utils.Constants;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

public class GenerateNewKeyActivity extends Fragment implements Constants, OnClickListener {
	EditText fullName, emailAddress, comment;
	Spinner keyType, keyLength;
	G4DatePicker expirationDate;
	ImageButton addPhoto;
	Button generateMyKey;
	
	HashMap<String, Object> preferences;
	Activity a;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		preferences = new HashMap<String, Object>();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater li, ViewGroup container, Bundle savedInstanceState) {
		Log.d(LOG, "gen new key container is: " + container.getId());
		View view = li.inflate(R.layout.generate_new_key_activity, container, false);
		
		fullName = (EditText) view.findViewById(R.id.fullName);
		emailAddress = (EditText) view.findViewById(R.id.email);
		comment = (EditText) view.findViewById(R.id.comment);
		
		keyType = (Spinner) view.findViewById(R.id.keyType);
		keyLength = (Spinner) view.findViewById(R.id.keyLength);
		
		expirationDate = (G4DatePicker) view.findViewById(R.id.expirationDate);
		addPhoto = (ImageButton) view.findViewById(R.id.addPhoto);
		addPhoto.setOnClickListener(this);
		
		generateMyKey = (Button) view.findViewById(R.id.generateKey);
		generateMyKey.setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		a = activity;
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
	}
	
	private boolean infoIsValid() {
		if(!(fullName.getText().length() >= 5)) {
			Toast.makeText(a, getString(R.string.generateNewKey_fail_fullName), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		if(!(emailAddress.getText().length() > 4 && emailAddress.getText().toString().contains("@"))) {
			Toast.makeText(a, getString(R.string.generateNewKey_fail_email), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		if(!expirationDate.isValidDate(G4DatePicker.IS_AFTER_TODAY)) {
			Toast.makeText(a, getString(R.string.generateNewKey_fail_expirationDate), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		return true;
	}
	
	private void gatherPreferences() {
		preferences.put(Keys.Primary.FULL_NAME, fullName.getText().toString());
		preferences.put(Keys.Primary.EMAIL_ADDRESS, emailAddress.getText().toString());
		if(comment.getText().toString().compareTo("") != 0)
			preferences.put(Keys.Primary.COMMENT, comment.getText().toString());
		preferences.put(Keys.Primary.KEY_TYPE, keyType.getSelectedItemPosition());
		preferences.put(Keys.Primary.KEY_LENGTH, keyLength.getSelectedItemPosition());
		preferences.put(Keys.Primary.EXPIRY, new int[] {expirationDate.Month(), expirationDate.Day(), expirationDate.Year()});
	}

	@Override
	public void onClick(View v) {
		if(v == generateMyKey) {
			if(infoIsValid())
				gatherPreferences();
				a.sendBroadcast(new Intent()
					.setAction(GenerateNewKey.Actions.GENERATE_NEW_KEY)
					.putExtra(GenerateNewKey.Actions.GENERATE_NEW_KEY, preferences)
				);
		} else if(v == addPhoto) {
			Intent intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
			startActivityForResult(intent, GenerateNewKey.Intents.ADD_PHOTO);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == Activity.RESULT_OK) {
			switch(requestCode) {
			case GenerateNewKey.Intents.ADD_PHOTO:
				preferences.put(Keys.Primary.ASSOC_PHOTO, data.getData().toString());
			}
		}
	}
	
	
	
}
