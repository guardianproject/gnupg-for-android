/*
 * $Id: GnuPGException.java,v 1.1 2005/01/24 13:56:58 stefan Exp $
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
 * If the underlying gpgme library reports an error, this exception is thrown.
 * Wraps all errors messages from inside gpgme.
 * 
 * @author Stefan Richter, stefan@freiheit.com
 */
public class GnuPGException extends RuntimeException {

    private static final long serialVersionUID = -775599686124698560L;

    /**
     * This Exception is normally only thrown from within the native part of
     * this library.
     * 
     * @param msg is an error message text from gpgme
     */
    GnuPGException(String msg) {
        super(msg);
    }
}
