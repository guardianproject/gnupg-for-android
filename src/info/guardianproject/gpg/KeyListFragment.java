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
import info.guardianproject.gpg.apg_compat.Apg;

import java.util.Vector;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

public class KeyListFragment extends SherlockListFragment {
    public static final String TAG = "KeyListFragment";

    protected ListView mListView;

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
            //setListAdapter(new KeyListKeyserverAdapter(mListView, searchString));
        } else {
            setListAdapter(new KeyListContactsAdapter(mListView, action, searchString,
                    selectedKeyIds));
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

}
