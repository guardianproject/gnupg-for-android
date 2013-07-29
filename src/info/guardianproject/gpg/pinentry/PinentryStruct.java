
package info.guardianproject.gpg.pinentry;

/**
 * This class is tightly coupled with the C/JNI code in
 * jni/pinentry_cmd_handler.c DO NOT change this class without checking how it
 * will affect the JNI.
 */
public class PinentryStruct {
    public enum PinentryColor {
        PINENTRY_COLOR_NONE, PINENTRY_COLOR_DEFAULT,
        PINENTRY_COLOR_BLACK, PINENTRY_COLOR_RED,
        PINENTRY_COLOR_GREEN, PINENTRY_COLOR_YELLOW,
        PINENTRY_COLOR_BLUE, PINENTRY_COLOR_MAGENTA,
        PINENTRY_COLOR_CYAN, PINENTRY_COLOR_WHITE
    }

    public String title = new String();
    public String description = new String();
    public String error = new String();
    public String prompt = new String();
    public String ok = new String();
    public String notok = new String();
    public String cancel = new String();
    public String pin = new String();
    public int pin_len;
    public String display = new String();
    public String ttyname = new String();
    public String ttytpe = new String();
    public String lc_ctype = new String();
    public String lc_messages = new String();
    public boolean debug;
    public int timeout;
    public int grab;
    public int parent_wid;
    public String touch_file = new String();
    public int result;
    public int canceled;
    public int locale_err;
    public int close_button;
    public int one_button;
    public String quality_bar = new String();
    public String quality_bar_tt = new String();
    public PinentryColor color_fg;
    public int color_fg_bright;
    public PinentryColor color_bg;
    public PinentryColor color_so;
    public int color_so_bright;
    public String default_ok = new String();
    public String default_cancel = new String();
    public String default_prompt = new String();

    public int isButtonBox = 1; // C-style boolean

}
