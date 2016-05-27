package com.reconinstruments.alarmclock;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class AlarmClockService2 extends Service {
	
	
	@Override
	public void onCreate() {
//		Toast.makeText(this, "MyAlarmService.onCreate()", Toast.LENGTH_LONG).show();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
//		Toast.makeText(this, "MyAlarmService.onBind()", Toast.LENGTH_LONG).show();
		return null;
	}
	@Override

	public void onDestroy() {
		super.onDestroy();
//		Toast.makeText(this, "MyAlarmService.onDestroy()", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onStart(Intent intent, int startId) {

		super.onStart(intent, startId);
		
		Bundle b =intent.getExtras();
		Boolean timeFormat24hr = b.getBoolean("timeFormat24hr");
		
		Intent alertIntent = new Intent();
	      alertIntent.setClass( this , AlarmPopup.class );
	      alertIntent.putExtra("timeFormat24hr", timeFormat24hr);
	      alertIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	      startActivity( alertIntent );
	     
	}
}