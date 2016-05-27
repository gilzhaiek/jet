package com.reconinstruments.example.widgets;

import java.util.Calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

public class ReconDatePickerDialog extends AlertDialog {

    final static String[] mMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    final static int[] daysInMonth = new int[] { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    
    ReconDateTimeSetListener mListener;
    
    public ReconDatePickerDialog(Context context, ReconDateTimeSetListener listener) {
	super(context);
	mListener = listener;
	initDialog(context);
    }
        
    private void initDialog(Context context) {
	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	View l = inflater.inflate(R.layout.date_picker_dialog, null);
	
	int baseYr = Calendar.getInstance().get(Calendar.YEAR);
	
	final ReconPickerView monthPicker = (ReconPickerView) l.findViewById(R.id.monthPicker);
	final ReconPickerView dayPicker = (ReconPickerView) l.findViewById(R.id.dayPicker);
	final ReconPickerView yearPicker = (ReconPickerView) l.findViewById(R.id.yearPicker);
	monthPicker.setContent(mMonths);
	dayPicker.setContent(1, 1, 31);
	yearPicker.setContent(baseYr, 0, 3000);
	
	monthPicker.setOnChangeListener(new ReconPickerChangeListener() {
	    @Override
	    public void onChange(ReconPickerView view, String value, boolean actionUp) {
		int currentDay = (int) Integer.parseInt(dayPicker.getSelectedValue());
		int year = (int) Integer.parseInt(yearPicker.getSelectedValue());
		int t = monthPicker.getSelectedIndex();
		int daysInThisMonth;
		
		if (isLeapYear(year) && t == 1) {
		    daysInThisMonth = 29;
		} else {
		    daysInThisMonth = daysInMonth[t];
		}
		
		if (currentDay > daysInThisMonth) {
		    dayPicker.setContent(1, 1, daysInThisMonth);
		} else {
		    dayPicker.setContent(currentDay, 1, daysInThisMonth);
		}
	    }
	});
	
	yearPicker.setOnChangeListener(new ReconPickerChangeListener() {
	    @Override
	    public void onChange(ReconPickerView view, String value, boolean actionUp) {
		int year = Integer.parseInt(value);
		int month = monthPicker.getSelectedIndex();
		int day = Integer.parseInt(dayPicker.getSelectedValue());
		if (month == 1) {
		    if (isLeapYear(year)) {
			if (day > 28) {
			    dayPicker.setContent(1, 1, 29);
			} else {
			    dayPicker.setContent(day, 1, 29);
			}
		    } else {
			if (day > 28) {
			    dayPicker.setContent(1, 1, 28);
			} else {
			    dayPicker.setContent(day, 1, 28);
			}
		    }
		}
		
	    }
	});
	
	this.setButton(BUTTON_POSITIVE, "Set", new OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		if (mListener != null) {
		    String month = monthPicker.getSelectedValue();
		    int day = (int) Integer.parseInt(dayPicker.getSelectedValue());
		    int year = (int) Integer.parseInt(yearPicker.getSelectedValue());
		    mListener.onDateSet(ReconDatePickerDialog.this, month, day, year);
		}
	    } 
	});
	this.setButton(BUTTON_NEGATIVE, "Cancel", new OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		ReconDatePickerDialog.this.dismiss();
	    } 
	});

	this.setView(l);
    }
    
    private boolean isLeapYear(int year) {
	// Logic from wikipedia.
	
	if (year % 400 == 0) {
	    return true;
	} else if (year % 100 == 0) {
	    return false;
	} else if (year % 4 == 0) {
	    return true;
	} else {
	    return false;
	}
	
    }

}
