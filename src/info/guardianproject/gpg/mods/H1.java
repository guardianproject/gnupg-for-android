package info.guardianproject.gpg.mods;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class H1 extends TextView {
	public H1(Context context) {
		super(context);
	}
	
	public H1(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}
	
	@Override
	public void setText(CharSequence text, BufferType type) {
		super.setText(text.toString().toUpperCase(), BufferType.NORMAL);
	}
	
}
