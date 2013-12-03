
package info.guardianproject.gpg;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class DebugStreamThread extends Thread {
    public static final String TAG = "DebugStreamThread";

    InputStream i;
    StreamUpdate update;

    DebugStreamThread(InputStream i) {
        this.i = i;
    }

    DebugStreamThread(InputStream i, StreamUpdate update) {
        this.i = i;
        this.update = update;
    }

    @Override
    public void run() {
        try {
            byte[] readBuffer = new byte[512];
            int readCount = -1;
            while ((readCount = i.read(readBuffer)) > 0) {
                String readString = new String(readBuffer, 0, readCount);
                update.update(readString);
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
    }

    public String dump() {
        if (update instanceof StringBufferStreamUpdate) {
            return ((StringBufferStreamUpdate) update).dump();
        }

        return null;
    }

    public static abstract class StreamUpdate {
        public abstract void update(String val);
    }

    public class StringBufferStreamUpdate extends StreamUpdate {
        StringBuilder sb = new StringBuilder();

        @Override
        public void update(String val) {
            sb.append(val);
        }

        public String dump() {
            return sb.toString();
        }
    }
}
