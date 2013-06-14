
package info.guardianproject.gpg.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.text.TextUtils;
import android.util.Log;

import info.guardianproject.gpg.R;

import java.util.ArrayList;
import java.util.List;

public class ContactManager {
    public static final String TAG = ContactManager.class.getSimpleName();

    /**
     * When we first add a sync adapter to the system, the contacts from that
     * sync adapter will be hidden unless they're merged/grouped with an
     * existing contact. But typically we want to actually show those contacts,
     * so we need to mess with the Settings table to get them to show up.
     *
     * @param context the Authenticator Activity context
     * @param account the Account who's visibility we're changing
     * @param visible true if we want the contacts visible, false for hidden
     */
    public static void setAccountContactsVisibility(Context context, Account account,
            boolean visible) {
        ContentValues values = new ContentValues();
        values.put(RawContacts.ACCOUNT_NAME, account.name);
        values.put(RawContacts.ACCOUNT_TYPE, SyncConstants.ACCOUNT_TYPE);
        values.put(Settings.UNGROUPED_VISIBLE, visible ? 1 : 0);

        context.getContentResolver().insert(Settings.CONTENT_URI, values);
    }

    /**
     * We have to have a group in which our Contacts exist.
     *
     * @param context
     * @param account
     * @return the group id
     */
    public static long ensureGroupExists(Context context, Account account) {
        final String groupName = context.getString(R.string.contact_group);
        final ContentResolver resolver = context.getContentResolver();

        // Lookup the group
        long groupId = 0;
        final Cursor cursor = resolver.query(Groups.CONTENT_URI, new String[] {
            Groups._ID
        },
                Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=? AND " +
                        Groups.TITLE + "=?",
                new String[] {
                        account.name, account.type, groupName
                }, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    groupId = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        if (groupId == 0) {
            // Sample group doesn't exist yet, so create it
            final ContentValues contentValues = new ContentValues();
            contentValues.put(Groups.ACCOUNT_NAME, account.name);
            contentValues.put(Groups.ACCOUNT_TYPE, account.type);
            contentValues.put(Groups.TITLE, groupName);
            // GROUP_IS_READ_ONLY was added in API 11
            if( android.os.Build.VERSION.SDK_INT >= 11 )
                contentValues.put(Groups.GROUP_IS_READ_ONLY, true);

            final Uri newGroupUri = resolver.insert(Groups.CONTENT_URI, contentValues);
            groupId = ContentUris.parseId(newGroupUri);
        }
        return groupId;
    }

    /**
     * Take a list of updated contacts and apply those changes to the contacts
     * database. Typically this list of contacts would have been returned from
     * the server, and we want to apply those changes locally.
     *
     * @param context The context of Authenticator Activity
     * @param account The username for the account
     * @param rawContacts The list of contacts to update
     * @param lastSyncMarker The previous server sync-state
     * @return the server syncState that should be used in our next sync
     *         request.
     */
    public static synchronized long updateContacts(Context context, String account,
            List<RawContact> rawContacts, long groupId, long lastSyncMarker) {

        long currentSyncMarker = lastSyncMarker;
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        final List<RawContact> newUsers = new ArrayList<RawContact>();

        Log.d(TAG, "In SyncContacts");
        for (final RawContact rawContact : rawContacts) {
            final long rawContactId = 0;

            if (rawContactId != 0) {
                // updating
                Log.d(TAG, "updating Contact");
                if (!rawContact.isDeleted()) {
                    // updateContact(context, resolver, rawContact,
                    // updateServerId,
                    // true, true, true, rawContactId, batchOperation);
                } else {
                    // TODO delete contacts
                    // deleteContact(context, rawContactId, batchOperation);
                }
            } else {
                // adding new contact
                Log.d(TAG, "adding contact");
                if (!rawContact.isDeleted()) {
                    newUsers.add(rawContact);
                    addContact(context, account, rawContact, groupId, true, batchOperation);
                }
            }
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        batchOperation.execute();

        return currentSyncMarker;
    }


    public static synchronized long deleteContacts(Context context, String account,
            List<RawContact> rawContacts, long groupId, long lastSyncMarker) {

        long currentSyncMarker = lastSyncMarker;
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);

        Log.d(TAG, "deleteContacts: " + rawContacts.size());
        for (final RawContact rawContact : rawContacts) {
            if (rawContact.getRawContactId() != 0) {
                 deleteContact(context, rawContact.getRawContactId(), batchOperation);
            }
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        batchOperation.execute();

        return currentSyncMarker;
    }

    /**
     * Adds a single contact to the platform contacts provider. This can be used
     * to respond to a new contact found as part of sync information returned
     * from the server, or because a user added a new contact.
     *
     * @param context the Authenticator Activity context
     * @param accountName the account the contact belongs to
     * @param rawContact the sample SyncAdapter User object
     * @param groupId the id of the sample group
     * @param inSync is the add part of a client-server sync?
     * @param batchOperation allow us to batch together multiple operations into
     *            a single provider call
     */
    public static void addContact(Context context, String accountName, RawContact rawContact,
            long groupId, boolean inSync, BatchOperation batchOperation) {

        // Put the data in the contacts provider
        final ContactOperations contactOp = ContactOperations.createNewContact(
                context, rawContact.getFingerprint(), accountName, inSync, batchOperation);

        contactOp.addName(rawContact.getFullName())
                .addEmail(rawContact.getEmail())
                .addKeyFingerprint(rawContact.getFingerprint())
                .addComment(rawContact.getComment())
                .addGroupMembership(groupId);

        // If we have a serverId, then go ahead and create our status profile.
        // Otherwise skip it - and we'll create it after we sync-up to the
        // server later on.
        if (!TextUtils.isEmpty(rawContact.getFingerprint())) {
            // TODO
            // contactOp.addProfileAction(rawContact.getServerContactId());
        }
    }

    public static List<RawContact> getDirtyContacts(Context context, Account account) {
        Log.i(TAG, "*** Looking for local dirty contacts");
        List<RawContact> dirtyContacts = new ArrayList<RawContact>();

        final ContentResolver resolver = context.getContentResolver();
        final Cursor c = resolver.query(DirtyQuery.CONTENT_URI,
                DirtyQuery.PROJECTION,
                DirtyQuery.SELECTION,
                new String[] {
                    account.name
                },
                null);
        try {
            while (c.moveToNext()) {
                final long rawContactId = c.getLong(DirtyQuery.COLUMN_RAW_CONTACT_ID);
                final long serverContactId = c.getLong(DirtyQuery.COLUMN_SERVER_ID);
                // final boolean isDirty =
                // "1".equals(c.getString(DirtyQuery.COLUMN_DIRTY));
                // final boolean isDeleted =
                // "1".equals(c.getString(DirtyQuery.COLUMN_DELETED));

                // for now we mark all contacts as dirty
                RawContact rawContact = getRawContact(context, rawContactId);
                Log.i(TAG, "Contact Name: " + rawContact.getFullName());
                dirtyContacts.add(rawContact);
                Log.i(TAG, "Dirty Contact: " + Long.toString(rawContactId));

                // if (isDeleted) {
                // Log.i(TAG, "Contact is marked for deletion");
                // RawContact rawContact =
                // RawContact.createDeletedContact(rawContactId,
                // serverContactId);
                // dirtyContacts.add(rawContact);
                // } else if (isDirty) {
                // RawContact rawContact = getRawContact(context, rawContactId);
                // Log.i(TAG, "Contact Name: " + rawContact.getBestName());
                // dirtyContacts.add(rawContact);
                // }
            }

        } finally {
            if (c != null) {
                c.close();
            }
        }
        return dirtyContacts;
    }

    /**
     * Deletes a contact from the platform contacts provider. This method is
     * used both for contacts that were deleted locally and then that deletion
     * was synced to the server, and for contacts that were deleted on the
     * server and the deletion was synced to the client.
     *
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id for this rawContact in contacts
     *            provider
     */
    private static void deleteContact(Context context, long rawContactId,
            BatchOperation batchOperation) {

        batchOperation.add(ContactOperations.newDeleteCpo(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                true, true).build());
    }

    /**
     * Return a User object with data extracted from a contact stored in the
     * local contacts database. Because a contact is actually stored over
     * several rows in the database, our query will return those multiple rows
     * of information. We then iterate over the rows and build the User
     * structure from what we find.
     *
     * @param context the Authenticator Activity context
     * @param rawContactId the unique ID for the local contact
     * @return a User object containing info on that contact
     */
    private static RawContact getRawContact(Context context, long rawContactId) {
        String fullName = null;
        String email = null;
        String comment = null;
        String keyfingerprint = null;
        long serverId = -1;

        final ContentResolver resolver = context.getContentResolver();
        final Cursor c =
                resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION,
                        new String[] {
                            String.valueOf(rawContactId)
                        }, null);
        try {
            while (c.moveToNext()) {
                final long id = c.getLong(DataQuery.COLUMN_ID);
                final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
                final long tempServerId = c.getLong(DataQuery.COLUMN_SERVER_ID);
                if (tempServerId > 0) {
                    serverId = tempServerId;
                }
                final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    fullName = c.getString(DataQuery.COLUMN_FULL_NAME);
                } else if (mimeType.equals(Note.CONTENT_ITEM_TYPE)) {
                    comment = c.getString(DataQuery.COLUMN_NOTE);
                    Log.d(TAG, "comment:" + comment);
                } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                    email = c.getString(DataQuery.COLUMN_EMAIL_ADDRESS);
                    Log.d(TAG, "email:" + email);
                } else if (mimeType.equals(SyncAdapterColumns.MIME_PROFILE)) {
                    keyfingerprint = c.getString(DataQuery.COLUMN_KEYFPR);
                    Log.d(TAG, "fingerprint:" + keyfingerprint);
                } else {
                    Log.d(TAG, "GOT UNKNOWN DATA: " + mimeType);
                }
            } // while
        } finally {
            c.close();
        }

