/*
 * $Id: GnuPGData.java,v 1.1 2005/01/24 13:56:58 stefan Exp $
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
   Holds the data that you want to work on and stores the results
   of crypto operations.

   @author Stefan Richter, stefan@freiheit.com
 */
public class GnuPGData extends GnuPGPeer{

    /**
       Use the factory methods from GnuPGContext to create GnuPGData instances.
       Generates an empty data object. Use this, if you want
       to create a data object to hold a result from a crypto
       operation.
     */
    protected GnuPGData(){
        setInternalRepresentation(gpgmeDataNew());
    }

    /**
     * Use the factory methods in GnuPGContext to create GnuPGData instances.
     * Generates a new data object based on read/write stream access to a file.
     * 
     * @param file your file
     * @throws IOException
     */
    protected GnuPGData(File file) throws IOException, GnuPGException {
        this(file, "r+");
    }

    /**
     * Use the factory methods in GnuPGContext to create GnuPGData instances.
     * Generates a new data object based on stream access to a file, with
     * settable open mode.
     * 
     * @param file your file
     * @param mode the POSIX mode to open the file, e.g "r" or "w+"
     * @throws IOException
     */
    protected GnuPGData(File file, String mode) throws IOException, GnuPGException {
        if (file == null || !file.exists())
            throw new FileNotFoundException();
        if (!file.canRead())
            throw new IOException("Cannot read: " + file);
        if (!file.canRead())
            throw new IOException("Is a directory: " + file);
        setInternalRepresentation(gpgmeDataNewFromFilename(file.getCanonicalPath(), mode));
    }

    /**
       Use the factory methods from GnuPGContext to create GnuPGData instances.
       Generates a new data object containing the given String.

       @param str your string
     */
    protected GnuPGData(String str){
        this(str.getBytes());
    }

    /**
       Use the factory methods from GnuPGContext to create GnuPGData instances.
       Generates a new Data-Object containing the given byte array.

       @param data your data
     */
    protected GnuPGData(byte[] data){
        long gpgmeDataNewFromMem = gpgmeDataNewFromMem(data);
        setInternalRepresentation(gpgmeDataNewFromMem);
    }


    /*
       FIXME: This is not working! Use it, and it will crash the JVM.
    */
     public void read(InputStream in) throws IOException{
         if(in != null){
             gpgmeDataRead(getInternalRepresentation(), in);
         }
     }

    /**
       Writes the data/string contained in this data object
       to the given Java OutputStream.

       @param out OutputStream, where the data/string should be written
     */
    public void write(OutputStream out) throws IOException{
        if (out != null) {
            gpgmeDataWrite(getInternalRepresentation(), out);
        }
    }

    /**
       Helper method to print out the data/string from this data object.

       @return String representation of the data contained in this data object (expect weird results with binary data)
     */
    @Override
    public String toString(){
        String result = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(this.size());
        try{
            this.write(baos);
            result = baos.toString();
        }
        catch(IOException ioe){
            result = ioe.getMessage();
        }
        return result;
    }

    /**
       This calls immediately the release method for the datastructure
       in the underlying gpgme library. This method is called by
       the finalizer of the class anyway, but you can call it yourself
       if you want to get rid of this datastructure at once and don't want to
       wait for the non-deterministic garbage-collector of the JVM.
    */
    public void destroy(){
        if(getInternalRepresentation() != 0){
            gpgmeDataRelease(getInternalRepresentation());
            setInternalRepresentation(0);
        }
    }

    /**
       Releases underlying datastructures. Simple calls the destroy() method.
     */
    @Override
    protected void finalize(){
        destroy();
    }

    public int size() {
        return gpgmeSize(getInternalRepresentation());
    }

    private native int gpgmeSize(long l);
    private native long gpgmeDataNewFromMem(byte[] plain);
    private native long gpgmeDataNewFromFilename(String filename, String mode);
    private native long gpgmeDataNew();
    private native void gpgmeDataWrite(long l, OutputStream out) throws IOException;
    private native void gpgmeDataRelease(long l);
    private native void gpgmeDataRead(long data, InputStream in) throws IOException;
}
