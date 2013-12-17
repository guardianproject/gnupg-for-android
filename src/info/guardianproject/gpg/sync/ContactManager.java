
package info.guardianproject.gpg.sync;

import info.guardianproject.gpg.R;

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
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.util.Log;

public class ContactManager {
    public static final String TAG = "ContactManager";

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
    public static synchronized void addContacts(Context context, String account,
            List<RawContact> rawContacts, long groupId) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        final List<RawContact> newUsers = new ArrayList<RawContact>();

        Log.d(TAG, "addContacts: " + rawContacts.size());
        for (final RawContact rawContact : rawContacts) {
            if (!rawContact.isDeleted()) {
                newUsers.add(rawContact);
                addContact(context, account, rawContact, groupId, true, batchOperation);
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
    public static void addContact(Context context, String accountName, RawContact rawContact,
            long groupId, boolean inSync, BatchOperation batchOperation) {

        // Put the data in the contacts provider
        final ContactOperations contactOp = ContactOperations.createNewContact(
                context, rawContact.getFingerprint(), accountName, inSync, batchOperation);

        contactOp.addName(rawContact.getFullName())
                .addEmail(rawContact.getEmail())
                .addEncryptFileTo(rawContact.getFingerprint())
                .addComment(rawContact.getComment())
                .addGroupMembership(groupId);
    }

    public static synchronized void deleteAllContacts(Context context, Account account) {
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation = new BatchOperation(context, resolver);
        final Uri contentUri = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
        final String selection = RawContacts.ACCOUNT_TYPE + "='" + SyncConstants.ACCOUNT_TYPE
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
        Log.v(TAG, "deleteContact " + rawContactId);
        batchOperation.add(ContactOperations.newDeleteCpo(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                true, true).build());
    }
}
