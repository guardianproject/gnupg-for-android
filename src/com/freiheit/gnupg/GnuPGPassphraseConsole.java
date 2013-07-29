/*
 * $Id: GnuPGPassphraseConsole.java,v 1.1 2005/01/24 13:56:58 stefan Exp $
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Requests a passphrase for a crypto operation from the command line. This is
 * triggered by gpgme. You must register this as a listener to the GnuPGContext.
 * <b>It stills echoes the passphrase on the console. Remember, this is an alpha
 * release...</b>
 * 
 * @see com.freiheit.gnupg.GnuPGContext
 * @author Stefan Richter, stefan@freiheit.com
 */
public class GnuPGPassphraseConsole implements GnuPGPassphraseListener {
    private BufferedReader _reader;

    /**
     * Default-Constructor.
     */
    public GnuPGPassphraseConsole() {
        _reader = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Prints to the console, asks for the passphrase and returns it to gpgme.
     */
    public String getPassphrase(String hint, String passphraseInfo, long wasBad) {
        StringBuffer prompt = new StringBuffer("Enter GnuPG Passphrase (");
        prompt.append(hint).append("): ");
        System.out.print(prompt.toString());
        String line = null;
        try {
            line = _reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return line;
    }
}
