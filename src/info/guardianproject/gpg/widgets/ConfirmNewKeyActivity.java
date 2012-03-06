package info.guardianproject.gpg.widgets;

import java.util.ArrayList;
import java.util.HashMap;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.Constants;
import info.guardianproject.gpg.MyKeysGroup;
import info.guardianproject.gpg.OverlayActivity;
import info.guardianproject.gpg.adapters.GPGScreen;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ConfirmNewKeyActivity extends Activity implements OnClickListener, Constants {
	Button editKey, confirmAndExit;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.confirmnewkeyactivity);
		
		setAssets();
	}
	
	@Override
	public void onClick(View v) {
		if(v == editKey) {
			HashMap<String, Object> extra = new HashMap<String, Object>();
			extra.put(MyKeys.ACTION, MyKeys.Actions.GO_TO_EDITOR);
			
			ArrayList<HashMap<String, Object>> extras = new ArrayList<HashMap<String, Object>>();
			extras.add(extra);
			OverlayActivity.group.finishWithResults(extras);
		} else if(v == confirmAndExit) {
			MyKeysGroup.root.changeView(GPGScreen.getViewFromGroup(MyKeysGroup.root.views, MyKeys.VIEWS[0]));
			finish();
		}
	}
	
	public void setAssets() {
		editKey = (Button) findViewById(R.id.editKey);
		editKey.setOnClickListener(this);
		
		confirmAndExit = (Button) findViewById(R.id.confirmAndExit);
		confirmAndExit.setOnClickListener(this);
	}

}
