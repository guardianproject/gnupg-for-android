package info.guardianproject.gpg.adapters;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class GPGHeaderTextView extends TextView {
	Context c;
	
	public GPGHeaderTextView(Context context) {
		super(context);
		this.c = context;
	}
	
	public GPGHeaderTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.c = context;
	}
	
	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text.toString().toUpperCase(), BufferType.NORMAL);
	}

}
