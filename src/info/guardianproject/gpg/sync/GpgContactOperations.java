
package info.guardianproject.gpg.sync;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.sync.SyncAdapter.EncryptFileTo;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

/**
 * Helper class for storing data in the platform content providers.
 */
public class GpgContactOperations {
    private final ContentValues mValues;
    private final BatchOperation mBatchOperation;
    private final Context mContext;
    private boolean mIsSyncOperation;
    private long mRawContactId;
    private int mBackReference;
    private boolean mIsNewContact;

    /**
     * Since we're sending a lot of contact provider operations in a single
     * batched operation, we want to make sure that we "yield" periodically so
     * that the Contact Provider can write changes to the DB, and can open a new
     * transaction. This prevents ANR (application not responding) errors. The
     * recommended time to specify that a yield is permitted is with the first
     * operation on a particular contact. So if we're updating multiple fields
     * for a single contact, we make sure that we call withYieldAllowed(true) on
     * the first field that we update. We use mIsYieldAllowed to keep track of
     * what value we should pass to withYieldAllowed().
     */
    private boolean mIsYieldAllowed;

    /**
     * Returns an instance of ContactOperations instance for adding new contact
     * to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param userId the userId of the sample SyncAdapter user object
     * @param accountName the username for the SyncAdapter account
     * @param isSyncOperation are we executing this as part of a sync operation?
     * @return instance of ContactOperations
     */
    public static GpgContactOperations newInstance(Context context, String keyFpr,
            String accountName, boolean isSyncOperation, BatchOperation batchOperation) {
        return new GpgContactOperations(context, keyFpr, accountName, isSyncOperation, batchOperation);
    }

    /**
     * Returns an instance of ContactOperations for updating existing contact in
     * the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id of the existing rawContact
     * @param isSyncOperation are we executing this as part of a sync operation?
     * @return instance of ContactOperations
     */
    public static GpgContactOperations updateExistingContact(Context context, long rawContactId,
            boolean isSyncOperation, BatchOperation batchOperation) {
        return new GpgContactOperations(context, rawContactId, isSyncOperation, batchOperation);
    }

    public GpgContactOperations(Context context, boolean isSyncOperation,
            BatchOperation batchOperation) {
        mValues = new ContentValues();
        mIsYieldAllowed = true;
        mIsSyncOperation = isSyncOperation;
        mContext = context;
        mBatchOperation = batchOperation;
    }

    public GpgContactOperations(Context context, String keyFpr, String accountName,
            boolean isSyncOperation, BatchOperation batchOperation) {
        this(context, isSyncOperation, batchOperation);
        mBackReference = mBatchOperation.size();
        mIsNewContact = true;
        mValues.put(RawContacts.SOURCE_ID, keyFpr);
        mValues.put(RawContacts.ACCOUNT_TYPE, GpgContactManager.ACCOUNT_TYPE);
        mValues.put(RawContacts.ACCOUNT_NAME, accountName);
        ContentProviderOperation.Builder builder =
                newInsertCpo(RawContacts.CONTENT_URI, mIsSyncOperation, true).withValues(mValues);
        mBatchOperation.add(builder.build());
    }

    public GpgContactOperations(Context context, long rawContactId, boolean isSyncOperation,
            BatchOperation batchOperation) {
        this(context, isSyncOperation, batchOperation);
        mIsNewContact = false;
        mRawContactId = rawContactId;
    }

    /**
     * Adds a contact name. We can take either a full name ("Bob Smith") or
     * separated first-name and last-name ("Bob" and "Smith").
     * 
     * @param name The full name of the contact - typically from an edit
     *            form Can be null if firstName/lastName are specified.
     * @return instance of ContactOperations
     */
    public GpgContactOperations addName(String name) {
        mValues.clear();

        if (!TextUtils.isEmpty(name)) {
            mValues.put(StructuredName.DISPLAY_NAME, name);
            mValues.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        }
        if (mValues.size() > 0) {
            addInsertOp();
        }
        return this;
    }

    /**
     * Adds an email
     * 
     * @param the email address we're adding
     * @return instance of ContactOperations
     */
    public GpgContactOperations addEmail(String email) {
        mValues.clear();
        if (!TextUtils.isEmpty(email)) {
            mValues.put(Email.DATA, email);
            mValues.put(Email.TYPE, Email.TYPE_OTHER);
            mValues.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            addInsertOp();
        }
        return this;
    }

    /**
     * Adds the key id
     * 
     * @param the email address we're adding
     * @return instance of ContactOperations
     */
    public GpgContactOperations addEncryptFileTo(String fingerprint, int flags) {
        mValues.clear();
        if (!TextUtils.isEmpty(fingerprint)) {
            mValues.put(EncryptFileTo.FINGERPRINT, fingerprint);
            mValues.put(EncryptFileTo.DETAIL,
                    mContext.getString(R.string.encrypt_file_to_detail));
            mValues.put(EncryptFileTo.SUMMARY,
                    mContext.getString(R.string.encrypt_file_to));
            mValues.put(EncryptFileTo.KEY_STATUS_FLAGS, flags);
            mValues.put(Data.MIMETYPE, EncryptFileTo.CONTENT_ITEM_TYPE);
            addInsertOp();
        }
        return this;
    }

