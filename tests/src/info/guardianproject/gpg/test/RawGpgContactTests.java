
package info.guardianproject.gpg.test;

import info.guardianproject.gpg.NativeHelper;
import info.guardianproject.gpg.sync.RawGpgContact;
import info.guardianproject.gpg.sync.RawGpgContact.KeyFlags;
import android.test.AndroidTestCase;
import android.util.Log;

public class RawGpgContactTests extends AndroidTestCase {
    public static final String TAG = "RawGpgContactTests";

    protected void setUp() throws Exception {
        Log.i(TAG, "setUp");
        super.setUp();
        NativeHelper.setup(getContext());
    }

    protected void tearDown() throws Exception {
        Log.i(TAG, "tearDown");
        super.tearDown();
    }

    public void testFlags() {
        RawGpgContact first, second;
        for (int i = 0; i < 512; i++) {
            Log.v(TAG, "i = " + i);
            first = new RawGpgContact("Testy McTest",
                    "test@test.com",
                    "nothing to see here",
                    "5E61C8780F86295CE17D86779F0FE587374BBE81",
                    i,
                    0,
                    false);
            int flags = (first.canEncrypt ? 1 << KeyFlags.canEncrypt : 0)
                    + (first.canSign ? 1 << KeyFlags.canSign : 0)
                    + (first.hasSecretKey ? 1 << KeyFlags.hasSecretKey : 0)
                    + (first.isDisabled ? 1 << KeyFlags.isDisabled : 0)
                    + (first.isExpired ? 1 << KeyFlags.isExpired : 0)
                    + (first.isInvalid ? 1 << KeyFlags.isInvalid : 0)
                    + (first.isQualified ? 1 << KeyFlags.isQualified : 0)
                    + (first.isRevoked ? 1 << KeyFlags.isRevoked : 0)
                    + (first.isSecret ? 1 << KeyFlags.isSecret : 0);
            Log.v(TAG, i + " == " + flags);
            assertTrue(i == flags);
            second = new RawGpgContact("Testy McTest",
                    "test@test.com",
                    "nothing to see here",
                    "5E61C8780F86295CE17D86779F0FE587374BBE81",
                    flags,
                    0,
                    false);
            assertTrue(first.flags == second.flags);
        }
    }
}
