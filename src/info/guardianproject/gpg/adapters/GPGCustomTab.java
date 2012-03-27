package info.guardianproject.gpg.adapters;

import info.guardianproject.gpg.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabWidget;
import android.widget.TextView;

public class GPGCustomTab {
	public View tab;
	
	public GPGCustomTab(Context context, String title, TabWidget tabWidget) {
		tab = LayoutInflater.from(context).inflate(R.layout.gpgtablayout, tabWidget, false);
		TextView indicator = (TextView) tab.findViewById(R.id.tabIndicator);
		indicator.setText(title.toUpperCase());
	}
	
}
