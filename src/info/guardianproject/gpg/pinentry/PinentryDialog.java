package info.guardianproject.gpg.pinentry;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import info.guardianproject.gpg.NativeHelper;
import info.guardianproject.gpg.R;

public class PinentryDialog extends DialogFragment {
    private final static String TAG = PinentryDialog.class.getSimpleName();

    static {
        System.load("/data/data/info.guardianproject.gpg/lib/libpinentry.so");
    }

    private PinentryCallback mCallback;
    private PinentryStruct pinentry;
    private EditText pinEdit;
    private TextView description;
    private TextView error;
    private TextView title;
    private Button okButton;
    private Button notOkButton;
    private Button cancelButton;

    private int app_uid;

    private boolean oneButton;
    private boolean promptingPin;

    private OnClickListener pinEnterClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setPin();
            syncNotify();
        }
    };

    private OnClickListener notOkClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setNotOked(true);
            syncNotify();
        }
    };

    private OnClickListener cancelClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setCanceled(true);
            syncNotify();
        }
    };

    private OnClickListener okClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setCanceled(false);
            syncNotify();
        }
    };



    private native void connectToGpgAgent(int uid);

    /** Called when the activity is first created. */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.activity_pinentry, container);

        Bundle params = getArguments();
        final int uid = params.getInt("uid", -1);

        if( uid < 0 ) {
            Log.e(TAG, "missing uid. aborting");
            return null;
        }
        app_uid = uid;

        description = (TextView) view.findViewById(R.id.description);
        error = (TextView) view.findViewById(R.id.error);
        title = (TextView) view.findViewById(R.id.title);
        okButton = (Button) view.findViewById(R.id.okButton);
        notOkButton = (Button) view.findViewById(R.id.notOkButton);
        cancelButton = (Button) view.findViewById(R.id.cancelButton);
        pinEdit = (EditText) view.findViewById(R.id.pinEdit);

        updateViews();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);
        mCallback = (PinentryCallback) getActivity();
        NativeHelper.setup(getActivity());
    }

    @Override
    public void onResume() {
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
                mCallback.onPinentryDialogClosed();
            }

        }).start();
    }

    @Override
    public void onStop() {
        syncNotify();
        mCallback.onPinentryDialogClosed();
        super.onDestroy();
    }

    private synchronized void syncNotify() {
        notify();
    }

    private synchronized void setPin() {
        if (pinentry == null) return;
        pinentry.pin    = pinEdit.getText().toString();
        pinentry.result = pinEdit.getText().length();
    }

    private synchronized void setNotOked( boolean notoked ) {
        if (pinentry == null) return;
        pinentry.canceled = notoked ? 0 : 1;
    }
    private synchronized void setCanceled( boolean canceled ) {
        if (pinentry == null) return;
        if(canceled)          pinentry.result = -1;
    }

    private synchronized void updateTitle() {
        if (pinentry.title.length() != 0 ) {
            title.setText(pinentry.title);
            title.setVisibility(View.VISIBLE);
        } else {
            title.setText("");
            title.setVisibility(View.GONE);
        }
    }

    private synchronized void updateDesc() {
        if (pinentry.description.length() != 0 ) {
            description.setText(pinentry.description);
            description.setVisibility(View.VISIBLE);
        } else {
            description.setText("");
            description.setVisibility(View.GONE);
        }
        if (pinentry.error.length() != 0 ) {
            error.setText(pinentry.error);
            error.setVisibility(View.VISIBLE);
        } else {
            error.setText("");
            error.setVisibility(View.GONE);
        }
    }

    private synchronized void setButton(boolean pinPrompt, boolean oneButton) {
        if( pinentry.ok.length() != 0 ) {
            okButton.setText(pinentry.ok);
        } else {
            okButton.setText(pinentry.default_ok);
        }
        if( pinentry.cancel.length() != 0 ) {
            cancelButton.setText(pinentry.cancel);
        } else {
            cancelButton.setText(pinentry.default_cancel);
        }
    }

    private synchronized void updateOkButton() {
        if( this.promptingPin ) {
            okButton.setOnClickListener(pinEnterClickListener);
        } else {
            okButton.setOnClickListener(okClickListener);
        }
        if( this.oneButton ) {
            okButton.setVisibility(View.GONE);
        } else {
            okButton.setVisibility(View.VISIBLE);
        }

        if( pinentry.ok.length() != 0 ) {
            okButton.setText(fix(pinentry.ok));
        } else {
            okButton.setText(fix(pinentry.default_ok));
        }
    }

    private synchronized void updateCancelButton() {
        cancelButton.setOnClickListener(cancelClickListener);
        okButton.setVisibility(View.VISIBLE);
        if( pinentry.cancel.length() != 0 ) {
            cancelButton.setText(fix(pinentry.cancel));
        } else {
            cancelButton.setText(fix(pinentry.default_cancel));
        }
    }

    private synchronized void updateNotOkButton() {
        notOkButton.setOnClickListener(notOkClickListener);
        if( this.promptingPin ) {
            notOkButton.setVisibility(View.GONE);
            return;
        }
        if( this.oneButton ) {
            notOkButton.setVisibility(View.GONE);
            return;
        }

        if( pinentry.notok.length() != 0 ) {
            notOkButton.setText(fix(pinentry.notok));
            notOkButton.setVisibility(View.VISIBLE);
        } else {
            notOkButton.setVisibility(View.GONE);
        }
    }

    private synchronized void updatePinEdit() {
        if( this.promptingPin ) {
            pinEdit.setVisibility(View.VISIBLE);
            return;
        } else {
            pinEdit.setVisibility(View.GONE);
        }
    }

    /*
     * The strings gpg-agent sends us include accelerator markers
     * these are underscores and on the desktop they enable the
     * ALT+X shortcuts actions. We remove them.
     */
    private String fix( String str ) {
        // double underscores are escaped underscores
        return str.replace("__", "ILoveHotSauce").replace("_", "").replace("ILoveHotSauce", "_");
    }

    private synchronized void updateView() {
        updateTitle();
        updateDesc();
        updateOkButton();
        updateCancelButton();
        updateNotOkButton();
        updatePinEdit();
    }

    private synchronized void updateViews() {
        if (pinentry != null) {
            promptingPin = pinentry.isButtonBox != 0;
            oneButton = pinentry.one_button == 0;
            updateView();
        }
    }

    PinentryStruct setPinentryStruct(PinentryStruct s) {

        synchronized (this) {
            pinentry = s;
        }
        getActivity().runOnUiThread(new Runnable() {
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

    public interface PinentryCallback {
        void onPinentryDialogClosed();
    }
}
