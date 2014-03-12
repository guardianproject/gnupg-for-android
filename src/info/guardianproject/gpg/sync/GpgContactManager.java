
package info.guardianproject.gpg.sync;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.sync.SyncAdapter.EncryptFileTo;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.util.Log;

public class GpgContactManager {
    private static final String TAG = "GpgContactManager";

    public static final String ACCOUNT_TYPE = "info.guardianproject.gpg.sync";

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
        values.put(RawContacts.ACCOUNT_TYPE, GpgContactManager.ACCOUNT_TYPE);
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
    public static long ensureGroupExists(Context context, Account account, String groupName) {
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
            if (android.os.Build.VERSION.SDK_INT >= 11)
                contentValues.put(Groups.GROUP_IS_READ_ONLY, true);

            final Uri newGroupUri = resolver.insert(Groups.CONTENT_URI, contentValues);
            groupId = ContentUris.parseId(newGroupUri);
        }
        return groupId;
    }

    /**
     * Take a list of keys and add all them to the contacts database.
     * 
     * @param context The context of Authenticator Activity
     * @param account The username for the account
     * @param rawContacts The list of contacts to update
     */
    public static synchronized void addContacts(Context context, Account account,
            List<RawGpgContact> rawContacts) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        final List<RawGpgContact> newUsers = new ArrayList<RawGpgContact>();

        for (final RawGpgContact rawContact : rawContacts) {
            if (!rawContact.deleted) {
                newUsers.add(rawContact);
                addContact(context, resolver, account, rawContact, true, batchOperation);
            }
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        batchOperation.execute();
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
    public static void addContact(Context context, ContentResolver resolver, Account account,
            RawGpgContact rawContact, boolean inSync, BatchOperation batchOperation) {

        // Put the data in the contacts provider
        final GpgContactOperations contactOp = GpgContactOperations.newInstance(
                context, rawContact.fingerprint, account.name, inSync, batchOperation);

        contactOp.addName(rawContact.name)
                .addEmail(rawContact.email)
                .addEncryptFileTo(rawContact.fingerprint, rawContact.flags)
                .addComment(rawContact.comment);
        long groupId = GpgContactManager.ensureGroupExists(context, account,
                context.getString(R.string.keyring_group_name));
        contactOp.addGroupMembership(groupId);
        if (rawContact.hasSecretKey) {
            groupId = GpgContactManager.ensureGroupExists(context, account,
                    context.getString(R.string.secret_key_group_name));
            contactOp.addGroupMembership(groupId);
        }
    }

    public static List<RawGpgContact> getAllContacts(Context context, Account account) {
        List<RawGpgContact> contacts = new ArrayList<RawGpgContact>();

        if (account == null) // if the account isn't setup yet...
            return contacts;

        final ContentResolver resolver = context.getContentResolver();
        final Uri contentUri = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
        final String[] projection = new String[] {
                RawContacts._ID,
                Contacts.DISPLAY_NAME
        };
        final String[] selectionArgs = new String[] {
                account.name
        };
        final String selection = RawContacts.ACCOUNT_TYPE + "='" + GpgContactManager.ACCOUNT_TYPE
                + "' AND " + RawContacts.ACCOUNT_NAME + "=?";
        final String sort = Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";

        final Cursor c = resolver.query(contentUri,
                projection,
                selection,
                selectionArgs,
                sort);
        try {
            while (c.moveToNext()) {
                RawGpgContact rawContact = getRawGpgContact(context, c.getLong(0));
                contacts.add(rawContact);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return contacts;
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
    private static RawGpgContact getRawGpgContact(Context context, long rawContactId) {
        String fullName = null;
        String email = null;
        String comment = null;
        String fingerprint = null;
        int flags = 0;
        final ContentResolver resolver = context.getContentResolver();
        final String selection = Data.RAW_CONTACT_ID + "=?";
        final String[] selectionArgs = new String[] {
                String.valueOf(rawContactId)
        };
        final String[] projection = new String[] {
                Data._ID, // 0
                RawContacts.SOURCE_ID, // 1
                Data.MIMETYPE,// 2
                Data.DATA1, // 3
                Data.DATA2, // 4
                Data.DATA3, // 5
                Data.DATA4, // 6
        };
        final Cursor c =
                resolver.query(Data.CONTENT_URI, projection, selection,
                        selectionArgs, null);
        try {
            while (c.moveToNext()) {
                final long id = c.getLong(0);
                final String mimeType = c.getString(2); // Data.MIMETYPE
                final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);
                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    fullName = c.getString(3); // Data.DATA1
                } else if (mimeType.equals(Note.CONTENT_ITEM_TYPE)) {
                    comment = c.getString(3); // Data.DATA1
                } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                    email = c.getString(3); // Data.DATA1
                } else if (mimeType.equals(EncryptFileTo.CONTENT_ITEM_TYPE)) {
                    fingerprint = c.getString(1); // RawContacts.SOURCE_ID
                    flags = c.getShort(6); // Data.DATA4
                } else if (mimeType.equals(GroupMembership.CONTENT_ITEM_TYPE)) {
                    // Log.d(TAG, "group item:" + c.getString(3)); // Data.DATA1
                } else {
                    Log.d(TAG, "GOT UNKNOWN DATA: " + mimeType);
                }
            }
        } finally {
            c.close();
        }

        return new RawGpgContact(fullName, email, comment, fingerprint, flags, rawContactId, false);
    }

    public static synchronized void deleteAllContacts(Context context, Account account) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        final Uri contentUri = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
        final String selection = RawContacts.ACCOUNT_TYPE + "='" + GpgContactManager.ACCOUNT_TYPE
                + "' AND " + RawContacts.ACCOUNT_NAME + "=?";
        final Cursor c = resolver.query(contentUri,
                new String[] {
                    RawContacts._ID
                },
                selection,
                new String[] {
                    account.name
                },
                null);
        try {
            while (c.moveToNext()) {
                final long rawContactId = c.getLong(0);
                if (rawContactId != 0)
                    deleteContact(context, rawContactId, batchOperation);
                if (batchOperation.size() >= 50)
                    batchOperation.execute();
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        batchOperation.execute();
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
        batchOperation.add(GpgContactOperations.newDeleteCpo(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                true, true).build());
    }
}
