package info.guardianproject.gpg.sync;

import com.freiheit.gnupg.GnuPGKey;

import info.guardianproject.gpg.GnuPG;

import java.util.ArrayList;
import java.util.List;

public class RawContact {

    private final String mFullName;
    private final String mEmail;
    private final String mComment;
    private final String mReadableFingerprint;
    private final String mFingerprint;
    private final long mRawContactId;
    private final boolean mDeleted;

    public static List<RawContact> fromPublicKeys() {
        GnuPGKey[] keys = GnuPG.context.listKeys();
        if( keys == null ) return new ArrayList<RawContact>();
        ArrayList<RawContact> list = new ArrayList<RawContact>(keys.length);
        for( GnuPGKey key : keys ) {
            list.add( RawContact.fromKey(key) );
        }
        return list;
    }

    public static RawContact fromKey(GnuPGKey key) {
        return new RawContact(key.getName(),
                                      key.getEmail(),
                                      key.getComment(),
                                      key.getFingerprint(),
                                      key.getFingerprint(),
                                      0,
                                      false);

    }

    public RawContact(String name, String email, String comment, String readableFpr, String fingerprint, long rawContactId, boolean deleted) {
        super();
        this.mFullName = name;
        this.mEmail = email;
        this.mComment = comment;
        this.mReadableFingerprint = readableFpr;
        this.mFingerprint = fingerprint;
        this.mRawContactId = rawContactId;
        this.mDeleted = deleted;
    }


    public String getFullName() {
        return mFullName;
    }


    public String getEmail() {
        return mEmail;
    }


    public String getComment() {
        return mComment;
    }

    public String getReadableFingerprint() {
        return mReadableFingerprint;
    }

    public String getFingerprint() {
        return mFingerprint;
    }

    public long getRawContactId() {
        return mRawContactId;
    }

    public boolean isDeleted() {
        return mDeleted;
    }


}
