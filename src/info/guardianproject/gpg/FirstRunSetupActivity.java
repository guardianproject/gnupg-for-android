package info.guardianproject.gpg;

import info.guardianproject.gpg.sync.SyncConstants;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

public class FirstRunSetupActivity extends Activity  {

    private final static String TAG = FirstRunSetupActivity.class.getSimpleName();

    private static final int REQUEST_GENKEY    = 0x501;
    private static final int REQUEST_IMPORTKEY = 0x502;

    private CheckBox integrateBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.first_run_setup_activity);

        integrateBox = (CheckBox) findViewById(R.id.integrateCheckBox);
        Button createButton = (Button) findViewById(R.id.createKey);
        Button importButton = (Button) findViewById(R.id.importKeys);
        Button skipButton = (Button) findViewById(R.id.skip);

        createButton.setOnClickListener(createKeys);
        importButton.setOnClickListener(importKeys);
        skipButton.setOnClickListener(skip);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Activity Result: " + requestCode + " " + resultCode);

        switch( requestCode ) {
            case REQUEST_GENKEY: {
                Log.d(TAG, "REQUEST_GENKEY");
                if (resultCode == RESULT_OK) {
                    startActivity( new Intent(this, MainActivity.class) );
                }
                return;
            }
            case REQUEST_IMPORTKEY: {
                Log.d(TAG, "REQUEST_IMPORTKEY");
                if (resultCode == RESULT_OK) {
                    startActivity( new Intent(this, MainActivity.class) );
                }
                return;
            }
        }
    }

    OnClickListener createKeys = new OnClickListener() {

        @Override
        public void onClick(View v) {
            setIntegratePrefs();
            startActivityForResult(new Intent(FirstRunSetupActivity.this, CreateKeyActivity.class), REQUEST_GENKEY );
        }
    };

    OnClickListener importKeys = new OnClickListener() {

        @Override
        public void onClick(View v) {
            setIntegratePrefs();
            startActivityForResult(new Intent(FirstRunSetupActivity.this, ImportFileActivity.class), REQUEST_IMPORTKEY);
        }
    };

    OnClickListener skip = new OnClickListener() {

        @Override
        public void onClick(View v) {
            setIntegratePrefs();
            startActivity(new Intent(FirstRunSetupActivity.this, MainActivity.class));
        }
    };

    private void setIntegratePrefs() {
        SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(this);
        Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean(SyncConstants.PREFS_INTEGRATE_CONTACTS, integrateBox.isChecked());
        prefsEditor.commit();
    }

}
