package com.reconinstruments.alarmclock;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

public class AlarmStartupService extends Service {
	public static final String PREFS_NAME = "AlarmPrefsFile";
	
	private ArrayList<ReconAlarm> alarmList = new ArrayList<ReconAlarm>();
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		readSavedData();
	}
	
	private void readSavedData(){
		SharedPreferences settings= getSharedPreferences(PREFS_NAME, 0);
		 
    	long tempLong=settings.getLong("alarm1_long", 0) ;
    	
    	
    	if(tempLong!=0){
    		alarmList.add(new ReconAlarm());
    		alarmList.get(0).setAlarmTime(tempLong);
    		if(settings.getBoolean("alarm1_24format", false))
    			alarmList.get(0).set24hrFormat();
    		if(settings.getBoolean("alarm1_enabled", false) && !alarmList.get(0).alarmTimeAlreadyPast())
    			alarmList.get(0).turnOnAlarm();
    		
    		
    		if(alarmList.get(0).alarmIsOn() && alarmList.get(0).getAlarmLong()>System.currentTimeMillis()){
                Intent myIntent = new Intent(AlarmStartupService.this, AlarmClockService.class);
                myIntent.putExtra("timeFormat24hr", alarmList.get(0).is24hrFormat());
            	PendingIntent pendingIntent = PendingIntent.getService(AlarmStartupService.this, 0, myIntent, 0);
            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            	alarmManager.set(AlarmManager.RTC_WAKEUP,alarmList.get(0).getAlarmLong(), pendingIntent);
        	}
    		
    	}
    	tempLong=settings.getLong("alarm2_long", 0) ;
    	
    	if(tempLong!=0){
    		alarmList.add(new ReconAlarm());
    		alarmList.get(1).setAlarmTime(tempLong);
    		if(settings.getBoolean("alarm2_24format", false))
    			alarmList.get(1).set24hrFormat();
    		if(settings.getBoolean("alarm2_enabled", false) && !alarmList.get(1).alarmTimeAlreadyPast())
    			alarmList.get(1).turnOnAlarm();
    		
    		if(alarmList.get(1).alarmIsOn() && alarmList.get(1).getAlarmLong()>System.currentTimeMillis()){
                Intent myIntent = new Intent(AlarmStartupService.this, AlarmClockService2.class);
                myIntent.putExtra("com.reconinstruments.ReconAlarm", alarmList.get(1));
            	PendingIntent pendingIntent = PendingIntent.getService(AlarmStartupService.this, 0, myIntent, 0);
            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            	alarmManager.set(AlarmManager.RTC_WAKEUP,alarmList.get(1).getAlarmLong(), pendingIntent);
        	}
    	
    	}
    	
    	tempLong=settings.getLong("alarm3_long", 0) ;
    	
    	if(tempLong!=0){
    		alarmList.add(new ReconAlarm());
    		alarmList.get(2).setAlarmTime(tempLong);
    		if(settings.getBoolean("alarm3_24format", false))
    			alarmList.get(2).set24hrFormat();
    		if(settings.getBoolean("alarm3_enabled", false)&& !alarmList.get(2).alarmTimeAlreadyPast())
    			alarmList.get(2).turnOnAlarm();
    		
    		if(alarmList.get(2).alarmIsOn()&& alarmList.get(2).getAlarmLong()>System.currentTimeMillis()){
                Intent myIntent = new Intent(AlarmStartupService.this, AlarmClockService3.class);
                myIntent.putExtra("com.reconinstruments.ReconAlarm", alarmList.get(2));
            	PendingIntent pendingIntent = PendingIntent.getService(AlarmStartupService.this, 0, myIntent, 0);
            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            	alarmManager.set(AlarmManager.RTC_WAKEUP,alarmList.get(2).getAlarmLong(), pendingIntent);
        	}
    	
    	}

	}
	

	

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}

