package info.guardianproject.gpg.pinentry;

public class PinentryStruct {
	public enum PinentryColor {
		PINENTRY_COLOR_NONE, PINENTRY_COLOR_DEFAULT,
		  PINENTRY_COLOR_BLACK, PINENTRY_COLOR_RED,
		  PINENTRY_COLOR_GREEN, PINENTRY_COLOR_YELLOW,
		  PINENTRY_COLOR_BLUE, PINENTRY_COLOR_MAGENTA,
		  PINENTRY_COLOR_CYAN, PINENTRY_COLOR_WHITE
	}
	public String title;
	public String description;
	public String error;
	public String prompt;
	public String ok;
	public String notok;
	public String cancel;
	public byte[] pin;
	public int pin_len;
	public String display;
	public String ttyname;
	public String ttytpe;
	public String lc_ctype;
	public String lc_messages;
	public boolean debug;
	public int timeout;
	public int grab;
	public int parent_wid;
	public String touch_file;
	public int result;
	public int canceled;
	public int locale_err;
	public int close_button;
	public int one_button;
	public String quality_bar;
	public String quality_bar_tt;
	public PinentryColor color_fg;
	public int color_fg_bright;
	public PinentryColor color_bg;
	public PinentryColor color_so;
	public 	int color_so_bright;
	public String default_ok;
	public String default_cancel;
	public String default_prompt;

}
