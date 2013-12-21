
package info.guardianproject.gpg;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openintents.openpgp.keyserver.HkpKeyServer;
import org.openintents.openpgp.keyserver.KeyServer.InsufficientQuery;
import org.openintents.openpgp.keyserver.KeyServer.KeyInfo;
import org.openintents.openpgp.keyserver.KeyServer.QueryException;
import org.openintents.openpgp.keyserver.KeyServer.TooManyResponses;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import android.util.Log;

public class KeyserverLoader extends AsyncTaskLoader<KeyserverResult<List<KeyInfo>>> {
    public static final String TAG = "KeyserverLoader";

    final Context mContext;
    String mSearchString;

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<KeyInfo> ALPHA_COMPARATOR = new Comparator<KeyInfo>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(KeyInfo object1, KeyInfo object2) {
            return sCollator.compare(object1.userIds.get(0), object2.userIds.get(0));
        }
    };

    public KeyserverLoader(Context context) {
        super(context);
        Log.v(TAG, "KeyserverLoader");
        mContext = context;
    }

    @Override
    public void onStartLoading() {
        Log.v(TAG, "onStartLoading");
        mSearchString = MainActivity.mCurrentSearchString;
    }

    @Override
    public KeyserverResult<List<KeyInfo>> loadInBackground() {
        Log.v(TAG, "loadInBackground");
        KeyserverResult<List<KeyInfo>> result = new KeyserverResult<List<KeyInfo>>();
        try {
            if(TextUtils.isEmpty(mSearchString))
                return result; // nothing to do...
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            String host = prefs.getString(GpgPreferenceActivity.PREF_KEYSERVER,
                    "ipv4.pool.sks-keyservers.net");
            HkpKeyServer keyserver = new HkpKeyServer(host);
            Log.i(TAG, "loadInBackground mSearchString: " + mSearchString);
            ArrayList<KeyInfo> data = keyserver.search(mSearchString);
            Collections.sort(data, ALPHA_COMPARATOR);
            result.setData(data);
        } catch (QueryException e) {
            result.setErrorResid(R.string.error_query_failed);
            e.printStackTrace();
        } catch (TooManyResponses e) {
            result.setErrorResid(R.string.error_too_many_responses);
            e.printStackTrace();
        } catch (InsufficientQuery e) {
            result.setErrorResid(R.string.error_insufficient_query);
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Called when there is new data to deliver to the client. The super class
     * will take care of delivering it; the implementation here just adds a
     * little more logic.
     */
    @Override
    public void deliverResult(KeyserverResult<List<KeyInfo>> result) {
        Log.v(TAG, "deliverResult");
        // An async query came in while the loader is stopped. We
        // don't need the result.
        if (isReset())
            return;
        // the Loader is currently started, immediately deliver its results
        if (isStarted())
            super.deliverResult(result);
    }

}
