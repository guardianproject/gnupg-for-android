
package info.guardianproject.gpg;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import com.freiheit.gnupg.GnuPGKey;

public class KeyDataProvider extends ContentProvider {
    public static final String AUTHORITY = "info.guardianproject.gpg";

    private static final int PUBLIC_KEY_BY_KEY_ID = 103;
    private static final int PUBLIC_KEY_BY_EMAIL = 104;
    private static final int SECRET_KEY_BY_KEY_ID = 203;
    private static final int SECRET_KEY_BY_EMAIL = 204;

    private static final String PUBLIC_KEY_CONTENT_DIR_TYPE = "vnd.android.cursor.dir/vnd.info.guardianproject.gpg.public_key";
    private static final String PUBLIC_KEY_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.info.guardianproject.gpg.public_key";

    private static final String SECRET_KEY_CONTENT_DIR_TYPE = "vnd.android.cursor.dir/vnd.info.guardianproject.gpg.secret_key";
    private static final String SECRET_KEY_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.info.guardianproject.gpg.secret_key";

    public static class Columns {
        public static final String _ID = "id";
        public static final String KEY_ID = "key_id";
        public static final String EMAIL = "email";
        public static final String FINGERPRINT = "fingerprint";
        public static final String COMMENT = "comment";
        public static final String NAME = "name";
    }

    private static final UriMatcher mUriMatcher;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, "keys/public/key_id/*",
                PUBLIC_KEY_BY_KEY_ID);
        mUriMatcher.addURI(AUTHORITY, "keys/public/email/*",
                PUBLIC_KEY_BY_EMAIL);

        mUriMatcher.addURI(AUTHORITY, "keys/secret/key_id/*",
                SECRET_KEY_BY_KEY_ID);
        mUriMatcher.addURI(AUTHORITY, "keys/secret/email/*",
                SECRET_KEY_BY_EMAIL);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        GnuPGKey[] keyArray = null;
        String query = uri.getLastPathSegment();
        int match = mUriMatcher.match(uri);
        switch (match) {
            case PUBLIC_KEY_BY_EMAIL:
                keyArray = GnuPG.context.searchKeys(query);
                break;
            case SECRET_KEY_BY_EMAIL:
                keyArray = GnuPG.context.searchSecretKeys(query);
                break;
            case PUBLIC_KEY_BY_KEY_ID:
                keyArray = new GnuPGKey[1];
                keyArray[0] = GnuPG.context.getKeyByFingerprint(query);
                break;
            case SECRET_KEY_BY_KEY_ID:
                keyArray = new GnuPGKey[1];
                keyArray[0] = GnuPG.context.getSecretKeyByFingerprint(query);
                break;
            default: {
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
        }

        if (keyArray == null)
            return null;

        final MatrixCursor c = new MatrixCursor(projection);
        for (int i = 0; i < keyArray.length; ++i) {
            Object[] row = new Object[projection.length];
            for (int j = 0; j < projection.length; j++) {
                if (projection[j].equals(Columns._ID)) {
                    row[j] = Integer.valueOf(i);
                } else if (projection[j].equals(Columns.KEY_ID)) {
                    row[j] = keyArray[i].getKeyID();
                } else if (projection[j].equals(Columns.EMAIL)) {
                    row[j] = keyArray[i].getEmail();
                } else if (projection[j].equals(Columns.COMMENT)) {
                    row[j] = keyArray[i].getComment();
                } else if (projection[j].equals(Columns.NAME)) {
                    row[j] = keyArray[i].getName();
                } else if (projection[j].equals(Columns.FINGERPRINT)) {
                    row[j] = keyArray[i].getFingerprint();
                }
            }
            c.addRow(row);
        }
        c.close();
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case PUBLIC_KEY_BY_EMAIL:
                return PUBLIC_KEY_CONTENT_DIR_TYPE;

            case PUBLIC_KEY_BY_KEY_ID:
                return PUBLIC_KEY_CONTENT_ITEM_TYPE;

            case SECRET_KEY_BY_EMAIL:
                return SECRET_KEY_CONTENT_DIR_TYPE;

            case SECRET_KEY_BY_KEY_ID:
                return SECRET_KEY_CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        throw new RuntimeException("Operation not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        throw new RuntimeException("Operation not supported");
    }
}
