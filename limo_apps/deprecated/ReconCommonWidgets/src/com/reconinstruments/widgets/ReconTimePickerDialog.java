package com.reconinstruments.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import android.widget.ToggleButton;

public class ReconTimePickerDialog extends AlertDialog {

    ReconDateTimeSetListener mListener;
    
    public ReconTimePickerDialog(Context context, ReconDateTimeSetListener listener) {
	super(context);
	mListener = listener;
	initDialog(context);
    }
    
    private void initDialog(Context context) {
	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	View l = inflater.inflate(R.layout.time_picker_dialog, null);
	
	final ReconPickerView hrPicker = (ReconPickerView) l.findViewById(R.id.hourPicker);
	final ReconPickerView minPicker = (ReconPickerView) l.findViewById(R.id.minutePicker);
	final ToggleButton amPm = (ToggleButton) l.findViewById(R.id.amPmToggle);
	hrPicker.setContent(1, 1, 12);
	minPicker.setContent(0,0, 59);
	
	this.setButton(BUTTON_POSITIVE, "Set", new OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		if (mListener != null) {
			int hrs = (int) Integer.parseInt(hrPicker.getSelectedValue());
			int mins = (int) Integer.parseInt(minPicker.getSelectedValue());
			boolean isPm = amPm.isChecked();
			mListener.onTimeSet(ReconTimePickerDialog.this, hrs, mins, isPm);
		}
	    } 
	});
	this.setButton(BUTTON_NEGATIVE, "Cancel", new OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
		ReconTimePickerDialog.this.dismiss();
	    } 
	});

	this.setView(l);
    }

}
