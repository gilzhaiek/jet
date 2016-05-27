package com.reconinstruments.alarmclock;

import java.util.Calendar;

import android.os.Parcel;
import android.os.Parcelable;


public class ReconAlarm implements Parcelable {

	private boolean alarmOn = false;
	private boolean timeFormat24hr = false;
	private Calendar mCal=Calendar.getInstance();
	
	public ReconAlarm(){
	}
	
	public void turnOnAlarm(){
			alarmOn = true;
		}
		
	public void turnOffAlarm(){
			if(alarmOn){
				alarmOn = false;
			}
	}
	
	public boolean alarmIsOn(){
		if(alarmOn)
			return true;
		else
			return false;
	}
		
	public void set24hrFormat(){
			timeFormat24hr = true;
	}
		
	public void set12hrFormat(){
			timeFormat24hr = false;
	}
	
	public boolean is24hrFormat(){
		if(timeFormat24hr)
			return true;
		else
			return false;
	}
	
	public void setAlarmTime(long milisec){
		mCal.setTimeInMillis(milisec);
//		if (System.currentTimeMillis()>mCal.getTimeInMillis())	//the alarm time already past, turn off alarm
//			turnOffAlarm();
		
	}
	
	public long getAlarmLong(){
		return mCal.getTimeInMillis();
	}
	


	public int getAlarmHour(int timeFormat){
		if(timeFormat==24)
			return mCal.get(Calendar.HOUR_OF_DAY);
		else{
			int hour = mCal.get(Calendar.HOUR);
			if (hour == 0)
				return 12;
			else
				return hour;
		}
	}
	public void add24hour(){
		mCal.add(Calendar.HOUR_OF_DAY,24);
	}
	public int getAlarmMinute(){
		return mCal.get(Calendar.MINUTE);
	}
	
	public boolean alarmTimeAlreadyPast(){
		if (System.currentTimeMillis()>mCal.getTimeInMillis())
			return true;
		else
			return false;
	}
	
	////////////////////////////////////////////////////////////////////////
	// Parcel stuff
	////////////////////////////////////////////////////////////////////////
	public ReconAlarm(Parcel in){
		readFromParcel(in);
	}
	
	public static final Parcelable.Creator<ReconAlarm> 	CREATOR = 
			new Parcelable.Creator<ReconAlarm>() {
		public ReconAlarm createFromParcel(Parcel in) {
			return new ReconAlarm(in);
		}
		public ReconAlarm[] newArray(int size) {
			return new ReconAlarm[size];
		}
	};
	
	public void writeToParcel(Parcel dest, int flags) {
		boolean boolArray[]= new boolean[2];
		boolArray[0]=alarmOn;
		boolArray[1]=timeFormat24hr;
		dest.writeBooleanArray(boolArray);

		long timeLong = mCal.getTimeInMillis();
		dest.writeLong(timeLong);

	}
	
	public void readFromParcel(Parcel src) {
		boolean boolArray[]= new boolean[2];
		src.readBooleanArray(boolArray);
		alarmOn=boolArray[0];
		timeFormat24hr = boolArray[1];
		
		long timeLong = src.readLong();
		setAlarmTime(timeLong);
		
	}
	
	public int describeContents() {
		return 0;
	}
	
}
