/*
 * $Id$
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
/**
 * 
 */

package com.freiheit.gnupg;

/**
 * Class to handle Generate Key Results<br>
 * Created on: 23. Mai 06<br>
 * 
 * @author <a href="mailto:stefan.neumann@freiheit.com">Stefan Neumann</a>
 * @version $Id$
 */
/**
 * TODO: DESCRIBE ME<br>
 * Created on: 23. Mai 06<br>
 * 
 * @author <a href="mailto:stefan.neumann@freiheit.com">Stefan Neumann</a>
 * @version $Id$
 */
/**
 * TODO: DESCRIBE ME<br>
 * Created on: 23. Mai 06<br>
 * 
 * @author <a href="mailto:stefan.neumann@freiheit.com">Stefan Neumann</a>
 * @version $Id$
 */
public class GnuPGGenkeyResult {

    private String _fpr;
    private boolean _primary;
    private boolean _sub;

    protected GnuPGGenkeyResult() {
    }

    /**
     * This is the fingerprint of the key that was created. If both a primary
     * and a sub key were generated, the fingerprint of the primary key will be
     * returned. If the crypto engine does not provide the fingerprint, `it will
     * return a null pointer.
     * 
     * @return fingerprint of the created key
     */
    public String getFpr() {
        return _fpr;
    }

    /**
     * This is a flag that is set to true if a primary key was created and to
     * false if not.
     * 
     * @return flag, if a primary key was created
     */
    public boolean isPrimary() {
        return _primary;
    }

    /**
     * This is a flag that is set to true if a subkey was created and to false
     * if not.
     * 
     * @return flag, if a subkey was created
     */
    public boolean isSub() {
        return _sub;
    }
}
