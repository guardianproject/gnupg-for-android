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

import java.util.List;
import java.util.Vector;

import org.openintents.openpgp.keyserver.KeyServer.KeyInfo;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;


public class KeyListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<KeyserverResult<List<KeyInfo>>> {
    public static final String TAG = "KeyListFragment";

    protected ListView mListView;
    protected ListAdapter mShowKeysAdapter = null;
    protected KeyListKeyserverAdapter mKeyserverAdapter = null;
    private String mCurrentAction;
    private Bundle mCurrentExtras;

    private OnKeysSelectedListener mCallback;

    /**
     * Fragment -> Activity communication
     * https://developer.android.com/training/
     * basics/fragments/communicating.html
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnKeysSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnKeysSelectedListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.search_hint));
        setHasOptionsMenu(true);
        mListView = getListView();

        String action = getArguments().getString("action");
        if (action == null) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        } else if (action.equals(Action.SELECT_PUBLIC_KEYS) || action.equals(Action.FIND_KEYS)) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        } else if (action.equals(Action.SELECT_SECRET_KEYS)) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            mListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    String[] userId = (String[]) mListView.getItemAtPosition(position);
                    mCallback.onKeySelected(id, Apg.userId(userId));
                }
            });
        } else {
            mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        }
        registerReceiver();
        handleIntent(action, getArguments().getBundle("extras"));
        LoaderManager lm = getLoaderManager();
        if (action.equals(Action.FIND_KEYS) && lm.getLoader(Tabs.FIND_KEYS) != null) {
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
            if (mShowKeysAdapter == null)
                mShowKeysAdapter = new KeyListContactsAdapter(mListView, action, searchString,
                        selectedKeyIds);
            setListAdapter(mShowKeysAdapter);
            if (selectedKeyIds != null) {
                ListAdapter adapter = getListAdapter();
                for (int i = 0; i < adapter.getCount(); ++i) {
                    long keyId = adapter.getItemId(i);
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
            Toast.makeText(getActivity(), result.getErrorResid(), Toast.LENGTH_LONG).show();
            setEmptyText(getString(result.getErrorResid()));
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

}
