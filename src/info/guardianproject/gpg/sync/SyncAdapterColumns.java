
package info.guardianproject.gpg.sync;

import android.provider.ContactsContract.Data;

public final class SyncAdapterColumns {

    private SyncAdapterColumns() {
    }

    /**
     * MIME-type used when storing a profile {@link Data} entry. The custom MIME
     * type you've defined for one of your custom data row types in the
     * ContactsContract.Data table. see: res/xml/contacts.xml
     */
    public static final String MIME_PROFILE =
            "vnd.android.cursor.item/vnd.pgp.keyid";

    public static final String DATA_KEYID = Data.DATA1;
    public static final String DATA_SUMMARY = Data.DATA2;
    public static final String DATA_DETAIL = Data.DATA3;
}
