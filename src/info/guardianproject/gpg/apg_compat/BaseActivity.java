/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.gpg.apg_compat;

import info.guardianproject.gpg.GnuPrivacyGuard;
import info.guardianproject.gpg.R;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class BaseActivity extends Activity
                          implements ProgressDialogUpdater {

    private ProgressDialog mProgressDialog = null;

    private long mSecretKeyId = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handlerCallback(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(Constants.path.app_dir);
            if (!dir.exists() && !dir.mkdirs()) {
                // ignore this for now, it's not crucial
                // that the directory doesn't exist at this point
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.preferences, 0, R.string.menu_settings)
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, Id.menu.option.about, 1, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Id.menu.option.about: {
                showDialog(Id.dialog.about);
                return true;
            }

            case Id.menu.option.search: {
                startSearch("", false, null, false);
                return true;
            }

            default: {
                break;
            }
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // in case it is a progress dialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        switch (id) {
            case Id.dialog.encrypting: {
                mProgressDialog.setMessage(this.getString(R.string.progress_initializing));
                return mProgressDialog;
            }

            case Id.dialog.decrypting: {
                mProgressDialog.setMessage(this.getString(R.string.progress_initializing));
                return mProgressDialog;
            }

            case Id.dialog.saving: {
                mProgressDialog.setMessage(this.getString(R.string.progress_saving));
                return mProgressDialog;
            }

            case Id.dialog.importing: {
                mProgressDialog.setMessage(this.getString(R.string.progress_importing));
                return mProgressDialog;
            }

            case Id.dialog.exporting: {
                mProgressDialog.setMessage(this.getString(R.string.progress_exporting));
                return mProgressDialog;
            }

            case Id.dialog.deleting: {
                mProgressDialog.setMessage(this.getString(R.string.progress_initializing));
                return mProgressDialog;
            }

            case Id.dialog.querying: {
                mProgressDialog.setMessage(this.getString(R.string.progress_querying));
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgressDialog.setCancelable(false);
                return mProgressDialog;
            }

            default: {
                break;
            }
        }
        mProgressDialog = null;

        switch (id) {
            case Id.dialog.about: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("About " + GnuPrivacyGuard.getVersionString(this));

                LayoutInflater inflater =
                        (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.info, null);
                TextView message = (TextView) layout.findViewById(R.id.message);
                message.setText("This is an attempt to bring OpenPGP to Android. " +
                        "It is far from complete, but more features are planned (see website).\n\n" +
                        "Feel free to send bug reports, suggestions, feature requests, feedback, " +
                        "photographs.\n\n" +
                        "mail: thi@thialfihar.org\n" +
                        "site: http://apg.thialfihar.org\n\n" +
                        "This software is provided \"as is\", without warranty of any kind.");
                alert.setView(layout);

                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                BaseActivity.this.removeDialog(Id.dialog.about);
                                            }
                                        });

                return alert.create();
            }

            default: {
                break;
            }
        }

        return super.onCreateDialog(id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Id.request.secret_keys: {
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    setSecretKeyId(bundle.getLong(Apg.EXTRA_KEY_ID));
                } else {
                    setSecretKeyId(Id.key.none);
                }
                break;
            }

            default: {
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(Constants.extras.status, Id.message.progress_update);
        data.putInt(Constants.extras.progress, progress);
        data.putInt(Constants.extras.progress_max, max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public void setProgress(String message, int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(Constants.extras.status, Id.message.progress_update);
        data.putString(Constants.extras.message, message);
        data.putInt(Constants.extras.progress, progress);
        data.putInt(Constants.extras.progress_max, max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public void handlerCallback(Message msg) {
        Bundle data = msg.getData();
        if (data == null) {
            return;
        }

        int type = data.getInt(Constants.extras.status);
        switch (type) {
            case Id.message.progress_update: {
                String message = data.getString(Constants.extras.message);
                if (mProgressDialog != null) {
                    if (message != null) {
                        mProgressDialog.setMessage(message);
                    }
                    mProgressDialog.setMax(data.getInt(Constants.extras.progress_max));
                    mProgressDialog.setProgress(data.getInt(Constants.extras.progress));
                }
                break;
            }

            case Id.message.import_done: // intentionally no break
            case Id.message.export_done: // intentionally no break
            case Id.message.query_done: // intentionally no break
            case Id.message.done: {
                mProgressDialog = null;
                doneCallback(msg);
                break;
            }

            default: {
                break;
            }
        }
    }

    public void doneCallback(Message msg) {

    }

    public void sendMessage(Message msg) {
        mHandler.sendMessage(msg);
    }

    public void setSecretKeyId(long id) {
        mSecretKeyId = id;
    }

    public long getSecretKeyId() {
        return mSecretKeyId;
    }
}
