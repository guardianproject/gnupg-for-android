/*
 * $Id: GnuPGPassphraseListener.java,v 1.1 2005/01/24 13:56:58 stefan Exp $
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
 * This is the listener interface you need to implement, if you want to react to
 * the passphrase callbacks of the gpgme library for yourself. In this way, you
 * can for example lookup passphrases from a database or so and return them to
 * gpgme. There are also two pre-fabricated listener.
 * 
 * @see com.freiheit.gnupg.GnuPGPassphraseWindow
 * @see com.freiheit.gnupg.GnuPGPassphraseConsole
 * @author Stefan Richter, stefan@freiheit.com
 */
public interface GnuPGPassphraseListener {
    /**
     * This method will be called by gpgme, if a passphrase is necessary to
     * complete a crypto operation. Implement this interface and register it
     * with the GnuPGContext on which you are operating.
     * 
     * @param hint TODO
     * @param passphraseInfo TODO
     * @param wasBad TODO
     * @return passphrase to be supplied to gpgme callback (MUST include a \n at
     *         the end of the string)
     * @see com.freiheit.gnupg.GnuPGContext
     */
    public String getPassphrase(String hint, String passphraseInfo, long wasBad);
}
