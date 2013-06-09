
package info.guardianproject.gpg.pinentry;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import info.guardianproject.gpg.NativeHelper;
import info.guardianproject.gpg.R;

/**
 * Activity for communicating with the native pinentry.
 *
 * To achieve the nice dialog overlay, this Activity should be started with
 *      Intent.FLAG_ACTIVITY_NEW_TASK);
 *      Intent.FLAG_ACTIVITY_CLEAR_TOP);
 *      Intent.FLAG_ACTIVITY_NO_HISTORY);
 * @author user
 *
 */
public class PinEntryActivity extends Activity {

    static final String TAG = "PinEntryActivity";

    private PinentryStruct pinentry;
    private EditText pinEdit;
    private TextView description;
    private TextView title;
    private Button okButton;
    private Button cancelButton;

    private int app_uid;

    private OnClickListener okClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setPin();
            syncNotify();
        }
    };

    private OnClickListener cancelClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            syncNotify();
            finish();
        }
    };

    static {
        System.load("/data/data/info.guardianproject.gpg/lib/libpinentry.so");
    }

    private native void connectToGpgAgent(int uid);

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinentry);
        NativeHelper.setup(this);

        Bundle params = getIntent().getExtras();
        final int uid = params.getInt("uid", -1);

        if( uid < 0 ) {
            Log.e(TAG, "missing uid. aborting");
            finish();
            return;
        }
        app_uid = uid;

        description = (TextView) findViewById(R.id.description);
        title = (TextView) findViewById(R.id.title);
        okButton = (Button) findViewById(R.id.okButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        pinEdit = (EditText) findViewById(R.id.pinEdit);

        okButton.setOnClickListener(okClickListener);
        cancelButton.setOnClickListener(cancelClickListener);

        pinentry = (PinentryStruct) getLastNonConfigurationInstance();
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        updateViews();

    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {

            @Override
            public void run() {
                // This function does major magic
                // it blocks (hence the thread)
                // when it returns it means gpg-agent is no longer communicating
                // with us
                // so we quit. we don't like gpg-agent anyways. neaner.
                connectToGpgAgent(app_uid);
                finish();
            }

        }).start();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return pinentry;
    }

    @Override
    protected void onStop() {
        syncNotify();
        finish();
        super.onDestroy();
    }

    private synchronized void syncNotify() {
        notify();
    }

    private synchronized void setPin() {
        if (pinentry == null) {
            return;
        }
        pinentry.pin = pinEdit.getText().toString();
    }

    private synchronized void updateViews() {
        if (pinentry != null) {
            if (pinentry.title != null) {
                title.setText(pinentry.title);
                title.setVisibility(View.VISIBLE);
            } else {
                title.setText("");
                title.setVisibility(View.GONE);
            }
            if (pinentry.description != null) {
                description.setText(pinentry.description);
                description.setVisibility(View.VISIBLE);
            } else {
                description.setText("");
                description.setVisibility(View.GONE);
            }
        }
    }

    PinentryStruct setPinentryStruct(PinentryStruct s) {

        synchronized (this) {
            pinentry = s;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateViews();
            }
        });

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return pinentry;
        }
    }
}
