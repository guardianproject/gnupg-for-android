
package info.guardianproject.gpg;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

public class Gpg2TaskFragment extends DialogFragment {
    public static final String TAG = "Gpg2TaskFragment";
    public static final int GPG2_TASK_FINISHED = 0;

    Messenger mMessenger;
    Gpg2Task mGpg2Task;
    String mGpg2Args;
    ProgressBar mProgressBar;

    public void configTask(Messenger messenger, Gpg2Task task, String args)
    {
        mMessenger = messenger;
        mGpg2Task = task;
        mGpg2Args = args;
        // tell AsyncTask to call updateProgress() and taskFinished() on this
        mGpg2Task.setFragment(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this instance so it isn't destroyed when MainActivity and
        // MainFragment change configuration.
        setRetainInstance(true);
        if (mGpg2Task != null)
            mGpg2Task.execute(mGpg2Args);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        getDialog().setCanceledOnTouchOutside(false);

        return view;
    }

    // This is to work around what is apparently a bug. If you don't have it
    // here the dialog will be dismissed on rotation, so tell it not to
    // dismiss.
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mGpg2Task != null)
            mGpg2Task.cancel(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // if the Gpg2Task finished while the user was away, dismiss this
        if (mGpg2Task == null)
            dismiss();
    }

    // This is also called by the AsyncTask.
    public void taskFinished(Integer exitvalue) {
        // check if resumed because it will crash if trying to dismiss the
        // dialog after the user has switched to another app.
        if (isResumed())
            dismiss();

        // If this isn't resumed, setting the task to null will allow us to
        // dimiss ourselves in onResume().
        mGpg2Task = null;

        sendMessageToHandler(GPG2_TASK_FINISHED, exitvalue);
    }

    /**
     * Send message back to handler which is initialized in a activity
     * 
     * @param what Message integer you want to send
     * @param exitvalue the POSIX exit value of the gpg2 process
     */
    private void sendMessageToHandler(Integer what, Integer exitvalue) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = exitvalue;
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(TAG, "Messenger is null!", e);
        }
    }

    public static class Gpg2Task extends AsyncTask<String, Void, Integer> {
        public static final String TAG = "Gpg2Task";
        Gpg2TaskFragment mFragment;
        int mProgress = 0;

        void setFragment(Gpg2TaskFragment fragment) {
            mFragment = fragment;
        }

        @Override
        protected Integer doInBackground(String... params) {
            return GnuPG.gpg2(params[0]);
        }

        @Override
        protected void onPostExecute(Integer exitvalue) {
            if (mFragment == null)
                return;
            mFragment.taskFinished(exitvalue);
        }
    }
}
