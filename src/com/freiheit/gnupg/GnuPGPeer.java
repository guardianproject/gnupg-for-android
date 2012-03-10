/*
 * $Id: GnuPGPeer.java,v 1.1 2005/01/24 13:56:58 stefan Exp $
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
   Peer Class to hold the pointers to connect to the underlying gpgme library.
   This is the base class for all GnuPG*.java Classes. DO NOT USE IT.
   It is not intended for library users.

   @author Stefan Richter, stefan@freiheit.com
 */
public class GnuPGPeer{
    // note that this is a pointer to an address in the javagnupg shared lib
    protected long _internalRepresentation;

    /**
       DO NOT USE IT. This is only use from inside the library.
     */
    protected void setInternalRepresentation(long ptr){
         // note that this is a pointer to an address in the javagnupg shared lib
        _internalRepresentation = ptr;
    }

    /**
       DO NOT USE IT. This is only use from inside the library.
     */
    protected long getInternalRepresentation(){
         // note that this is a pointer to an address in the javagnupg shared lib
        return _internalRepresentation;
    }
}
/*
 * Local variables:
 * c-basic-offset: 4
 * indent-tabs-mode: nil
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
