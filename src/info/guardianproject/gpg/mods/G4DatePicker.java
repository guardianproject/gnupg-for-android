package info.guardianproject.gpg.mods;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import info.guardianproject.gpg.R;
import info.guardianproject.gpg.utils.Constants;
import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class G4DatePicker extends RelativeLayout implements Constants, OnClickListener {
	Context context;
	TextView month;
	EditText day, year;
	ImageButton monthUp, monthDown, dayUp, dayDown, yearUp, yearDown;
	Calendar cal;
	
	String[] months;
	int[] days;
	int[] props = new int[3];
	View holder;
	
	public static final int IS_AFTER_TODAY = 1;
	
	public G4DatePicker(Context context) {
		super(context);
		this.context = context;
		init();
	}
	
	public G4DatePicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}
	
	private void init() {
		cal = Calendar.getInstance();
		months = context.getResources().getStringArray(R.array.months_short);
		holder = View.inflate(context,  R.layout.g4_date_picker_mod, this);
		props[0] = cal.get(Calendar.MONTH);	//month
		props[1] = cal.get(Calendar.DAY_OF_MONTH);	//day
		props[2] = cal.get(Calendar.YEAR) + 4;	// year + 4
 	}
	
	private InputFilter numFilter = new InputFilter() {

		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			for(int i = start; i < end; i++)
				if(!Character.isDigit(source.charAt(i)))
					return "";
			return null;
		}
		
	};
	
	private InputFilter monthFilter = new InputFilter() {
		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			Log.d(LOG, "text is: " + source);
			return null;
		}
	};
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		for(int i = 0; i < getChildCount(); i++)
			getChildAt(i).layout(l, t, r, b);
		
		if(changed) {
			month = (TextView) holder.findViewById(R.id.month_selectionHolder);
			monthUp = (ImageButton) holder.findViewById(R.id.month_upArrow);
			monthUp.setOnClickListener(this);
			monthDown = (ImageButton) holder.findViewById(R.id.month_downArrow);
			monthDown.setOnClickListener(this);
			
			day = (EditText) holder.findViewById(R.id.day_selectionHolder);
			day.setFilters(new InputFilter[] {numFilter});
			dayUp = (ImageButton) holder.findViewById(R.id.day_upArrow);
			dayUp.setOnClickListener(this);
			dayDown = (ImageButton) holder.findViewById(R.id.day_downArrow);
			dayDown.setOnClickListener(this);
			
			year = (EditText) holder.findViewById(R.id.year_selectionHolder);
			year.setFilters(new InputFilter[] {numFilter});
			yearUp = (ImageButton) holder.findViewById(R.id.year_upArrow);
			yearUp.setOnClickListener(this);
			yearDown = (ImageButton) holder.findViewById(R.id.year_downArrow);
			yearDown.setOnClickListener(this);
			
			resetValues();
			reconfigureAvaliableDays();
		}
	}
	
	private void resetValues() {
		month.setText(months[props[0]]);
		day.setText(Integer.toString(props[1]));
		year.setText(Integer.toString(props[2]));
	}
	
	private void reconfigureAvaliableDays() {
		Calendar mCal = new GregorianCalendar(props[2], props[0], 1);
		days = new int[mCal.getActualMaximum(Calendar.DAY_OF_MONTH)];
		for(int i = 0; i < days.length; i++)
			days[i] = i;
	}
	
	public boolean isValidDate(int additionalFilter) {
		switch(additionalFilter) {
		case IS_AFTER_TODAY:
			if(cal.compareTo(new GregorianCalendar(props[2], props[0], props[1])) != 1)
				return true;
			else
				return false;
		default:
			return false;
		}
	}
	
	public int Month() {
		return props[0];
	}
	
	public int Day() {
		return props[1];
	}
	
	public int Year() {
		return props[2];
	}

	@Override
	public void onClick(View v) {
		if(v == monthUp) {
			if(props[0] != months.length -1)
				props[0] = ++props[0];
			else
				props[0] = 0;
			
		} else if(v == monthDown) {
			if(props[0] != 0)
				props[0] = --props[0];
			else
				props[0] = months.length - 1;
			
		} else if(v == dayUp) {
			if(props[1] != days.length)
				props[1] = ++props[1];
			else
				props[1] = 1;
		} else if(v == dayDown) {
			if(props[1] != 1)
				props[1] = --props[1];
			else
				props[1] = days.length;
		} else if(v == yearUp) {
			props[2] = ++props[2];
		} else if(v == yearDown) {
			props[2] = --props[2];
		}
		
		reconfigureAvaliableDays();
		resetValues();
	}

}
