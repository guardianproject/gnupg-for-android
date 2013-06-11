package info.guardianproject.gpg.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
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

import info.guardianproject.gpg.KeyListAdapter;
import info.guardianproject.gpg.R;
import info.guardianproject.gpg.apg_compat.Apg;

import java.util.Vector;

public class KeyListFragment extends SherlockFragment {
    protected ListView mListView;
    protected KeyListAdapter mListAdapter;
    protected View mFilterLayout;
    protected Button mClearFilterButton;
    protected TextView mFilterInfo;

    private OnKeysSelectedListener mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View result = inflater.inflate(R.layout.key_list, container, false);

//        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        mListView = (ListView) result.findViewById(R.id.list);

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
                    String[] userId = (String[])mListView.getItemAtPosition(position);
                    mCallback.onKeySelected(id, Apg.userId(userId));
                }
            });
        }

        Button okButton = (Button) result.findViewById(R.id.btn_ok);
        okButton.setOnClickListener(okClicked);

        Button cancelButton = (Button) result.findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(cancelClicked);
        mFilterLayout = result.findViewById(R.id.layout_filter);
        mFilterInfo = (TextView) mFilterLayout.findViewById(R.id.filterInfo);
        mClearFilterButton = (Button) mFilterLayout.findViewById(R.id.btn_clear);

        mClearFilterButton.setOnClickListener(clearFilterclicked);

        handleIntent(action, getArguments().getBundle("extras"));
        return result;
    }

    /**
     * Fragment -> Activity communication
     * https://developer.android.com/training/basics/fragments/communicating.html
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

    private void handleIntent(String action, Bundle extras) {
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
            mFilterInfo.setText(getString(R.string.filterInfo, searchString));
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

    OnClickListener cancelClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mCallback.onKeySelectionCanceled();
        }
    };
    OnClickListener okClicked = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent data = new Intent();
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

}
