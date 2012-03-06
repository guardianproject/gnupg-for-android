package info.guardianproject.gpg.screens;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.Constants;
import info.guardianproject.gpg.MyKeysGroup;
import info.guardianproject.gpg.adapters.GPGScreen;
import android.app.ActivityGroup;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

public class ViewMyKeysActivity extends ActivityGroup implements OnClickListener, Constants {
	ImageButton generateNewKey;
	ListView myKeysList;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewmykeysactivity);
		setAssets();
	}
	
	public void setAssets() {
		generateNewKey = (ImageButton) findViewById(R.id.generateNewKey);
		generateNewKey.setOnClickListener(this);
	
		myKeysList = (ListView) findViewById(R.id.myKeysList);
	}

	@Override
	public void onClick(View v) {
		if(v == generateNewKey) {
			MyKeysGroup.root.changeView(GPGScreen.getViewFromGroup(MyKeysGroup.root.views, GenerateNewKey.TAG));
		}
			
			
	}

}
