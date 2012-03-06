package info.guardianproject.gpg.render;

import info.guardianproject.gpg.Constants;

import java.io.Serializable;

import android.net.Uri;

public class KeyInfo implements Serializable, Constants {
	private static final long serialVersionUID = -4854608177472610659L;
	
	Uri inApp_uri;
	String inApp_id;
	String inApp_name;
	boolean isPluggedIn;
	Uri contactUri;
	byte[] photo;
	Key[] keys;
	
	public KeyInfo() {
		
	}
	
	private class Key {
		boolean isPrimary;
		KeyId[] ids;
		String footprint;
		String id;
		int algorithmType;
		int length;
		long createdOn;
		long expiresOn;
		Key[] subkeys;
		
		Key() {}
	}
	
	private class KeyId {
		String name;
		String email;
		String comment;
		
		KeyId() {}
		
	}
}
