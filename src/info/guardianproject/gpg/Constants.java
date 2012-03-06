package info.guardianproject.gpg;

import info.guardianproject.gpg.screens.GenerateNewKeyActivity;
import info.guardianproject.gpg.screens.KeyEditorActivity;
import info.guardianproject.gpg.screens.ViewAllKeysActivity;
import info.guardianproject.gpg.screens.ViewMyKeysActivity;
import info.guardianproject.gpg.screens.WebOfTrustActivity;

public interface Constants {
	public final static String LOG = "***** GPG UI *****";
	public final static String LOG_NH = "NativeHelper";
	public final static String LOG_AS = "AgentsService";
	public static final String LOG_UPDATE = "LOG_UPDATE";
	public static final String COMMAND_FINISHED = "COMMAND_FINISHED";
	
	public static class KeyManager {
		public final static String TAG = "keyManagerGroup";
		public final static Class<?> ROOT = KeyManagerGroup.class;
		
		public final static String[] VIEWS = {"viewAllKeys"};
		public final static Class<?>[] TARGETS = {ViewAllKeysActivity.class};
		
	}
	
	public static class MyKeys {
		public final static String TAG = "myKeysGroup";
		public final static Class<?> ROOT = MyKeysGroup.class;
		
		public final static String[] VIEWS = {"viewMyKeys", GenerateNewKey.TAG, KeyEditor.TAG};
		public final static Class<?>[] TARGETS = {ViewMyKeysActivity.class, GenerateNewKeyActivity.class, KeyEditorActivity.class};
		
		public final static String ACTION = "myKeysAction";
		
		public final static class Actions {
			public final static int GO_TO_EDITOR = 500;
		}
	}
	
	public static class WebOfTrust {
		public final static String TAG = "webOfTrustGroup";
		public final static Class<?> ROOT = WebOfTrustActivity.class;
		
		public final static String[] VIEWS = {"webOfTrustOverview"};
		public final static Class<?>[] TARGETS = {WebOfTrustActivity.class};
	}
	
	public static class FileManager {
		public final static String ACTION = "fileManagerAction";
		public final static int REQUEST_CODE = 300;
		
		public static class Actions {
			public final static int IMPORT_KEY = 1;
		}
	}
	
	public static class SearchForKey {
		public final static String TAG = "searchForKey";
		
		public static class Actions {
			public final static String SEARCH_FOR_KEY = "performSearch";
		}
		
		public static class Views {
			public final static String MAIN = "searchForKey_main";
			public final static String SPINNER = "searchForKey_spinner";
		}
	}
	
	public static class GenerateNewKey {
		public final static String TAG = "generateNewKey";
		
		public static class Actions {
			public final static String GENERATE_NEW_KEY = "generateNewKey";
		}
		
		public static class Views {
			public final static String MAIN = "generateNewKey_main";
			public final static String SPINNER = "generateNewKey_spinner";
			public final static String FINISH = "generateNewKey_finish";
		}
	}
	
	public static class KeyEditor {
		public final static String TAG = "keyEditor";
	}
	
	public static class GPGProgressDialog {
		public final static String TAG = "spinnerActivity";
		public final static String INHERITED_TITLE = "inheritedTitle";
		public final static String ACTION = "threadedActions";
		public final static String RESULT = "threadedResults";
		
		public static class Actions {
			public final static String THREAD = "actionToPerform";
			public final static String PARAMETERS = "actionPerameters";
		}
		
		public static class Results {
			public final static String THREAD = "actionPerformed";
			public final static String DATA = "data";
		}
	}
	
	public static class Overlay {
		public final static String TARGET = "overlayTarget";
		public final static int REQUEST_CODE = 301;
		public final static String RESULT_DATA = "returnedValues";
		public final static String BOOTSTRAPPED_DATA = "bootstrappedData";
		
		public static class Targets {
			public final static int SEARCH_FOR_KEY = 400;
			public final static int GENERATE_NEW_KEY = 401;
		}
	}
	
	public static class GPG {
		public static class Commands {
			public final static String LIST_KEYS = "./gpg2 --list-keys";
			public final static String SEARCH_KEYS = "./gpg2 --search-keys %sk"; // plus search param
			public final static String RUN_TEST = "./gpg2 --version";
			public final static String BATCH_GEN = "%echo Generating a basic OpenPGP key\n" +
					"Key-Type: %kt\n" +
					"Key-Length: %kl\n" +
					"Subkey-Type: %skt\n" +
					"Subkey-Length: %skl\n" +
					"Name-Real: %nr\n" +
					"Name-Comment: %nc\n" +
					"Name-Email: %ne\n" +
					"Expire-Date: %ed\n" +
					"%no-ask-passphrase\n" +
					"%no-protection\n" +
					"%commit\n" +
					"%echo done";
			public final static String GENERATE_KEY = "./gpg2 --batch --gen-key %bf"; // plus path to batch.txt
		}
		
		public static class Replace {
			public final static String KEY_TYPE = "%kt";
			public final static String KEY_LENGTH = "%kl";
			public final static String SUBKEY_TYPE = "%skt";
			public final static String SUBKEY_LENGTH = "%skl";
			public final static String NAME_REAL = "%nr";
			public final static String NAME_EMAIL = "%ne";
			public final static String NAME_COMMENT = "%nc";
			public final static String EXPIRE_DATE = "%ed";
			public final static String PATH_TO_BATCH = "%bf";
			public final static String SEARCH_KEYS = "%sk";
		}
	}
	
	public static class Keys {
		public final static String KEY_ID = "keyIdContext";
	}
}
