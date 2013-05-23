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

import info.guardianproject.gpg.GPGApplication;
import info.guardianproject.gpg.NativeHelper;
import info.guardianproject.gpg.R;

import java.math.BigInteger;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.freiheit.gnupg.GnuPGContext;
import com.freiheit.gnupg.GnuPGKey;

public class SelectPublicKeyListAdapter extends BaseAdapter {
    protected LayoutInflater mInflater;
    protected ListView mParent;
    protected String mSearchString;
    protected Activity mActivity;
    protected long mSelectedKeyIds[];

    private GnuPGContext mCtx = null;
    private GnuPGKey[] mKeyArray;

    public SelectPublicKeyListAdapter(Activity activity, ListView parent,
                                      String searchString, long selectedKeyIds[]) {
        mActivity = activity;
        mParent = parent;
        mSearchString = searchString;
        mSelectedKeyIds = selectedKeyIds;

        mInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mCtx = new GnuPGContext();
        // set the homeDir option to our custom home location
        mCtx.setEngineInfo(mCtx.getProtocol(), mCtx.getFilename(),
        		NativeHelper.app_home.getAbsolutePath());
        mKeyArray = mCtx.listKeys();
		if (mKeyArray == null) {
			Log.e(GPGApplication.TAG, "keyArray is null");
		}
    }

    @Override
    public boolean isEnabled(int position) {
// TODO this should check whether the key is valid for use, eg. not expired, revoked, etc.
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public int getCount() {
        return mKeyArray.length;
    }

    public Object getItem(int position) {
        return mKeyArray[position].getEmail(); // USER_ID
    }

    public long getItemId(int position) {
        String keyId = mKeyArray[position].getKeyID();
        return new BigInteger(keyId, 16).longValue(); // MASTER_KEY_ID
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        GnuPGKey key = mKeyArray[position];
        View view = mInflater.inflate(R.layout.select_public_key_item, null);
        boolean enabled = isEnabled(position);

        TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
        TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
        TextView keyId = (TextView) view.findViewById(R.id.keyId);
        TextView status = (TextView) view.findViewById(R.id.status);

        mainUserId.setText(key.getName());
        mainUserIdRest.setText(key.getEmail());
        keyId.setText(key.getKeyID());
        status.setText(R.string.unknownStatus);

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

        if (enabled) {
            status.setText(R.string.canEncrypt);
        } else {
        	// TODO handle this logic
            if (0 > 0) {
                // has some CAN_ENCRYPT keys, but col(4) = 0, so must be revoked or expired
                status.setText(R.string.expired);
            } else {
                status.setText(R.string.noKey);
            }
        }

        status.setText(status.getText() + " ");

        if (!enabled) {
            mParent.setItemChecked(position, false);
        }

        view.setEnabled(enabled);
        mainUserId.setEnabled(enabled);
        mainUserIdRest.setEnabled(enabled);
        keyId.setEnabled(enabled);
        status.setEnabled(enabled);

        return view;
    }
}