        // Now that we've extracted all the information we care about,
        // create the actual User object.
        RawContact rawContact = new RawContact(fullName, email, comment, keyfingerprint, keyfingerprint,
                rawContactId, false);

        return rawContact;
    }

    /**
     * Constants for a query to find SampleSyncAdapter contacts that are in need
     * of syncing to the server. This should cover new, edited, and deleted
     * contacts.
     */
    final private static class DirtyQuery {

        private DirtyQuery() {
        }

        public final static String[] PROJECTION = new String[] {
                RawContacts._ID,
                RawContacts.SOURCE_ID,
                RawContacts.DIRTY,
                RawContacts.DELETED,
                RawContacts.VERSION
        };

        public final static int COLUMN_RAW_CONTACT_ID = 0;
        public final static int COLUMN_SERVER_ID = 1;
        public final static int COLUMN_DIRTY = 2;
        public final static int COLUMN_DELETED = 3;
        public final static int COLUMN_VERSION = 4;

        public static final Uri CONTENT_URI = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();

        public static final String SELECTION =
                /*RawContacts.DIRTY + "=1 AND "*/ ""
                        + RawContacts.ACCOUNT_TYPE + "='" + SyncConstants.ACCOUNT_TYPE + "' AND "
                        + RawContacts.ACCOUNT_NAME + "=?";
    }

    /**
     * Constants for a query to get contact data for a given rawContactId
     */
    final private static class DataQuery {

        private DataQuery() {
        }

        public static final String[] PROJECTION =
                new String[] {
                        Data._ID, RawContacts.SOURCE_ID, Data.MIMETYPE, Data.DATA1,
                        Data.DATA2, Data.DATA3, Data.DATA15, Data.SYNC1
                };

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_SERVER_ID = 1;
        public static final int COLUMN_MIMETYPE = 2;
        public static final int COLUMN_DATA1 = 3;
        public static final int COLUMN_DATA2 = 4;
        public static final int COLUMN_DATA3 = 5;
        public static final int COLUMN_DATA15 = 6;
        public static final int COLUMN_SYNC1 = 7;

        public static final Uri CONTENT_URI = Data.CONTENT_URI;

        public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
        public static final int COLUMN_EMAIL_TYPE = COLUMN_DATA2;
        public static final int COLUMN_NOTE = COLUMN_DATA1;
        public static final int COLUMN_KEYFPR = COLUMN_DATA1;
        public static final int COLUMN_FULL_NAME = COLUMN_DATA1;
        public static final int COLUMN_AVATAR_IMAGE = COLUMN_DATA15;
        public static final int COLUMN_SYNC_DIRTY = COLUMN_SYNC1;

        public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
    }
}
