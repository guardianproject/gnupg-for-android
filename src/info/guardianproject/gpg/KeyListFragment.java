/*
 * Copyright (C) 2013 Abel Luck <abel@guardianproject.info>
 * Copyright (C) 2013 Hans-Christoph Steiner <hans@guardianproject.info>
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

package info.guardianproject.gpg;

import info.guardianproject.gpg.GpgApplication.Action;
import info.guardianproject.gpg.MainActivity.Tabs;
import info.guardianproject.gpg.apg_compat.Apg;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.openintents.openpgp.keyserver.HkpKeyServer;
import org.openintents.openpgp.keyserver.KeyServer.KeyInfo;
import org.openintents.openpgp.keyserver.KeyServer.QueryException;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class KeyListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<KeyserverResult<List<KeyInfo>>> {
    public static final String TAG = "KeyListFragment";

    protected ListView mListView;
    protected ListAdapter mShowKeysAdapter = null;
    protected KeyListKeyserverAdapter mKeyserverAdapter = null;
    protected String mAction;
    protected ActionMode mActionMode;
    private String mCurrentAction;
    private Bundle mCurrentExtras;

    private ActionBarActivity mCurrentActivity;

    private final HashSet<Integer> mSelectedPositions = new HashSet<Integer>();

    /**
     * Fragment -> Activity communication
     * https://developer.android.com/training/
     * basics/fragments/communicating.html
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCurrentActivity = (ActionBarActivity) activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.search_hint));
        setHasOptionsMenu(true);
        mListView = getListView();

        mAction = getArguments().getString("action");
        Log.i(TAG, "onActivityCreated  action: " + mAction);
        if (mAction == null || mAction.equals(Action.SHOW_PUBLIC_KEYS)) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        } else if (mAction.equals(Action.FIND_KEYS)) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        } else if (mAction.equals(Action.SELECT_PUBLIC_KEYS)) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        } else if (mAction.equals(Action.SELECT_SECRET_KEYS)) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        } else {
            mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        }
        registerReceiver();
        handleIntent(mAction, getArguments().getBundle("extras"));
        LoaderManager lm = getLoaderManager();
        if (mAction.equals(Action.FIND_KEYS) && lm.getLoader(Tabs.FIND_KEYS) != null) {
            lm.initLoader(Tabs.FIND_KEYS, null, this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    public void handleIntent(String action, Bundle extras) {
        // Why doesn't java have default parameters :(
        if (extras == null)
            extras = new Bundle();

        mCurrentAction = action;
        mCurrentExtras = extras;

        String searchString = null;
        if (Intent.ACTION_SEARCH.equals(action)) {
            searchString = extras.getString(SearchManager.QUERY);
            if (searchString != null && searchString.trim().length() == 0) {
                searchString = null;
            }
        }

        long selectedKeyIds[] = null;
        selectedKeyIds = extras.getLongArray(Apg.EXTRA_SELECTION);

        if (selectedKeyIds == null) {
            Vector<Long> vector = new Vector<Long>();
            for (int i = 0; i < mListView.getCount(); ++i) {
                if (mListView.isItemChecked(i)) {
                    vector.add(mListView.getItemIdAtPosition(i));
                }
            }
            selectedKeyIds = new long[vector.size()];
            for (int i = 0; i < vector.size(); ++i) {
                selectedKeyIds[i] = vector.get(i);
            }
        }

        if (action.equals(Action.FIND_KEYS)) {
            Log.d(TAG, "action: " + Action.FIND_KEYS);
            if (mKeyserverAdapter == null)
                mKeyserverAdapter = new KeyListKeyserverAdapter(mListView, searchString);
            setListAdapter(mKeyserverAdapter);
        } else {
            Log.d(TAG, "action: other");
            mShowKeysAdapter = new KeyListContactsAdapter(mListView, action, searchString,
                    selectedKeyIds);
            setListAdapter(mShowKeysAdapter);
            if (selectedKeyIds != null) {
                for (int i = 0; i < mShowKeysAdapter.getCount(); ++i) {
                    long keyId = mShowKeysAdapter.getItemId(i);
                    for (int j = 0; j < selectedKeyIds.length; ++j) {
                        if (keyId == selectedKeyIds[j]) {
                            mListView.setItemChecked(i, true);
                            break;
                        }
                    }
                }
            }
        }
    }

    public interface OnKeysSelectedListener {
        public void onKeySelected(long id, String userId);

        public void onKeysSelected(long selectedKeyIds[], String selectedUserIds[]);

        public void onKeySelectionCanceled();
    }

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(GpgApplication.BROADCAST_ACTION_KEYLIST_CHANGED)) {
                // refresh keylist
                Log.d("KeyListFragment", "BROADCAST_ACTION_KEYLIST_CHANGED");
                handleIntent(mCurrentAction, mCurrentExtras);
            }
        }
    };

    private void registerReceiver() {
        Log.d("KeyListFragment", "register!");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mBroadcastReceiver,
                new IntentFilter(GpgApplication.BROADCAST_ACTION_KEYLIST_CHANGED));
    }

    private void unregisterReceiver() {
        Log.d("KeyListFragment", "unregister");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mBroadcastReceiver);
    }

    void restartLoader() {
        getLoaderManager().restartLoader(Tabs.FIND_KEYS, null, this);
    }

    @Override
    public Loader<KeyserverResult<List<KeyInfo>>> onCreateLoader(int id, Bundle args) {
        Log.v(TAG, "Loader<List<KeyInfo>> onCreateLoader " + id);
        Loader<KeyserverResult<List<KeyInfo>>> loader = new KeyserverLoader(getActivity());
        // the AsyncTaskLoader won't start without this here
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<KeyserverResult<List<KeyInfo>>> loader,
            KeyserverResult<List<KeyInfo>> result) {
        Log.v(TAG, "onLoadFinished");
        List<KeyInfo> data = result.getData();
        if (data != null) {
            mKeyserverAdapter.setData(data);
        } else {
            int resid = result.getErrorResid();
            // TODO really, resid should never be wrong, wtf?
            if (resid > 0) {
                Toast.makeText(getActivity(), resid, Toast.LENGTH_LONG).show();
                setEmptyText(getString(resid));
            } else {
                Log.w(TAG, "resid <= 0?");
            }
        }
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<KeyserverResult<List<KeyInfo>>> loader) {
        Log.v(TAG, "onLoaderReset");
        mKeyserverAdapter.setData(null);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mSelectedPositions.contains(position))
            mSelectedPositions.remove(position);
        else
            mSelectedPositions.add(position);

        if (mActionMode == null
                && (mAction.equals(Action.FIND_KEYS)
                        || mAction.equals(Action.SELECT_SECRET_KEYS)
                        || mAction.equals(Action.SELECT_PUBLIC_KEYS)))
            mActionMode = mCurrentActivity.startSupportActionMode(mActionModeCallback);
    }

    ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            if (mAction.equals(Action.FIND_KEYS)) {
                inflater.inflate(R.menu.find_keys_context_menu, menu);
            } else {
                inflater.inflate(R.menu.encrypt_file_to_context_menu, menu);
            }
            return true;
        }

        // Called each time the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.select_keys:
                    long[] selectedKeyIds = new long[mSelectedPositions.size()];
                    String[] selectedEmails = new String[selectedKeyIds.length];
                    int p = 0;
                    for (Integer position : mSelectedPositions) {
                        selectedKeyIds[p] = mListView.getItemIdAtPosition(position);
                        String[] itemStrings = (String[]) mListView.getItemAtPosition(position);
                        selectedEmails[p] = itemStrings[1];
                        p++;
                    }
                    Intent data = new Intent();
                    data.putExtra(Intent.EXTRA_UID, selectedKeyIds);
                    data.putExtra(Intent.EXTRA_EMAIL, selectedEmails);
                    mCurrentActivity.setResult(Activity.RESULT_OK, data);
                    mode.finish();
                    return true;
                case R.id.download:
                    Log.i(TAG, "download the keys!");
                    mCurrentActivity.setSupportProgressBarIndeterminateVisibility(true);
                    new AsyncTask<Void, Void, Void>() {
                        final Context context = getActivity().getApplicationContext();
                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(context);
                        final String host = prefs.getString(GpgPreferenceActivity.PREF_KEYSERVER,
                                "ipv4.pool.sks-keyservers.net");
                        final long[] selected = mListView.getCheckedItemIds();
                        final Intent intent = new Intent(context, ImportFileActivity.class);

                        @Override
                        protected Void doInBackground(Void... params) {
                            HkpKeyServer keyserver = new HkpKeyServer(host);
                            String keys = "";
                            for (int i = 0; i < selected.length; i++) {
                                Log.v(TAG, "id: " + selected[i]);
                                try {
                                    keys += keyserver.get(selected[i]);
                                } catch (QueryException e) {
                                    e.printStackTrace();
                                }
                            }
                            // An Intent can carry very much data, so write it
                            // to a cached file
                            try {
                                File privateFile = File.createTempFile("keyserver", ".pkr",
                                        mCurrentActivity.getCacheDir());
                                FileUtils.writeStringToFile(privateFile, keys);
                                intent.setType(getString(R.string.pgp_keys));
                                intent.setAction(Intent.ACTION_SEND);
                                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(privateFile));
                            } catch (IOException e) {
                                e.printStackTrace();
                                // try sending the key data inline in the Intent
                                intent.setType("text/plain");
                                intent.setAction(Intent.ACTION_SEND);
                                intent.putExtra(Intent.EXTRA_TEXT, keys);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void v) {
                            GpgApplication.triggerContactsSync();
                            mCurrentActivity.setSupportProgressBarIndeterminateVisibility(false);
                            mode.finish(); // Action picked, so close the CAB
                            startActivity(intent);
                        }
                    }.execute();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mListView.clearChoices();
            for (int i : mSelectedPositions)
                mListView.setItemChecked(i, false);
            mSelectedPositions.clear();
        }
    };
}
