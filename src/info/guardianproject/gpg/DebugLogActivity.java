
package info.guardianproject.gpg;

import java.io.File;
import java.io.OutputStream;
import java.util.Calendar;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnCreateContextMenuListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.freiheit.gnupg.GnuPGKey;

public class DebugLogActivity extends ActionBarActivity implements OnCreateContextMenuListener {
    public static final String TAG = "DebugLogActivity";

    private ScrollView consoleScroll;
    private TextView consoleText;

    public static final String LOG_UPDATE = "LOG_UPDATE";
    public static final String COMMAND_FINISHED = "COMMAND_FINISHED";

    private CommandThread commandThread;
    private BroadcastReceiver logUpdateReceiver;
    private BroadcastReceiver commandFinishedReceiver;
    public String command;

    private FileDialogFragment mFileDialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_log_activity);
        consoleScroll = (ScrollView) findViewById(R.id.consoleScroll);
        consoleText = (TextView) findViewById(R.id.consoleText);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceivers();
        GpgApplication.startGpgAgent(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceivers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.debug_log_activity, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Activity Result: " + requestCode + " " + resultCode);
        if (resultCode == RESULT_CANCELED || data == null)
            return;

        switch (requestCode) {
            case GpgApplication.FILENAME: {
                if (resultCode == RESULT_OK) {
                    try {
                        String path = data.getData().getPath();
                        Log.d(TAG, "path=" + path);

                        // set filename used in export/import dialogs
                        mFileDialog.setFilename(path);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Nullpointer while retrieving path!", e);
                    }
                }
                return;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Context context = getApplicationContext();
        // Set a popup EditText view to get user input
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        alert.setView(input);

        switch (item.getItemId()) {
            case R.id.menu_settings_key:
                startActivity(new Intent(this, GpgPreferenceActivity.class));
                return true;
            case R.id.menu_create_key:
                startActivity(new Intent(this, CreateKeyActivity.class));
                return true;
            case R.id.menu_receive_key:
                alert.setTitle(R.string.receive_key);
                alert.setMessage(R.string.receive_key_message);
                alert.setPositiveButton("Receive", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(context);
                        String ks = prefs.getString(GpgPreferenceActivity.PREF_KEYSERVER,
                                "200.144.121.45");
                        command = NativeHelper.gpg2
                                + " --keyserver " + ks + " --recv-keys "
                                + input.getText().toString();
                        commandThread = new CommandThread();
                        commandThread.start();
                    }
                });
                alert.show();
                return true;
            case R.id.menu_send_key:
                alert.setTitle(R.string.send_key);
                alert.setMessage(R.string.send_key_message);
                alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(context);
                        String ks = prefs.getString(GpgPreferenceActivity.PREF_KEYSERVER,
                                "200.144.121.45");
                        command = NativeHelper.gpg2
                                + " --keyserver " + ks + " --send-keys "
                                + input.getText().toString();
                        commandThread = new CommandThread();
                        commandThread.start();
                    }
                });
                alert.show();
                return true;
            case R.id.menu_change_passphrase:
                GnuPGKey[] keys = GnuPG.context.listSecretKeys();
                if (keys != null && keys.length > 0) {
                    final GnuPGKey key = keys[0];
                    String msg = String.format(getString(R.string.changing_passphrase_for_format),
                                key.getName() + " <" + key.getEmail() + ">:" + key.getKeyID());
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... params) {
                            GnuPG.context.changePassphrase(key);
                            return null;
                        }
                    }.execute();
                } else {
                    Toast.makeText(this, R.string.error_no_secret_key, Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.menu_run_test:
                command = NativeHelper.app_opt + "/tests/run-tests-with-password.sh";
                commandThread = new CommandThread();
                commandThread.start();
                Log.i(TAG, "finished " + command);
                return true;
            case R.id.menu_decrypt_file:
                final String decryptFilename = (NativeHelper.app_opt.getAbsolutePath()
                        + "/tests/icon.png.gpg");
                showDecryptFile(decryptFilename);
                return true;
            case R.id.menu_import_key_from_file:
                final String defaultFilename = (NativeHelper.app_opt.getAbsolutePath()
                        + "/tests/public-keys.pkr");
                showImportFromFileDialog(defaultFilename);
                return true;
            case R.id.menu_export_keys_to_file:
                final String exportFilename = Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/gnupg-keyring.pkr";
                showExportToFileDialog(exportFilename);
                return true;
            case R.id.menu_share_log:
                shareTestLog();
                Log.i(TAG, "finished menu_share_log");
                return true;
        }
        return false;
    }

    /**
     * Show dialog to select file to decrypt
     */
    public void showDecryptFile(final String defaultFilename) {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OK) {
                    Bundle data = message.getData();
                    File f = new File(data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME));
                    final String decryptFilename = f.getAbsolutePath();
                    final int lastPeriodPos = decryptFilename.lastIndexOf('.');
                    String outputFilename = decryptFilename.substring(0, lastPeriodPos);

                    command = NativeHelper.gpg2 + "--output " + outputFilename
                            + " --decrypt " + decryptFilename;
                    commandThread = new CommandThread();
                    commandThread.start();
                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        new Runnable() {
            @Override
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(messenger,
                        getString(R.string.title_decrypt_file),
                        getString(R.string.dialog_specify_decrypt_file), defaultFilename,
                        null, GpgApplication.FILENAME);

                mFileDialog.show(getSupportFragmentManager(), "fileDialog");
            }
        }.run();
    }

    /**
     * Show dialog to choose a file to import keys from
     */
    public void showImportFromFileDialog(final String defaultFilename) {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OK) {
                    Bundle data = message.getData();
                    String importFilename = new File(
                            data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME))
                            .getAbsolutePath();
                    boolean deleteAfterImport = data
                            .getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED);

                    Log.d(TAG, "importFilename: " + importFilename);
                    Log.d(TAG, "deleteAfterImport: " + deleteAfterImport);
                    command = NativeHelper.gpg2 + " --import " + importFilename;
                    commandThread = new CommandThread();
                    commandThread.start();
                    if (deleteAfterImport)
                        new File(importFilename).delete();
                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        new Runnable() {
            @Override
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(messenger,
                        getString(R.string.title_import_keys),
                        getString(R.string.dialog_specify_import_file_msg), defaultFilename,
                        null, GpgApplication.FILENAME);

                mFileDialog.show(getSupportFragmentManager(), "fileDialog");
            }
        }.run();
    }

    /**
     * Show dialog to choose a file to export keys to
     */
    public void showExportToFileDialog(final String defaultFilename) {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OK) {
                    Bundle data = message.getData();
                    String exportFilename = new File(
                            data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME))
                            .getAbsolutePath();
                    boolean exportSecretKeys = data
                            .getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED);

                    Log.d(TAG, "exportFilename: " + exportFilename);
                    Log.d(TAG, "exportSecretKeys: " + exportSecretKeys);
                    command = NativeHelper.gpg2 + " --batch ";
                    String extension = null;
                    if (exportSecretKeys) {
                        command += " --export-secret-keys ";
                        extension = ".skr";
                    } else {
                        command += " --export ";
                        extension = ".pkr";
                    }
                    // force the right file extension
                    final int lastPeriodPos = exportFilename.lastIndexOf('.');
                    final int lastSlashPos = exportFilename.lastIndexOf('/');
                    if (lastPeriodPos == -1 || lastPeriodPos < lastSlashPos)
                        // if the name has no extension, just tack it on to the
                        // end
                        exportFilename += extension;
                    else
                        exportFilename = exportFilename.substring(0, lastPeriodPos) + extension;

                    final File exportFile = new File(exportFilename);
                    if (exportFile.exists()) {
                        Calendar now = Calendar.getInstance();
                        File newPath = new File(exportFile + "."
                                + String.valueOf(now.getTimeInMillis()));
                        exportFile.renameTo(newPath);
                        Toast.makeText(
                                getBaseContext(),
                                String.format(getString(R.string.renamed_existing_file_format),
                                        newPath),
                                Toast.LENGTH_LONG).show();
                    }

                    command += " --output " + exportFilename;
                    commandThread = new CommandThread();
                    commandThread.start();
                }
            }
        };
        final Messenger messenger = new Messenger(returnHandler);
        new Runnable() {
            @Override
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(messenger,
                        getString(R.string.title_export_keys),
                        getString(R.string.dialog_specify_export_file_msg), defaultFilename,
                        getString(R.string.label_export_secret_keys), GpgApplication.FILENAME);

                mFileDialog.show(getSupportFragmentManager(), "fileDialog");
            }
        }.run();
    }

    private void updateLog() {
        final String logContents = NativeHelper.log.toString();
        if (logContents != null && logContents.trim().length() > 0)
            consoleText.setText(logContents);
        consoleScroll.scrollTo(0, consoleText.getHeight());
    }

    class CommandThread extends Thread {
        private LogUpdate logUpdate;

        @Override
        public void run() {
            logUpdate = new LogUpdate();
            try {
                File dir = new File(NativeHelper.app_opt, "bin");
                Process sh = Runtime.getRuntime().exec("/system/bin/sh",
                        NativeHelper.envp, dir);
                OutputStream os = sh.getOutputStream();

                DebugStreamThread it = new DebugStreamThread(sh.getInputStream(), logUpdate);
                DebugStreamThread et = new DebugStreamThread(sh.getErrorStream(), logUpdate);

                it.start();
                et.start();

                Log.i(TAG, command);
                writeCommand(os, command);
                writeCommand(os, "exit");

                sh.waitFor();
                Log.i(TAG, "Done!");
            } catch (Exception e) {
                Log.e(TAG, "Error!!!", e);
            } finally {
                synchronized (DebugLogActivity.this) {
                    commandThread = null;
                }
                sendBroadcast(new Intent(COMMAND_FINISHED));
            }
        }
    }

    class LogUpdate extends DebugStreamThread.StreamUpdate {

        StringBuffer sb = new StringBuffer();

        @Override
        public void update(String val) {
            NativeHelper.log.append(val);
            sendBroadcast(new Intent(LOG_UPDATE));
        }
    }

    public static void writeCommand(OutputStream os, String command) throws Exception {
        os.write((command + "\n").getBytes("ASCII"));
    }

    private void registerReceivers() {
        logUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateLog();
            }
        };
        registerReceiver(logUpdateReceiver, new IntentFilter(DebugLogActivity.LOG_UPDATE));

        commandFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(COMMAND_FINISHED)) {
                    if (command.contains("--import") || command.contains("--recv-keys")) {
                        Log.d(TAG, "Import complete.");
                        GpgApplication.sendKeylistChangedBroadcast(DebugLogActivity.this);
                    }
                }
            }
        };
        registerReceiver(commandFinishedReceiver, new IntentFilter(
                DebugLogActivity.COMMAND_FINISHED));
    }

    private void unregisterReceivers() {
        if (logUpdateReceiver != null)
            unregisterReceiver(logUpdateReceiver);

        if (commandFinishedReceiver != null)
            unregisterReceiver(commandFinishedReceiver);
    }

    protected void shareTestLog() {
        Intent i = new Intent(android.content.Intent.ACTION_SEND);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "test log from " + getString(R.string.app_name));
        i.putExtra(Intent.EXTRA_TEXT,
                "Attached is an log sent by " + getString(R.string.app_name)
                        + ".  For more info, see:\n"
                        + "https://guardianproject.info/code/gnupg\n\n"
                        + "manufacturer: " + Build.MANUFACTURER + "\n"
                        + "model: " + Build.MODEL + "\n"
                        + "product: " + Build.PRODUCT + "\n"
                        + "brand: " + Build.BRAND + "\n"
                        + "device: " + Build.DEVICE + "\n"
                        + "board: " + Build.BOARD + "\n"
                        + "ID: " + Build.ID + "\n"
                        + "CPU ABI: " + Build.CPU_ABI + "\n"
                        + "release: " + Build.VERSION.RELEASE + "\n"
                        + "incremental: " + Build.VERSION.INCREMENTAL + "\n"
                        + "codename: " + Build.VERSION.CODENAME + "\n"
                        + "SDK: " + Build.VERSION.SDK_INT + "\n"
                        + "\n\nlog:\n----------------------------------\n"
                        + consoleText.getText().toString()
                );
        startActivity(Intent.createChooser(i, getString(R.string.dialog_share_file_using)));
    }
}
