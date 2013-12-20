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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.freiheit.gnupg.GnuPGKey;

import info.guardianproject.gpg.GpgApplication.Action;
import info.guardianproject.gpg.apg_compat.Apg;

import java.math.BigInteger;
import java.util.Locale;

public class KeyListGnuPGKeyAdapter extends BaseAdapter {
    public static final String TAG = "KeyListGnuPGKeyAdapter";

    protected LayoutInflater mInflater;
    protected ListView mParent;
    protected String mSearchString;
    protected long mSelectedKeyIds[];

    private GnuPGKey[] mKeyArray;

    public KeyListGnuPGKeyAdapter(ListView parent, String action,
            String searchString, long selectedKeyIds[]) {
        mParent = parent;
        mSearchString = searchString;
        mSelectedKeyIds = selectedKeyIds;

        mInflater = (LayoutInflater) parent.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        if (action == null || action.equals(Action.SHOW_PUBLIC_KEYS)
                || action.equals(Action.SELECT_PUBLIC_KEYS)) {
            mKeyArray = GnuPG.context.listKeys();
        } else if (action.equals(Action.SHOW_SECRET_KEYS)
                || action.equals(Action.SELECT_SECRET_KEYS)) {
            mKeyArray = GnuPG.context.listSecretKeys();
        }
        if (mKeyArray == null) {
            Log.e(TAG, "keyArray is null");
        }
    }

    @Override
    public boolean isEnabled(int position) {
        GnuPGKey key = mKeyArray[position];
        return (!key.isDisabled()
                && !key.isExpired()
                && !key.isRevoked()
                && !key.isInvalid());
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        if (mKeyArray == null)
            return 0;
        else
            return mKeyArray.length;
    }

    @Override
    public Object getItem(int position) {
        GnuPGKey key = mKeyArray[position];
        String[] ret = new String[3];
        ret[0] = key.getName();
        ret[1] = key.getEmail();
        ret[2] = key.getComment();
        return ret;
    }

    @Override
    public long getItemId(int position) {
        String keyId = mKeyArray[position].getKeyID();
        return new BigInteger(keyId, 16).longValue(); // MASTER_KEY_ID
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GnuPGKey key = mKeyArray[position];
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
        String keyId = key.getKeyID().toLowerCase(Locale.ENGLISH);
        int[] keyIdColors = GpgApplication.genFingerprintColor(keyId);

        keyId0.setText(keyId.substring(0, 4));
        keyId1.setText(keyId.substring(4, 8));
        keyId2.setText(keyId.substring(8, 12));
        keyId3.setText(keyId.substring(12));

        keyId0.setTextColor(keyIdColors[0]);
        keyId1.setTextColor(keyIdColors[1]);
        keyId2.setTextColor(keyIdColors[2]);
        keyId3.setTextColor(keyIdColors[3]);

        mainUserId.setText(key.getName());
        mainUserIdRest.setText(key.getEmail());
        status.setText(R.string.unknownStatus);

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

        if (usable)
            status.setText(R.string.usable);
        else if (key.isDisabled())
            status.setText(R.string.disabled);
        else if (key.isExpired())
            status.setText(R.string.expired);
        else if (key.isInvalid())
            status.setText(R.string.invalid);
        else if (key.isRevoked())
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
