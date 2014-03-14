/*
 * $Id: GnuPGKey.java,v 1.1 2005/01/24 13:56:58 stefan Exp $
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a key. You can manage and find keys using GnuPGContext. You can
 * not instantiate a key by yourself. You always need a context.
 * 
 * @see com.freiheit.gnupg.GnuPGContext
 * @author Stefan Richter, stefan@freiheit.com
 */
public class GnuPGKey extends GnuPGPeer {
    /**
     * This constructor is only called from within the JNI routines. This is for
     * example used, when a keylist search returns keys. This is how then each
     * key is instantiated on the java side from c.
     */
    protected GnuPGKey(long ptr) {
        // note that this is a pointer to an address in the gnupg-for-java shared lib
        setInternalRepresentation(ptr);
    }

    /**
     * Fetch a public key by its 16 hex char fingerprint String
     * 
     * @param context - the current GnuPGContext
     * @param fingerprint - the 16 hex char fingerprint String
     */
    protected GnuPGKey(GnuPGContext context, String fingerprint) {
        setInternalRepresentation(gpgmeGetKey(context.getInternalRepresentation(), fingerprint,
                false));
    }

    /**
     * Fetch a key by its 16 hex char fingerprint String
     * 
     * @param context the current GnuPGContext
     * @param fingerprint the 16 hex char fingerprint String
     * @param secret_only whether to return only secret keys
     */
    protected GnuPGKey(GnuPGContext context, String fingerprint, boolean secret_only) {
        setInternalRepresentation(gpgmeGetKey(context.getInternalRepresentation(), fingerprint,
                secret_only));
    }

    /**
     * Get the Name of the default key/userid.
     */
    public String getName() {
        return gpgmeGetName(getInternalRepresentation());
    }

    /**
     * Get the Email-Address of the default key/userid.
     */
    public String getEmail() {
        return gpgmeGetEmail(getInternalRepresentation());
    }

    /**
     * Get the Key-ID of the default key/userid.
     */
    public String getKeyID() {
        return gpgmeGetKeyID(getInternalRepresentation());
    }

    /**
     * Get the Fingerprint of the default key/userid.
     */
    public String getFingerprint() {
        return gpgmeGetFingerprint(getInternalRepresentation());
    }

    /**
     * Get the Comment of the default key/userid.
     */
    public String getComment() {
        return gpgmeGetComment(getInternalRepresentation());
    }

    /**
     * Get the User-ID of the default key/userid.
     */
    public String getUserID() {
        return gpgmeGetUserID(getInternalRepresentation());
    }

    public boolean canEncrypt() {
        return gpgmeCanEncrypt(getInternalRepresentation());
    }

    public boolean canSign() {
        return gpgmeCanSign(getInternalRepresentation());
    }

    public boolean canCertify() {
        return gpgmeCanCertify(getInternalRepresentation());
    }

    public boolean canAuthenticate() {
        return gpgmeCanAuthenticate(getInternalRepresentation());
    }

    public boolean isRevoked() {
        return gpgmeIsRevoked(getInternalRepresentation());
    }

    public boolean isExpired() {
        return gpgmeIsExpired(getInternalRepresentation());
    }

    public boolean isDisabled() {
        return gpgmeIsDisabled(getInternalRepresentation());
    }

    public boolean isInvalid() {
        return gpgmeIsInvalid(getInternalRepresentation());
    }

    public boolean isQualified() {
        return gpgmeIsQualified(getInternalRepresentation());
    }

    public boolean isSecret() {
        return gpgmeIsSecret(getInternalRepresentation());
    }

    public boolean hasSecretKey() {
        return gpgmeHasSecretKey(getInternalRepresentation());
    }

