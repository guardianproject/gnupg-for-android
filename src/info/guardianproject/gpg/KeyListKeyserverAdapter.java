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

import java.math.BigInteger;
import java.util.Locale;

import org.openintents.openpgp.keyserver.HkpKeyServer;
import org.openintents.openpgp.keyserver.KeyServer;
import org.openintents.openpgp.keyserver.KeyServer.InsufficientQuery;
import org.openintents.openpgp.keyserver.KeyServer.KeyInfo;
import org.openintents.openpgp.keyserver.KeyServer.QueryException;
import org.openintents.openpgp.keyserver.KeyServer.TooManyResponses;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class KeyListKeyserverAdapter extends BaseAdapter {
    public static final String TAG = "KeyListKeyserverAdapter";

    protected LayoutInflater mInflater;
    protected ListView mParent;
    protected String mSearchString;

    private HkpKeyServer mKeyserver;
    private KeyInfo[] mKeyArray;

    public KeyListKeyserverAdapter(ListView parent, String searchString) {
        Log.v(TAG, "KeyListKeyserverAdapter");
        mParent = parent;
        mSearchString = searchString;

        mInflater = (LayoutInflater) parent.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        Context context = mParent.getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String host = prefs.getString(GpgPreferenceActivity.PREF_KEYSERVER,
                "ipv4.pool.sks-keyservers.net");
/*
        mKeyserver = new HkpKeyServer(host);
        try {
            mKeyArray = mKeyserver.search("hans@eds.org").toArray(new KeyInfo[0]);
        } catch (QueryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TooManyResponses e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InsufficientQuery e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
*/
        if (mKeyArray == null) {
            Log.e(TAG, "keyArray is null");
        }
    }

    @Override
    public boolean isEnabled(int position) {
        KeyInfo key = mKeyArray[position];
        return (!key.isRevoked);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        Log.v(TAG, "getCount ");
        if (mKeyArray == null)
            return 0;
        else
            return mKeyArray.length;
    }

    @Override
    public Object getItem(int position) {
        Log.v(TAG, "getItem " + position);
        KeyInfo key = mKeyArray[position];
        String[] ret = new String[3];
        ret[0] = "";
        ret[1] = "";
        ret[2] = "";
        // TODO parse this out userids better
        if (key.userIds.size() > 0) {
            ret[0] = key.userIds.get(0);
        }
        return ret;
    }

    @Override
    public long getItemId(int position) {
        return mKeyArray[position].keyId; // MASTER_KEY_ID
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.v(TAG, "getView " + position);
        KeyInfo key = mKeyArray[position];
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
        String keyId = KeyInfo.hexFromKeyId(mKeyArray[position].keyId);
        int[] keyIdColors = GpgApplication.genFingerprintColor(keyId);

        keyId0.setText(keyId.substring(0, 4));
        keyId1.setText(keyId.substring(4, 8));
        keyId2.setText(keyId.substring(8, 12));
        keyId3.setText(keyId.substring(12));

        keyId0.setTextColor(keyIdColors[0]);
        keyId1.setTextColor(keyIdColors[1]);
        keyId2.setTextColor(keyIdColors[2]);
        keyId3.setTextColor(keyIdColors[3]);

        mainUserId.setText(key.userIds.get(0));
        mainUserIdRest.setText(""); // TODO parse out email from userId above?
        status.setText(R.string.unknownStatus);

        if (mainUserIdRest.getText().length() == 0) {
            mainUserIdRest.setVisibility(View.GONE);
        }

        if (usable)
            status.setText("");
        /*
         * else if (key.isDisabled()) status.setText(R.string.disabled); else if
         * (key.isExpired()) status.setText(R.string.expired); else if
         * (key.isInvalid()) status.setText(R.string.invalid); else if
         * (key.isRevoked()) status.setText(R.string.revoked);
         */
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
