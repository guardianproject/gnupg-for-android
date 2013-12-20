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

package info.guardianproject.gpg;

import info.guardianproject.gpg.GpgApplication.Action;
import info.guardianproject.gpg.apg_compat.Apg;
import info.guardianproject.gpg.sync.GpgContactManager;
import info.guardianproject.gpg.sync.RawGpgContact;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class KeyListContactsAdapter extends BaseAdapter {
    public static final String TAG = "KeyListContactsAdapter";

    protected LayoutInflater mInflater;
    protected ListView mParent;
    protected String mSearchString;
    protected long mSelectedKeyIds[];

    private RawGpgContact[] mContacts;

    public KeyListContactsAdapter(ListView parent, String action, String searchString,
            long selectedKeyIds[]) {
        Context c = parent.getContext();
        mParent = parent;
        mSearchString = searchString;
        mSelectedKeyIds = selectedKeyIds;

        mInflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        List<RawGpgContact> contacts = GpgContactManager.getAllContacts(c,
                GpgApplication.mSyncAccount);
        if (action == null || action.equals(Action.SHOW_PUBLIC_KEYS)
                || action.equals(Action.SELECT_PUBLIC_KEYS)) {
            Log.v(TAG, "showing all keys");
            mContacts = contacts.toArray(new RawGpgContact[contacts.size()]);
        } else if (action.equals(Action.SHOW_SECRET_KEYS)
                || action.equals(Action.SELECT_SECRET_KEYS)) {
            Log.v(TAG, "showing only secret keys");
            // TODO this should be implemented using the Group secret_key_group_name
            List<RawGpgContact> secretKeys = new ArrayList<RawGpgContact>();
            for (RawGpgContact r : contacts)
                if (r.isSecret || r.hasSecretKey)
                    secretKeys.add(r);
            mContacts = secretKeys.toArray(new RawGpgContact[secretKeys.size()]);
        }
        if (mContacts == null) {
            Log.e(TAG, "keyArray is null");
        }
    }

    @Override
    public boolean isEnabled(int position) {
        RawGpgContact key = mContacts[position];
        return (!key.isDisabled
                && !key.isExpired
                && !key.isRevoked
                && !key.isInvalid);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        if (mContacts == null)
            return 0;
        else
            return mContacts.length;
    }

    @Override
    public Object getItem(int position) {
        RawGpgContact key = mContacts[position];
        String[] ret = new String[3];
        ret[0] = key.name;
        ret[1] = key.email;
        ret[2] = key.comment;
        return ret;
    }

    @Override
    public long getItemId(int position) {
        String keyId = mContacts[position].keyID;
        return new BigInteger(keyId, 16).longValue(); // MASTER_KEY_ID
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RawGpgContact key = mContacts[position];
        View view = mInflater.inflate(R.layout.key_list_item, null);
        boolean usable = isEnabled(position);

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        TextView status = (TextView) view.findViewById(R.id.status);

        TextView keyId0 = (TextView) view.findViewById(R.id.keyId0);
        TextView keyId1 = (TextView) view.findViewById(R.id.keyId1);
        TextView keyId2 = (TextView) view.findViewById(R.id.keyId2);
        TextView keyId3 = (TextView) view.findViewById(R.id.keyId3);

        // set text color based on the web colors generate from fingerprint
        int[] keyIdColors = GpgApplication.genFingerprintColor(key.keyID);

        keyId0.setText(key.keyID.substring(0, 4));
        keyId1.setText(key.keyID.substring(4, 8));
        keyId2.setText(key.keyID.substring(8, 12));
        keyId3.setText(key.keyID.substring(12));

        keyId0.setTextColor(keyIdColors[0]);
        keyId1.setTextColor(keyIdColors[1]);
        keyId2.setTextColor(keyIdColors[2]);
        keyId3.setTextColor(keyIdColors[3]);

        mainUserId.setText(key.name);
        mainUserIdRest.setText(key.email);
        status.setText(R.string.unknownStatus);

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

        if (usable)
            status.setText(""); // default state
        else if (key.isDisabled)
            status.setText(R.string.disabled);
        else if (key.isExpired)
            status.setText(R.string.expired);
        else if (key.isInvalid)
            status.setText(R.string.invalid);
        else if (key.isRevoked)
            status.setText(R.string.revoked);
        else
            status.setText(R.string.noKey);

        status.setText(status.getText() + " ");

        if (!usable) {
            mParent.setItemChecked(position, false);
        }

        view.setEnabled(usable);
        mainUserId.setEnabled(usable);
        mainUserIdRest.setEnabled(usable);
        status.setEnabled(usable);

        return view;
    }
}
