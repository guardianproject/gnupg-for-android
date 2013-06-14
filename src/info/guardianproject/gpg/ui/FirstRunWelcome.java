package info.guardianproject.gpg.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import info.guardianproject.gpg.R;

public class FirstRunWelcome extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_welcome);

        Button next = (Button) findViewById(R.id.nextButton);
        next.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(FirstRunWelcome.this, FirstRunSetup.class));
            }
        });
    }

}
