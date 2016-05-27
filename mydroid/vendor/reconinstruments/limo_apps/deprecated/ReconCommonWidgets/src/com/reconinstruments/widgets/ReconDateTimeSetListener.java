package com.reconinstruments.widgets;

public interface ReconDateTimeSetListener {
    abstract void onTimeSet(ReconTimePickerDialog view, int hourOfDay, int minute, boolean isPm);
    abstract void onDateSet(ReconDatePickerDialog view, String month, int dayOfMonth, int year);
}
