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

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

import info.guardianproject.gpg.apg_compat.Apg;

import java.util.Vector;

public class KeyListFragment extends SherlockFragment {

    protected ListView mListView;
    protected KeyListAdapter mListAdapter;
    protected View mFilterLayout;
    protected Button mClearFilterButton;
    protected Button mOkButton;
    protected Button mCancelButton;
    protected TextView mFilterInfo;
    protected View mKeyListButtonBar;
    protected boolean mShowButtons = true;

    private String mCurrentAction;
    private Bundle mCurrentExtras;

    private OnKeysSelectedListener mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View result = inflater.inflate(R.layout.key_list_fragment, container, false);
        return result;
    }

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
        mListView = (ListView) getView().findViewById(R.id.list);

        String action = getArguments().getString("action");
        if (action == null || action.equals(""))
            mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        else if (action.equals(Apg.Intent.SELECT_PUBLIC_KEYS))
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        else if (action.equals(Apg.Intent.SELECT_SECRET_KEY)) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            mListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    String[] userId = (String[]) mListView.getItemAtPosition(position);
                    mCallback.onKeySelected(id, Apg.userId(userId));
                }
            });
        }

        mOkButton = (Button) getView().findViewById(R.id.btn_ok);
        mOkButton.setOnClickListener(okClicked);

        mCancelButton = (Button) getView().findViewById(R.id.btn_cancel);
        mCancelButton.setOnClickListener(cancelClicked);
        mFilterLayout = getView().findViewById(R.id.layout_filter);
        mFilterInfo = (TextView) mFilterLayout.findViewById(R.id.filterInfo);
        mClearFilterButton = (Button) mFilterLayout.findViewById(R.id.btn_clear);
        mKeyListButtonBar = getView().findViewById(R.id.keyListButtonBar);

        mClearFilterButton.setOnClickListener(clearFilterclicked);
        updateButtons();
        registerReceiver();
        handleIntent(action, getArguments().getBundle("extras"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    public void toggleButtons(boolean visible) {
        mShowButtons = visible;
        updateButtons();
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

        if (searchString == null) {
            mFilterLayout.setVisibility(View.GONE);
        } else {
            mFilterLayout.setVisibility(View.VISIBLE);
            mFilterInfo.setText(getString(R.string.filterInfo_format, searchString));
        }

        mListAdapter = new KeyListAdapter(mListView, action,
                searchString, selectedKeyIds);
        mListView.setAdapter(mListAdapter);

        if (selectedKeyIds != null) {
            for (int i = 0; i < mListAdapter.getCount(); ++i) {
                long keyId = mListAdapter.getItemId(i);
                for (int j = 0; j < selectedKeyIds.length; ++j) {
                    if (keyId == selectedKeyIds[j]) {
                        mListView.setItemChecked(i, true);
                        break;
                    }
                }
            }
        }
    }

    private void updateButtons() {
        if (mOkButton != null && mCancelButton != null) {
            mOkButton.setVisibility(mShowButtons ? View.VISIBLE : View.INVISIBLE);
            mCancelButton.setVisibility(mShowButtons ? View.VISIBLE : View.INVISIBLE);
            mKeyListButtonBar.setVisibility(mShowButtons ? View.VISIBLE : View.INVISIBLE);
        }
    }

    OnClickListener cancelClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mCallback.onKeySelectionCanceled();
        }
    };
    OnClickListener okClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Vector<Long> keys = new Vector<Long>();
            Vector<String> userIds = new Vector<String>();
            for (int i = 0; i < mListView.getCount(); ++i) {
                if (mListView.isItemChecked(i)) {
                    keys.add(mListView.getItemIdAtPosition(i));
                    String userId[] = (String[]) mListView.getItemAtPosition(i);
                    userIds.add(Apg.userId(userId));
                }
            }
            long selectedKeyIds[] = new long[keys.size()];
            for (int i = 0; i < keys.size(); ++i) {
                selectedKeyIds[i] = keys.get(i);
            }

            String userIdArray[] = new String[0];
            mCallback.onKeysSelected(selectedKeyIds, userIds.toArray(userIdArray));
        }
    };

    OnClickListener clearFilterclicked = new OnClickListener() {

        @Override
        public void onClick(View v) {
            handleIntent("", new Bundle());
        }
    };

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
