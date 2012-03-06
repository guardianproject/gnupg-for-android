package info.guardianproject.gpg.render;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateParser {
	static SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
	
	public static long getDateAsMillis(int d, int m, int y) throws ParseException {
		Date date = format.parse(y + "/" + (m + 1) + "/" + d);
		return date.getTime();
	}
}