    /**
     * Adds the comment
     * 
     * @param the email address we're adding
     * @return instance of ContactOperations
     */
    public GpgContactOperations addComment(String comment) {
        mValues.clear();
        if (!TextUtils.isEmpty(comment)) {
            mValues.put(Note.NOTE, comment);
            mValues.put(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE);
            addInsertOp();
        }
        return this;
    }

    /**
     * Adds a group membership
     * 
     * @param id The id of the group to assign
     * @return instance of ContactOperations
     */
    public GpgContactOperations addGroupMembership(long groupId) {
        mValues.clear();
        mValues.put(GroupMembership.GROUP_ROW_ID, groupId);
        mValues.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        addInsertOp();
        return this;
    }

    /**
     * Adds a profile action
     * 
     * @param userId the userId of the sample SyncAdapter user object
     * @return instance of ContactOperations
     */
    public GpgContactOperations addProfileAction(long userId) {
        // TODO
        // mValues.clear();
        // if (userId != 0) {
        // mValues.put(SampleSyncAdapterColumns.DATA_PID, userId);
        // mValues.put(SampleSyncAdapterColumns.DATA_SUMMARY, mContext
        // .getString(R.string.profile_action));
        // mValues.put(SampleSyncAdapterColumns.DATA_DETAIL, mContext
        // .getString(R.string.view_profile));
        // mValues.put(Data.MIMETYPE, SampleSyncAdapterColumns.MIME_PROFILE);
        // addInsertOp();
        // }
        return this;
    }

    /**
     * Updates contact's email
     * 
     * @param email email id of the sample SyncAdapter user
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public GpgContactOperations updateEmail(String email, String existingEmail, Uri uri) {
        if (!TextUtils.equals(existingEmail, email)) {
            mValues.clear();
            mValues.put(Email.DATA, email);
            addUpdateOp(uri);
        }
        return this;
    }

    /**
     * Updates contact's keyfingerprint
     * 
     * @param keyfpr
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public GpgContactOperations updateFingerprint(String keyfpr, String existingKeyfpr, Uri uri) {
        if (!TextUtils.equals(existingKeyfpr, keyfpr)) {
            mValues.clear();
            mValues.put(EncryptFileTo.FINGERPRINT, keyfpr);
            addUpdateOp(uri);
        }
        return this;
    }

    /**
     * Updates contact's comment
     * 
     * @param comment
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public GpgContactOperations updateComment(String comment, String existingComment, Uri uri) {
        if (!TextUtils.equals(existingComment, comment)) {
            mValues.clear();
            mValues.put(Note.NOTE, comment);
            addUpdateOp(uri);
        }
        return this;
    }

    /**
     * Updates contact's name.
     * 
     * @param uri Uri for the existing raw contact to be updated
     * @param existingFullName the full name stored in provider
     * @param fullName the new full name to store
     * @return instance of ContactOperations
     */
    public GpgContactOperations updateName(Uri uri,
            String existingFullName,
            String fullName) {

        mValues.clear();
        if (!TextUtils.equals(existingFullName, fullName)) {
            mValues.put(StructuredName.DISPLAY_NAME, fullName);
        }
        if (mValues.size() > 0) {
            addUpdateOp(uri);
        }
        return this;
    }

    public GpgContactOperations updateDirtyFlag(boolean isDirty, Uri uri) {
        int isDirtyValue = isDirty ? 1 : 0;
        mValues.clear();
        mValues.put(RawContacts.DIRTY, isDirtyValue);
        addUpdateOp(uri);
        return this;
    }

    /**
     * Updates contact's profile action
     * 
     * @param userId sample SyncAdapter user id
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public GpgContactOperations updateProfileAction(Integer userId, Uri uri) {
        // TODO
        // mValues.clear();
        // mValues.put(EncryptFileTo.DATA_PID, userId);
        // addUpdateOp(uri);
        return this;
    }

    /**
     * Adds an insert operation into the batch
     */
    private void addInsertOp() {

        if (!mIsNewContact) {
            mValues.put(Data.RAW_CONTACT_ID, mRawContactId);
        }
        ContentProviderOperation.Builder builder =
                newInsertCpo(Data.CONTENT_URI, mIsSyncOperation, mIsYieldAllowed);
        builder.withValues(mValues);
        if (mIsNewContact) {
            builder.withValueBackReference(Data.RAW_CONTACT_ID, mBackReference);
        }
        mIsYieldAllowed = false;
        mBatchOperation.add(builder.build());
    }

    /**
     * Adds an update operation into the batch
     */
    private void addUpdateOp(Uri uri) {
        ContentProviderOperation.Builder builder =
                newUpdateCpo(uri, mIsSyncOperation, mIsYieldAllowed).withValues(mValues);
        mIsYieldAllowed = false;
        mBatchOperation.add(builder.build());
    }

    public static ContentProviderOperation.Builder newInsertCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    public static ContentProviderOperation.Builder newUpdateCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newUpdate(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    public static ContentProviderOperation.Builder newDeleteCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newDelete(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
        if (isSyncOperation) {
            /**
             * If we're in the middle of a real sync-adapter operation, then go
             * ahead and tell the Contacts provider that we're the sync adapter.
             * That gives us some special permissions - like the ability to
             * really delete a contact, and the ability to clear the dirty flag.
             * <p>
             * /* If we're not in the middle of a sync operation (for example,
             * we just locally created/edited a new contact), then we don't want
             * to use the special permissions, and the system will automagically
             * mark the contact as 'dirty' for us!
             */
            return uri.buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                    .build();
        }
        return uri;
    }
}
