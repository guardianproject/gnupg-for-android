/*
 * $Id: GnuPGSignature.java,v 1.1 2005/01/24 13:56:58 stefan Exp $
 * (c) Copyright 2005 freiheit.com technologies gmbh, Germany.
 *
 * This file is part of Java for GnuPG  (http://www.freiheit.com).
 *
 * Java for GnuPG is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * Please see COPYING for the complete licence.
 */

package com.freiheit.gnupg;

/**
 * Represents a signature from a key. A signature doesn't stand alone. It is
 * always bound to a key. So if you want to see a signature, you first have to
 * get a key via the GnuPGContext.
 * <p>
 * This class accesses directly the corresponding structure "under the hood" and
 * stores no data in java members. This means, that every call of a method also
 * results in a native call to access gpgme memory.
 * 
 * @author Stefan Richter, stefan@freiheit.com
 * @see com.freiheit.gnupg.GnuPGContext
 */
public class GnuPGSignature extends GnuPGPeer {

    /**
     * This constructor is only called from GnuPGKey. It is called when the lib
     * browses thru the linked list of signatures of each key.
     * 
     * @see com.freiheit.gnupg.GnuPGKey
     */
    protected GnuPGSignature(long ptr) {
        // note that this is a pointer to an address in the gnupg-for-java shared lib
        setInternalRepresentation(ptr);
    }

    /**
     * Is signature key revoked?
     * 
     * @return true, if revoked
     */
    public boolean isRevoked() {
        return gpgmeGetRevoked(getInternalRepresentation());
    }

    /**
     * Is signature key expired?
     * 
     * @return true, if expired
     */
    public boolean isExpired() {
        return gpgmeGetExpired(getInternalRepresentation());
    }

    /**
     * Is signature key invalid?
     * 
     * @return true, if invalid
     */
    public boolean isInvalid() {
        return gpgmeGetInvalid(getInternalRepresentation());
    }

    /**
     * Is signature key exportable?
     * 
     * @return true, if exportable
     */
    public boolean isExportable() {
        return gpgmeGetExportable(getInternalRepresentation());
    }

    /**
     * Returns Key-ID of signature key.
     * 
     * @return Key-ID
     */
    public String getKeyID() {
        return gpgmeGetKeyID(getInternalRepresentation());
    }

    /**
     * Returns User-ID of signer.
     * 
     * @return User-ID
     */
    public String getUserID() {
        return gpgmeGetUserID(getInternalRepresentation());
    }

    /**
     * Returns Name of signer.
     * 
     * @return Name
     */
    public String getName() {
        return gpgmeGetName(getInternalRepresentation());
    }

    /**
     * Returns Email-Address of signer.
     * 
     * @return Email-Address
     */
    public String getEmail() {
        return gpgmeGetEmail(getInternalRepresentation());
    }

    /**
     * Returns Comment.
     * 
     * @return Comment
     */
    public String getComment() {
        return gpgmeGetComment(getInternalRepresentation());
    }

    /**
     * Checks, if all signature details are available.
     * 
     * @return true, if more info than the keyid is available
     */
    public boolean hasDetails() {
        return !getName().equals("");
    }

    /**
     * String-Representation of this Signature.
     * 
     * @return String single line of text with name, comment etc.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getKeyID());
        if (hasDetails()) {
            buf.append(", ");
            buf.append(getName());
            buf.append(", ");
            buf.append(getComment());
            buf.append(", ");
            buf.append(getEmail());
        }
        else {
            buf.append(" <No details available.>");
        }
        return buf.toString();
    }

    /**
     * Gets the next pointer in the gpgme_key_sig_t structure.
     * 
     * @return new GnuPGSignature-Object
     */
    protected GnuPGSignature getNextSignature() {
        GnuPGSignature result = null;

        // note that this is a pointer to an address in the gnupg-for-java shared lib
        long next = gpgmeGetNextSignature(getInternalRepresentation());

        if (next != 0) {
            result = new GnuPGSignature(next);
        }

        return result;
    }

    // native method declarations:
    private native boolean gpgmeGetRevoked(long l);

    private native boolean gpgmeGetExpired(long l);

    private native boolean gpgmeGetInvalid(long l);

    private native boolean gpgmeGetExportable(long l);

    private native String gpgmeGetKeyID(long l);

    private native String gpgmeGetUserID(long l);

    private native String gpgmeGetName(long l);

    private native String gpgmeGetEmail(long l);

    private native String gpgmeGetComment(long l);

    private native long gpgmeGetNextSignature(long l);

}