    /**
     * Lists all signatures of the default key/userid. Every key can have
     * multiple signatures. Signatures can be incomplete. This means, that not
     * all details (name, email-address etc.) were downloaded from a keyserver.
     * But you will at least see the key-id of the signature. Use GnuPG to
     * --refresh-keys if you want to see all signature details.
     * <p>
     * Currently I am not supporting java generics for a type safe iterator,
     * because there are to many people still using jdk-1.4.x without generics
     * support. This will be changed on increasing demand.
     * <p>
     * 
     * @return Iterator of GnuPGSignature objects
     * @see com.freiheit.gnupg.GnuPGSignature
     */
    public Iterator<GnuPGSignature> getSignatures() {
        List<GnuPGSignature> siglist = null;
        GnuPGSignature sig = getSignature();
        while (sig != null) {
            if (siglist == null) {
                siglist = new ArrayList<GnuPGSignature>();
            }
            siglist.add(sig);
            sig = sig.getNextSignature();
        }
        if (siglist == null)
            return null;
        else
            return siglist.listIterator();
    }

    /**
     * Helper to list signatures in the toString()-method.
     */
    private String listSignatures() {
        Iterator<GnuPGSignature> iter = getSignatures();
        GnuPGSignature sig;
        StringBuffer buf = new StringBuffer();

        while (iter != null && iter.hasNext()) {
            sig = (GnuPGSignature) iter.next();
            buf.append("\t").append(sig).append("\n");
        }

        return buf.toString();
    }

    /**
     * Helper to get the head of the linked list of signatures from the native
     * space.
     */
    private GnuPGSignature getSignature() {
        GnuPGSignature result = null;

        // note that thhis is a pointer to an address in the gnupg-for-java shared
        // lib
        long ptr = gpgmeGetSignature(getInternalRepresentation());

        if (ptr != 0) {
            result = new GnuPGSignature(ptr);
        }

        return result;
    }

    /**
     * Return this key with all of its signatures.
     * 
     * @return String multiline text with one key and 0..many signatures
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getKeyID()).
                append(": ").
                append(getName()).
                append(", ").
                append(getComment()).
                append(", ").
                append(getEmail()).
                append(", ").
                append("[").
                append(getFingerprint()).
                append("]").
                append("\n").
                append(listSignatures());
        return buf.toString();
    }

    /**
     * This calls immediately the release method for the datastructure in the
     * underlying gpgme library. This method is called by the finalizer of the
     * class anyway, but you can call it yourself if you want to get rid of this
     * datastructure at once and don't want to wait for the non-deterministic
     * garbage-collector of the JVM.
     */
    public void destroy() {
        if (getInternalRepresentation() != 0) {
            gpgmeKeyUnref(getInternalRepresentation());
            setInternalRepresentation(0);
        }
    }

    /**
     * Releases underlying datastructures. Simple calls the destroy() method.
     */
    protected void finalize() {
        destroy();
    }

    private native long gpgmeGetKey(long context, String fingerprint, boolean secret);

    private native long gpgmeKeyUnref(long keyptr);

    private native String gpgmeGetName(long keyptr);

    private native String gpgmeGetEmail(long keyptr);

    private native String gpgmeGetKeyID(long keyptr);

    private native String gpgmeGetFingerprint(long keyptr);

    private native String gpgmeGetComment(long keyptr);

    private native String gpgmeGetUserID(long keyptr);

    private native long gpgmeGetSignature(long keyptr);

    private native boolean gpgmeCanEncrypt(long keyptr);

    private native boolean gpgmeCanSign(long keyptr);

    private native boolean gpgmeCanCertify(long keyptr);

    private native boolean gpgmeCanAuthenticate(long keyptr);

    private native boolean gpgmeIsRevoked(long keyptr);

    private native boolean gpgmeIsExpired(long keyptr);

    private native boolean gpgmeIsDisabled(long keyptr);

    private native boolean gpgmeIsInvalid(long keyptr);

    private native boolean gpgmeIsQualified(long keyptr);

    private native boolean gpgmeIsSecret(long keyptr);

    private native boolean gpgmeHasSecretKey(long keyptr);

}
