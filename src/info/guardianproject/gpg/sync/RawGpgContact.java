
package info.guardianproject.gpg.sync;

import info.guardianproject.gpg.GnuPG;

import java.util.ArrayList;
import java.util.List;

import com.freiheit.gnupg.GnuPGKey;

public class RawGpgContact {

    public final String name;
    public final String email;
    public final String comment;
    public final String keyID;
    public final String fingerprint;
    public final int flags;
    public final boolean canEncrypt;
    public final boolean canSign;
    public final boolean hasSecretKey;
    public final boolean isDisabled;
    public final boolean isExpired;
    public final boolean isInvalid;
    public final boolean isQualified;
    public final boolean isRevoked;
    public final boolean isSecret;
    public final long rawContactId;
    public final boolean deleted;

    public static class KeyFlags {
        public static int canEncrypt = 0;
        public static int canSign = 1;
        public static int hasSecretKey = 2;
        public static int isDisabled = 3;
        public static int isExpired = 4;
        public static int isInvalid = 5;
        public static int isQualified = 6;
        public static int isRevoked = 7;
        public static int isSecret = 8;
    }

    public static List<RawGpgContact> fromPublicKeys() {
        GnuPGKey[] keys = GnuPG.context.listKeys();
        if (keys == null)
            return new ArrayList<RawGpgContact>();
        ArrayList<RawGpgContact> list = new ArrayList<RawGpgContact>(keys.length);
        for (GnuPGKey key : keys) {
            list.add(new RawGpgContact(key));
        }
        return list;
    }

    public RawGpgContact(GnuPGKey key) {
        super();
        this.name = key.getName();
        this.email = key.getEmail();
        this.comment = key.getComment();
        this.fingerprint = key.getFingerprint();
        this.keyID = key.getKeyID();
        this.canEncrypt = key.canEncrypt();
        this.canSign = key.canSign();
        this.hasSecretKey = key.hasSecretKey();
        this.isDisabled = key.isDisabled();
        this.isExpired = key.isExpired();
        this.isInvalid = key.isInvalid();
        this.isQualified = key.isQualified();
        this.isRevoked = key.isRevoked();
        this.isSecret = key.isSecret();
        this.flags = (this.canEncrypt ? 1 << KeyFlags.canEncrypt : 0)
                + (this.canSign ? 1 << KeyFlags.canSign : 0)
                + (this.hasSecretKey ? 1 << KeyFlags.hasSecretKey : 0)
                + (this.isDisabled ? 1 << KeyFlags.isDisabled : 0)
                + (this.isExpired ? 1 << KeyFlags.isExpired : 0)
                + (this.isInvalid ? 1 << KeyFlags.isInvalid : 0)
                + (this.isQualified ? 1 << KeyFlags.isQualified : 0)
                + (this.isRevoked ? 1 << KeyFlags.isRevoked : 0)
                + (this.isSecret ? 1 << KeyFlags.isSecret : 0);
        this.rawContactId = 0;
        this.deleted = false;
    }

    public RawGpgContact(String name, String email, String comment, String fingerprint,
            int flags, long rawContactId, boolean deleted) {
        super();
        this.name = name;
        this.email = email;
        this.comment = comment;
        this.fingerprint = fingerprint;
        this.keyID = fingerprint.substring(fingerprint.length() - 16);
        this.flags = flags;
        this.canEncrypt = (flags >> KeyFlags.canEncrypt & 1) != 0;
        this.canSign = (flags >> KeyFlags.canSign & 1) != 0;
        this.hasSecretKey = (flags >> KeyFlags.hasSecretKey & 1) != 0;
        this.isDisabled = (flags >> KeyFlags.isDisabled & 1) != 0;
        this.isExpired = (flags >> KeyFlags.isExpired & 1) != 0;
        this.isInvalid = (flags >> KeyFlags.isInvalid & 1) != 0;
        this.isQualified = (flags >> KeyFlags.isQualified & 1) != 0;
        this.isRevoked = (flags >> KeyFlags.isRevoked & 1) != 0;
        this.isSecret = (flags >> KeyFlags.isSecret & 1) != 0;
        this.rawContactId = rawContactId;
        this.deleted = deleted;
    }
}
