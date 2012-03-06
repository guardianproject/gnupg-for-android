package info.guardianproject.gpg.action;

import info.guardianproject.gpg.Constants;
import android.util.Log;

public class InterfaceActions implements Constants {
	@SuppressWarnings("unused")
	private static String performSearch(String queryParam) {
		Log.d(LOG, "Searching keyservers for " + queryParam);
		return "Searching keyservers for " + queryParam; 
	}
	
	@SuppressWarnings("unused")
	private static String generateNewKey(String batchCommand) {
		Log.d(LOG, "generating a new key with command:\n" + batchCommand);
		return "generated a key";
	}
}
