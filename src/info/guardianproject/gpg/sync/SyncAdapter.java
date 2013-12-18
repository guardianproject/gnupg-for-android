
package info.guardianproject.gpg.sync;

import java.util.List;

import org.apache.http.ParseException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncAdapter";
    private static final String SYNC_MARKER_KEY = "info.guardianproject.gpg.sync.marker";

    private final AccountManager mAccountManager;

    private final Context mContext;

    public final class EncryptFileTo {
        /**
         * MIME-type used when storing a profile {@link Data} entry. The custom
         * MIME type you've defined for one of your custom data row types in the
         * {@link ContactsContract.Data} table. see: res/xml/contacts.xml.
         * EncryptFileTo.CONTENT_ITEM_TYPE is also defined in
         * res/values/mimetypes.xml as \@string/mimetype_encryptfileto
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.info.guardianproject.gpg.encryptfileto";
        public static final String FINGERPRINT = Data.DATA1;
        public static final String SUMMARY = Data.DATA2;
        public static final String DETAIL = Data.DATA3;
        public static final String KEY_STATUS_FLAGS = Data.DATA4;
    }

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        try {
            // TODO this should probably be used to prevent duplication of work
            // long lastSyncMarker = getServerSyncMarker(account);

            /*
             * All keys from gnupg are written into Contacts By default,
             * contacts from a 3rd party provider are hidden in the contacts
             * list. So let's set the flag that causes them to be visible, so
             * that users can actually see these contacts.
             */
            GpgContactManager.setAccountContactsVisibility(getContext(), account, true);

            List<RawGpgContact> updatedContacts = RawGpgContact.fromPublicKeys();
            final long groupId = GpgContactManager.ensureGroupExists(mContext, account);
            GpgContactManager.deleteAllContacts(mContext, account);
            Log.d(TAG, "number of contacts to add: " + updatedContacts.size());
            GpgContactManager.addContacts(mContext,
                    account.name,
                    updatedContacts,
                    groupId);
        } catch (final ParseException e) {
            Log.e(TAG, "ParseException", e);
            syncResult.stats.numParseExceptions++;
        }
    }

    /**
     * This helper function fetches the last known high-water-mark we received
     * from the server - or 0 if we've never synced.
     * 
     * @param account the account we're syncing
     * @return the change high-water-mark
     */
    private long getServerSyncMarker(Account account) {
        String markerString = mAccountManager.getUserData(account, SYNC_MARKER_KEY);
        if (!TextUtils.isEmpty(markerString)) {
            return Long.parseLong(markerString);
        }
        return 0;
    }

    /**
     * Save off the high-water-mark we receive back from the server.
     * 
     * @param account The account we're syncing
     * @param marker The high-water-mark we want to save.
     */
    private void setServerSyncMarker(Account account, long marker) {
        mAccountManager.setUserData(account, SYNC_MARKER_KEY, Long.toString(marker));
    }

}
