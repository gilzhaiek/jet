package com.reconinstruments.alarmclock;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AlarmClockActivity extends Activity {
	public static final String PREFS_NAME = "AlarmPrefsFile";

	private ReconAlarm reconAlarm;
	private ArrayList<ReconAlarm> alarmList = new ArrayList<ReconAlarm>();
	private static final int ALARM1_ACTIVITY = 1;
	private static final int ALARM2_ACTIVITY = 2;
	private static final int ALARM3_ACTIVITY = 3;
	private Button alarm1_button;
	private Button alarm2_button;
	private Button alarm3_button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarmclock);
        
//        TextView txt = (TextView) findViewById(R.id.text1);
//        Typeface font = Typeface.createFromAsset(getAssets(), "DroidSans_Bold.ttf");
//        txt.setTypeface(font);
        
         alarm1_button = (Button)findViewById(R.id.time1Selector);
    	 alarm2_button = (Button)findViewById(R.id.time2Selector);
    	 alarm3_button = (Button)findViewById(R.id.time3Selector);
        reconAlarm = new ReconAlarm();

        restoreSavedData();
        refreshScreen();
    }

    @Override
    protected void onPause (){
    	super.onPause();
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        if(alarmList.size()>=1){
	        editor.putLong("alarm1_long", alarmList.get(0).getAlarmLong());
	        editor.putBoolean("alarm1_24format", alarmList.get(0).is24hrFormat());
	        editor.putBoolean("alarm1_enabled", alarmList.get(0).alarmIsOn());
	
        }

        if(alarmList.size()>=2){
	        editor.putLong("alarm2_long", alarmList.get(1).getAlarmLong());
	        editor.putBoolean("alarm2_24format", alarmList.get(1).is24hrFormat());
	        editor.putBoolean("alarm2_enabled",  alarmList.get(1).alarmIsOn());
        }
        if(alarmList.size()>=3){
	        editor.putLong("alarm3_long", alarmList.get(2).getAlarmLong());
	        editor.putBoolean("alarm3_24format", alarmList.get(2).is24hrFormat());
	        editor.putBoolean("alarm3_enabled", alarmList.get(2).alarmIsOn());
        }
        editor.commit();
        
    }
    private void restoreSavedData(){
    	
    	SharedPreferences settings= getSharedPreferences(PREFS_NAME, 0);
 
    	long tempLong=settings.getLong("alarm1_long", 0) ;
    	
    	if(tempLong!=0){
    		alarmList.add(new ReconAlarm());
    		alarmList.get(0).setAlarmTime(tempLong);
    		if(settings.getBoolean("alarm1_24format", false))
    			alarmList.get(0).set24hrFormat();
    		if(settings.getBoolean("alarm1_enabled", false) && !alarmList.get(0).alarmTimeAlreadyPast())
    			alarmList.get(0).turnOnAlarm();
    	}
  
    	tempLong=settings.getLong("alarm2_long", 0) ;
    	
    	if(tempLong!=0){
    		alarmList.add(new ReconAlarm());
    		alarmList.get(1).setAlarmTime(tempLong);
    		if(settings.getBoolean("alarm2_24format", false))
    			alarmList.get(1).set24hrFormat();
    		if(settings.getBoolean("alarm2_enabled", false) && !alarmList.get(1).alarmTimeAlreadyPast())
    			alarmList.get(1).turnOnAlarm();
    	}
    	
    	tempLong=settings.getLong("alarm3_long", 0) ;
    	
    	if(tempLong!=0){
    		alarmList.add(new ReconAlarm());
    		alarmList.get(2).setAlarmTime(tempLong);
    		if(settings.getBoolean("alarm3_24format", false))
    			alarmList.get(2).set24hrFormat();
    		if(settings.getBoolean("alarm3_enabled", false)&& !alarmList.get(2).alarmTimeAlreadyPast())
    			alarmList.get(2).turnOnAlarm();
    	}
    }
    
    private void refreshScreen(){

    	//alarm 1
    	TextView alarm1_text = (TextView) findViewById(R.id.text1);
    	TextView onoff1_text = (TextView) findViewById(R.id.on_off1);
    	TextView ampm1_text = (TextView) findViewById(R.id.am_pm1);
    	
    	if(alarmList.size()>=1){
    		alarm1_text.setText(generateTimeString(alarmList.get(0)));
        	if (alarmList.get(0).alarmIsOn()){
        		onoff1_text.setText("on");
        		onoff1_text.setTextColor(Color.parseColor("#00aaff"));
        	}
        	else{
        		onoff1_text.setText("off");
        		onoff1_text.setTextColor(Color.parseColor("#737373"));
        	}
        	
        	if(alarmList.get(0).is24hrFormat()){
        		ampm1_text.setText("");
        	}else{
        		if(alarmList.get(0).getAlarmHour(24)<12)
        			ampm1_text.setText("AM");
        		else
        			ampm1_text.setText("PM");
        	}
    	}
    	
    	//alarm 2
    	TextView alarm2_text = (TextView) findViewById(R.id.text2);
    	TextView onoff2_text = (TextView) findViewById(R.id.on_off2);
    	TextView ampm2_text = (TextView) findViewById(R.id.am_pm2);
    	if(alarmList.size()>=2){
	    	alarm2_text.setText(generateTimeString(alarmList.get(1)));
	    	if (alarmList.get(1).alarmIsOn()){
	    		onoff2_text.setText("on");
	    		onoff2_text.setTextColor(Color.parseColor("#00aaff"));
	    	}
	    	else{
	    		onoff2_text.setText("off");
	    		onoff2_text.setTextColor(Color.parseColor("#737373"));
	    	}
	    	
	    	if(alarmList.get(1).is24hrFormat()){
	    		ampm2_text.setText("");
	    	}else{
	    		if(alarmList.get(1).getAlarmHour(24)<12)
	    			ampm2_text.setText("AM");
	    		else
	    			ampm2_text.setText("PM");
	    	}
    	}
    	//alarm 3
    	TextView alarm3_text = (TextView) findViewById(R.id.text3);
    	TextView onoff3_text = (TextView) findViewById(R.id.on_off3);
    	TextView ampm3_text = (TextView) findViewById(R.id.am_pm3);
    	
    	
    	if(alarmList.size()>=3){
	    	alarm3_text.setText(generateTimeString(alarmList.get(2)));
	    	if (alarmList.get(2).alarmIsOn()){
	    		onoff3_text.setText("on");
	    		onoff3_text.setTextColor(Color.parseColor("#00aaff"));
	    	}
	    	else{
	    		onoff3_text.setText("off");
	    		onoff3_text.setTextColor(Color.parseColor("#737373"));
	    	}
	    	
	    	if(alarmList.get(2).is24hrFormat()){
	    		ampm3_text.setText("");
	    	}else{
	    		if(alarmList.get(2).getAlarmHour(24)<12)
	    			ampm3_text.setText("AM");
	    		else
	    			ampm3_text.setText("PM");
	    	}
    	
    	}
    	
    	
    	alarm1_button.setEnabled(true);
		alarm1_button.setFocusable(true);
		alarm2_button.setEnabled(true);
		alarm2_button.setFocusable(true);
		alarm3_button.setEnabled(true);
		alarm3_button.setFocusable(true);
    	onoff1_text.setVisibility(View.VISIBLE);
		ampm1_text.setVisibility(View.VISIBLE);
		onoff2_text.setVisibility(View.VISIBLE);
		ampm2_text.setVisibility(View.VISIBLE);
		onoff3_text.setVisibility(View.VISIBLE);
		ampm3_text.setVisibility(View.VISIBLE);
		
    	if(alarmList.size()==0){	//when no alarm
    		alarm1_text.setText("+");
    		alarm2_text.setText("");
    		alarm3_text.setText("");
    		onoff1_text.setVisibility(View.INVISIBLE);
    		ampm1_text.setVisibility(View.INVISIBLE);
    		alarm2_button.setEnabled(false);
    		alarm2_button.setFocusable(false);
    		onoff2_text.setVisibility(View.INVISIBLE);
    		ampm2_text.setVisibility(View.INVISIBLE);
    		alarm3_button.setEnabled(false);
    		alarm3_button.setFocusable(false);
    		onoff3_text.setVisibility(View.INVISIBLE);
    		ampm3_text.setVisibility(View.INVISIBLE);
    	}else if(alarmList.size()==1){
    		alarm2_text.setText("+");
    		onoff2_text.setVisibility(View.INVISIBLE);
    		ampm2_text.setVisibility(View.INVISIBLE);
    		alarm3_text.setText("");
    		alarm3_button.setEnabled(false);
    		alarm3_button.setFocusable(false);
    		onoff3_text.setVisibility(View.INVISIBLE);
    		ampm3_text.setVisibility(View.INVISIBLE);
    	}else if(alarmList.size()==2){
    		alarm3_text.setText("+");
    		onoff3_text.setVisibility(View.INVISIBLE);
    		ampm3_text.setVisibility(View.INVISIBLE);
    	}else if(alarmList.size()==3){
    		
    	}
    	ImageView arrow1 = (ImageView)findViewById(R.id.arrow11);
    	if(alarm1_button.isFocused()&&alarmList.size()>=1){
    		 
    		 arrow1.setVisibility(View.VISIBLE);
    	}else{
    		arrow1.setVisibility(View.INVISIBLE);
    	}
    	ImageView arrow2 = (ImageView)findViewById(R.id.arrow12);
    	if(alarm2_button.isFocused()&&alarmList.size()>=2){
    		 
    		 arrow2.setVisibility(View.VISIBLE);
    	}else{
    		arrow2.setVisibility(View.INVISIBLE);
    	}
    	ImageView arrow3 = (ImageView)findViewById(R.id.arrow13);
    	if(alarm3_button.isFocused()&&alarmList.size()>=3){
    		 
    		 arrow3.setVisibility(View.VISIBLE);
    	}else{
    		arrow3.setVisibility(View.INVISIBLE);
    	}
    		
    	alarm1_button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
  	      public void onFocusChange(View v, boolean hasFocus) {

  	        Button alarm1_button = (Button)findViewById(R.id.time1Selector);
  	        ImageView arrow1 = (ImageView)findViewById(R.id.arrow11);
  	        if(hasFocus){
  	        	alarm1_button.setBackgroundResource(R.drawable.select_bar);
  	        	if(alarmList.size()>0){
  	        		
  	        		arrow1.setVisibility(View.VISIBLE);
  	        	}
  	        	
  	        }else{
  	        	alarm1_button.setBackgroundResource(R.drawable.invisible_bar);
  	        	arrow1.setVisibility(View.INVISIBLE);
  	        }
  	      }
  	    });
    	
    	alarm2_button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
    	      public void onFocusChange(View v, boolean hasFocus) {

    	        Button alarm2_button = (Button)findViewById(R.id.time2Selector);
    	        ImageView arrow2 = (ImageView)findViewById(R.id.arrow12);
    	        if(hasFocus){
    	        	alarm2_button.setBackgroundResource(R.drawable.select_bar);
    	        	if(alarmList.size()>1){
    	        		
    	        		arrow2.setVisibility(View.VISIBLE);
    	        	}
    	        	
    	        }else{
    	        	alarm2_button.setBackgroundResource(R.drawable.invisible_bar);
    	        	arrow2.setVisibility(View.INVISIBLE);
    	        }
    	      }
    	    });
    	
    	alarm3_button.setOnFocusChangeListener(new View.OnFocusChangeListener() {
    	      public void onFocusChange(View v, boolean hasFocus) {

    	        Button alarm3_button = (Button)findViewById(R.id.time3Selector);
    	        ImageView arrow3 = (ImageView)findViewById(R.id.arrow13);
    	        if(hasFocus){
    	        	alarm3_button.setBackgroundResource(R.drawable.select_bar);
    	        	if(alarmList.size()>2){
    	        		
    	        		arrow3.setVisibility(View.VISIBLE);
    	        	}
    	        	
    	        }else{
    	        	alarm3_button.setBackgroundResource(R.drawable.invisible_bar);
    	        	arrow3.setVisibility(View.INVISIBLE);
    	        }
    	      }
    	    });
    
    	
    }
    public static String generateTimeString(ReconAlarm alarm){
    	String hourStr,minuteStr;
    	
		if(alarm.is24hrFormat())
			hourStr = Integer.toString(alarm.getAlarmHour(24));
		else
			hourStr = Integer.toString(alarm.getAlarmHour(12));
		
		if (hourStr.length()==1)
			if(alarm.is24hrFormat())
				hourStr= "0"+hourStr;
			else
				hourStr= "  "+hourStr;
		
		minuteStr = Integer.toString(alarm.getAlarmMinute());
		if (minuteStr.length()==1)
			minuteStr= "0"+minuteStr;
	
    	return hourStr+":"+minuteStr;
    }

    public void onClick_alarm1(View view){
    	
    	Intent i = new Intent();
    	i.setClass( this , AlarmSettingActivity.class );
    	if(alarmList.size()<1)
    		i.putExtra("com.reconinstruments.ReconAlarm", new ReconAlarm());
    	else
    		i.putExtra("com.reconinstruments.ReconAlarm", alarmList.get(0));
    	
    	startActivityForResult(i,ALARM1_ACTIVITY);
    }
    
    public void onClick_alarm2(View view){
    	Intent i = new Intent();
    	i.setClass( this , AlarmSettingActivity.class );
    	if(alarmList.size()<2)
    		i.putExtra("com.reconinstruments.ReconAlarm", new ReconAlarm());
    	else
    		i.putExtra("com.reconinstruments.ReconAlarm", alarmList.get(1));
    	startActivityForResult(i,ALARM2_ACTIVITY);
    }
    public void onClick_alarm3(View view){
    	Intent i = new Intent();
    	i.setClass( this , AlarmSettingActivity.class );
    	if(alarmList.size()<3)
    		i.putExtra("com.reconinstruments.ReconAlarm", new ReconAlarm());
    	else
    		i.putExtra("com.reconinstruments.ReconAlarm", alarmList.get(2));
    	startActivityForResult(i,ALARM3_ACTIVITY);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	switch (requestCode) {
        case ALARM1_ACTIVITY:
            // This is the standard resultCode that is sent back if the
            // activity crashed or didn't doesn't supply an explicit result.
            if (resultCode == RESULT_CANCELED){
//            	if (alarmList.size()>=1)
//            		alarmList.remove(0);
//            	refreshScreen();
            } 
            else {
            	Bundle b = data.getExtras();
            	reconAlarm = b.getParcelable("com.reconinstruments.ReconAlarm");
            	
            	if(reconAlarm.getAlarmLong()==0)	//delete flag
            	{
            		if(alarmList.size()>=1)
            		alarmList.remove(0);
            		refreshScreen();
            		
            	}else{
            	if (alarmList.size()<1)
            		alarmList.add(reconAlarm);
            	else
            		alarmList.set(0, reconAlarm);
	            	refreshScreen();
	            	
	            	if(alarmList.get(0).alarmIsOn()){
		                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService.class);
		                myIntent.putExtra("timeFormat24hr", alarmList.get(0).is24hrFormat());
		            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
		            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		            	alarmManager.set(AlarmManager.RTC_WAKEUP,alarmList.get(0).getAlarmLong(), pendingIntent);
	            	}else{
		                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService.class);
		                myIntent.putExtra("timeFormat24hr", alarmList.get(0).is24hrFormat());
		            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
		            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		            	alarmManager.cancel(pendingIntent);
	            	}
            	}
            }
            break;
        case ALARM2_ACTIVITY:
            // This is the standard resultCode that is sent back if the
            // activity crashed or didn't doesn't supply an explicit result.
            if (resultCode == RESULT_CANCELED){
            	
            } 
            else {
            	Bundle b = data.getExtras();
        		reconAlarm = b.getParcelable("com.reconinstruments.ReconAlarm");
        		
        		
        		
        		if(reconAlarm.getAlarmLong()==0)	//delete flag
            	{
            		if(alarmList.size()>=2)
            		alarmList.remove(1);
            		refreshScreen();
            		
            	}else{
            	if (alarmList.size()<2)
            		alarmList.add(reconAlarm);
            	else
            		alarmList.set(1, reconAlarm);
            		refreshScreen();
	            	
	            	if(alarmList.get(1).alarmIsOn()){
		                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService2.class);
		                myIntent.putExtra("com.reconinstruments.ReconAlarm", alarmList.get(1));
		            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
		            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		            	alarmManager.set(AlarmManager.RTC_WAKEUP,alarmList.get(1).getAlarmLong(), pendingIntent);
	            	}else{
	            		Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService2.class);
	 	                myIntent.putExtra("com.reconinstruments.ReconAlarm", alarmList.get(1));
	 	            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
	 	            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
	 	            	alarmManager.cancel(pendingIntent);
	            	}
            	}
            }
            break;
        case ALARM3_ACTIVITY:
            // This is the standard resultCode that is sent back if the
            // activity crashed or didn't doesn't supply an explicit result.
            if (resultCode == RESULT_CANCELED){
            	
            } 
            else {
            	Bundle b = data.getExtras();
        		reconAlarm = b.getParcelable("com.reconinstruments.ReconAlarm");
        		
        		if(reconAlarm.getAlarmLong()==0)	//delete flag
            	{
            		if(alarmList.size()>=3)
            		alarmList.remove(2);
            		refreshScreen();
            		
            	}else{
	        		if (alarmList.size()<3)
	            		alarmList.add(reconAlarm);
	            	else
	            		alarmList.set(2, reconAlarm);
	            	refreshScreen();
	            	
	            	if(alarmList.get(2).alarmIsOn()){
		                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService3.class);
		                myIntent.putExtra("com.reconinstruments.ReconAlarm", alarmList.get(2));
		            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
		            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		            	alarmManager.set(AlarmManager.RTC_WAKEUP,alarmList.get(2).getAlarmLong(), pendingIntent);
	            	}else{
	            		Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService3.class);
	 	                myIntent.putExtra("com.reconinstruments.ReconAlarm", alarmList.get(1));
	 	            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
	 	            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
	 	            	alarmManager.cancel(pendingIntent);
	            	}
            	}
            }
            break;
        default:
            break;
    	}
    }
    public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			if(alarm1_button.isFocused() && alarmList.size()>=1){
				if(alarmList.get(0).alarmIsOn())
					alarmList.get(0).turnOffAlarm();
				else{
					alarmList.get(0).turnOnAlarm();
					if (System.currentTimeMillis()>=alarmList.get(0).getAlarmLong())
						alarmList.get(0).add24hour();
				}
            	if(alarmList.get(0).alarmIsOn()){
	                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService.class);
	                myIntent.putExtra("timeFormat24hr", alarmList.get(0).is24hrFormat());
	            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
	            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
	            	alarmManager.set(AlarmManager.RTC_WAKEUP,alarmList.get(0).getAlarmLong(), pendingIntent);
            	}else{
	                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService.class);
	                myIntent.putExtra("timeFormat24hr", alarmList.get(0).is24hrFormat());
	            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
	            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
	            	alarmManager.cancel(pendingIntent);
            	}
			}else if(alarm2_button.isFocused() && alarmList.size()>=2){
				if(alarmList.get(1).alarmIsOn())
					alarmList.get(1).turnOffAlarm();
				else{
					alarmList.get(1).turnOnAlarm();
					if (System.currentTimeMillis()>=alarmList.get(1).getAlarmLong())
						alarmList.get(1).add24hour();
				}
            	if(alarmList.get(1).alarmIsOn()){
	                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService2.class);
	                myIntent.putExtra("timeFormat24hr", alarmList.get(1).is24hrFormat());
	            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
	            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
	            	alarmManager.set(AlarmManager.RTC_WAKEUP,alarmList.get(1).getAlarmLong(), pendingIntent);
            	}else{
	                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService2.class);
	                myIntent.putExtra("timeFormat24hr", alarmList.get(1).is24hrFormat());
	            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
	            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
	            	alarmManager.cancel(pendingIntent);
            	}
			}else if(alarm3_button.isFocused() && alarmList.size()>=3){
				if(alarmList.get(2).alarmIsOn())
					alarmList.get(2).turnOffAlarm();
				else{
					alarmList.get(2).turnOnAlarm();
					if (System.currentTimeMillis()>=alarmList.get(2).getAlarmLong())
						alarmList.get(2).add24hour();
				}
            	if(alarmList.get(2).alarmIsOn()){
	                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService3.class);
	                myIntent.putExtra("timeFormat24hr", alarmList.get(2).is24hrFormat());
	            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
	            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
	            	alarmManager.set(AlarmManager.RTC_WAKEUP,alarmList.get(2).getAlarmLong(), pendingIntent);
            	}else{
	                Intent myIntent = new Intent(AlarmClockActivity.this, AlarmClockService3.class);
	                myIntent.putExtra("timeFormat24hr", alarmList.get(2).is24hrFormat());
	            	PendingIntent pendingIntent = PendingIntent.getService(AlarmClockActivity.this, 0, myIntent, 0);
	            	AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
	            	alarmManager.cancel(pendingIntent);
            	}
			}
			
				
			refreshScreen();
		}

		return super.onKeyDown(keyCode, event);
	}
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
		}
    	return super.onKeyDown(keyCode, event);
	
    }
}
