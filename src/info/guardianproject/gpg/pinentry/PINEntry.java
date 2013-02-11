package info.guardianproject.gpg.pinentry;

import info.guardianproject.gpg.R;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class PINEntry extends Activity {
	public static String SOCKET_ADDRESS = "info.guardianproject.gpg.pinentryhelper";


	public Handler mHandler;

	private EditText pinEdit;
	private TextView statusView;
	private Button okButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        statusView = (TextView) findViewById(R.id.statusLabel);
        okButton = (Button) findViewById(R.id.okButton);
        pinEdit = (EditText) findViewById(R.id.pinEdit);

        okButton.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
			}
		});

        setContentView(R.layout.activity_pinentry);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pinentry, menu);
        return true;
    }
}
