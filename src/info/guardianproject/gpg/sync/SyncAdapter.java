package info.guardianproject.gpg.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.apache.http.ParseException;

import java.util.List;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = SyncAdapter.class.getSimpleName();
    private static final String SYNC_MARKER_KEY = "info.guardianproject.gpg.sync.marker";
    private static final boolean NOTIFY_AUTH_FAILURE = true;

    private final AccountManager mAccountManager;

    private final Context mContext;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {

        try {
            long lastSyncMarker = getServerSyncMarker(account);
            // first pass at naive "sync
            // all keys from gnupg are written into Contacts

            // By default, contacts from a 3rd party provider are hidden in the contacts
            // list. So let's set the flag that causes them to be visible, so that users
            // can actually see these contacts.
            if (lastSyncMarker == 0) {
                ContactManager.setAccountContactsVisibility(getContext(), account, true);
            }

            List<RawContact> dirtyContacts = ContactManager.getDirtyContacts(mContext, account);
            List<RawContact> updatedContacts = RawContact.fromPublicKeys();
            // Make sure that our group exists
            final long groupId = ContactManager.ensureGroupExists(mContext, account);

            Log.d(TAG, "Before update contacts, scyncing : " + updatedContacts.size() );


            ContactManager.deleteContacts(mContext,
                    account.name,
                    dirtyContacts,
                    groupId,
                    lastSyncMarker);
            long newSyncState = ContactManager.updateContacts(mContext,
                    account.name,
                    updatedContacts,
                    groupId,
                    lastSyncMarker);

            // Save off the new sync marker. On our next sync, we only want to receive
            // contacts that have changed since this sync...
            setServerSyncMarker(account, newSyncState);

//            if (dirtyContacts.size() > 0) {
//                ContactManager.clearSyncFlags(mContext, dirtyContacts);
//            }

        } catch (final ParseException e) {
            Log.e(TAG, "ParseException", e);
            syncResult.stats.numParseExceptions++;
        }
    }

    /**
     * This helper function fetches the last known high-water-mark
     * we received from the server - or 0 if we've never synced.
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
     * @param account The account we're syncing
     * @param marker The high-water-mark we want to save.
     */
    private void setServerSyncMarker(Account account, long marker) {
        mAccountManager.setUserData(account, SYNC_MARKER_KEY, Long.toString(marker));
    }

}
