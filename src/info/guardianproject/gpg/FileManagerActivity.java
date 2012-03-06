package info.guardianproject.gpg;

import info.guardianproject.gpg.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class FileManagerActivity extends Activity implements Constants, OnClickListener {
	ImageButton folderNavigation;
	TextView folderName, fileManagerAction;
	ListView fileList;
	
	Bundle b;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.filemanageractivity);
		
		setAssets();
		
		
	}
	
	@Override
	public void onClick(View v) {
		if(v == folderNavigation) {
			
		}
		
	}
	
	private void setAssets() {
		folderNavigation = (ImageButton) findViewById(R.id.folderNavigation);
		folderNavigation.setOnClickListener(this);
		
		fileManagerAction = (TextView) findViewById(R.id.fileManagerAction);
		String fileManagerTitle = null;
		
		switch(getIntent().getIntExtra(FileManager.ACTION, 0)) {
		case FileManager.Actions.IMPORT_KEY:
			fileManagerTitle = getResources().getString(R.string.fileManager_importKey_title);
			break;
		default:
			fileManagerTitle = getResources().getString(R.string.fileManager_default_title);
			break;
		}
		
		fileManagerAction.setText(fileManagerTitle);
		folderName = (TextView) findViewById(R.id.folderName);
		
		fileList = (ListView) findViewById(R.id.fileList);
	}

}