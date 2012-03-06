package info.guardianproject.gpg.adapters;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.Intent;

public class GPGScreen implements Serializable {
	private static final long serialVersionUID = 5559408559937336138L;
	
	public String label;
	public Intent intent;
	
	public GPGScreen(String label, Intent intent) {
		this.label = label;
		this.intent = intent;
	}
	
	public static GPGScreen getViewFromGroup(ArrayList<GPGScreen> group, String label) throws NullPointerException {
		GPGScreen ret = null;
		for(GPGScreen screen : group) {
			if(screen.label.compareTo(label) == 0)
				ret = screen;
		}
		return ret;
	}
}