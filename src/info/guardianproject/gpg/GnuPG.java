package info.guardianproject.gpg;

import com.freiheit.gnupg.GnuPGContext;

public class GnuPG {
	public static GnuPGContext context = null;

	public static void createContext() {
		context = new GnuPGContext();
		// set the homeDir option to our custom home location
		context.setEngineInfo(context.getProtocol(), context.getFilename(),
				NativeHelper.app_home.getAbsolutePath());
	}
}
