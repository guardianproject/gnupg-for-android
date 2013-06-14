package info.guardianproject.gpg.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

import info.guardianproject.gpg.CreateKeyActivity;
import info.guardianproject.gpg.ImportFileActivity;
import info.guardianproject.gpg.R;
import info.guardianproject.gpg.sync.SyncConstants;

public class FirstRunSetup extends Activity  {

    CheckBox integrateBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wizard_keys);

        integrateBox = (CheckBox) findViewById(R.id.integrateCheckBox);
        Button createButton = (Button) findViewById(R.id.createKey);
        Button importButton = (Button) findViewById(R.id.importKeys);
        Button skipButton = (Button) findViewById(R.id.skip);

        createButton.setOnClickListener(createKeys);
        importButton.setOnClickListener(importKeys);
        skipButton.setOnClickListener(skip);
    }

    OnClickListener createKeys = new OnClickListener() {

        @Override
        public void onClick(View v) {
            setIntegratePrefs();
            startActivity(new Intent(FirstRunSetup.this, CreateKeyActivity.class));
        }
    };

    OnClickListener importKeys = new OnClickListener() {

        @Override
        public void onClick(View v) {
            setIntegratePrefs();
            startActivity(new Intent(FirstRunSetup.this, ImportFileActivity.class));
        }
    };

    OnClickListener skip = new OnClickListener() {

        @Override
        public void onClick(View v) {
            setIntegratePrefs();
            startActivity(new Intent(FirstRunSetup.this, MainActivity.class));
        }
    };

    private void setIntegratePrefs() {
        SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(this);
        Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean(SyncConstants.PREFS_INTEGRATE_CONTACTS, integrateBox.isChecked());
        prefsEditor.commit();
    }

}
