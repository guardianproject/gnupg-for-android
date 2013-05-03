package info.guardianproject.gpg;

public class Posix {

    static {
        System.loadLibrary("posix");
    }

    public static native int umask(int mask);
}
