package info.guardianproject.gpg;

import java.io.File;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class TerminalEmulatorPathReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

        String packageName = context.getPackageName();

        String action = intent.getAction();

        if (action.equals("jackpal.androidterm.broadcast.APPEND_TO_PATH")) {
            /* The directory we want appended goes into the result extras */
            Bundle result = getResultExtras(true);

            /**
             * By convention, entries are indexed by package name.
             *
             * If you need to impose an ordering constraint for some reason,
             * you may prepend a number to your package name -- for example,
             * 50-com.example.awesomebin or 00-net.busybox.android.
             */
            File aliases = new File(NativeHelper.app_opt.getAbsolutePath(), "aliases");
            String pathToAppend = aliases.getAbsolutePath();
            result.putString(packageName, pathToAppend);

            setResultCode(Activity.RESULT_OK);
        }
	}
}
