package com.reconinstruments.alarmclock;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AlarmSettingActivity extends Activity {
	private ReconAlarm reconAlarm;
	
	private Button onoff_button;
	private Button hour_button;
	private Button minute_button;
	private Button ampm_button;
	private Button delete_button;
	private TextView onoff_text;
	private TextView hour_text;
	private TextView minute_text;
	private TextView ampm_text;
	private boolean settings_changed = false;
	private int hour;
	private int minute;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);

		setContentView(R.layout.alarm_setting);

		Bundle b = getIntent().getExtras();
		reconAlarm = b.getParcelable("com.reconinstruments.ReconAlarm");
		hour = reconAlarm.getAlarmHour(24);
		minute = reconAlarm.getAlarmMinute();
		
		initializeScreen();
	}

	private void initializeScreen() {
		onoff_text = (TextView) findViewById(R.id.on_off1);
		hour_text = (TextView) findViewById(R.id.hour);
		minute_text = (TextView) findViewById(R.id.minute);
		ampm_text = (TextView) findViewById(R.id.ampm);
		onoff_button = (Button) findViewById(R.id.onOffselector);
		hour_button = (Button) findViewById(R.id.hourSelector);
		minute_button = (Button) findViewById(R.id.minuteSelector);
		ampm_button = (Button) findViewById(R.id.ampmSelector);
		delete_button = (Button) findViewById(R.id.deleteSelector);

		if (reconAlarm.alarmIsOn()) {
			onoff_text.setText("on");
			onoff_text.setTextColor(Color.parseColor("#00aaff"));
		} else {
			onoff_text.setText("off");
			onoff_text.setTextColor(Color.parseColor("#737373"));
		}

		updateHourText();
		updateMinuteText();
		updateAMPMText();

	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			if(!delete_button.isFocused())
				settings_changed= true;
			if (onoff_button.isFocused()) {
				toggleOnOff();
			} else if (hour_button.isFocused()) {
				decrementHour();
			} else if (minute_button.isFocused()) {
				decrementMinute();
			} else if (ampm_button.isFocused()) {
				toggleAMPM_down();
			} else if (delete_button.isFocused()) {

			}

		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			if(!delete_button.isFocused())
				settings_changed= true;
			if (onoff_button.isFocused()) {
				toggleOnOff();
			} else if (hour_button.isFocused()) {
				
				incrementHour();
			} else if (minute_button.isFocused()) {
				incrementMinute();
			} else if (ampm_button.isFocused()) {
				settings_changed= true;
				toggleAMPM_up();
			} else if (delete_button.isFocused()) {

			}

		}
	

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(settings_changed== true)
				onClick_accept(ampm_text);
			else
				finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	private void toggleOnOff() {
		if (reconAlarm.alarmIsOn()) {
			reconAlarm.turnOffAlarm();
			onoff_text.setText("off");
			onoff_text.setTextColor(Color.parseColor("#737373"));
		} else {
			reconAlarm.turnOnAlarm();
			onoff_text.setText("on");
			onoff_text.setTextColor(Color.parseColor("#00aaff"));
		}

	}

	private void incrementHour() {
		hour++;
		if (hour>23)
			hour=0;
		
		updateHourText();
	}
	
	private void decrementHour(){
		hour--;
		if (hour<0)
			hour=23;

		updateHourText();
	}
	
	private void incrementMinute(){
		minute++;
		if (minute>59)
			minute=0;
		updateMinuteText();
	}
	
	private void decrementMinute(){
		minute--;
		if(minute<0)
			minute=59;
		updateMinuteText();
	}
	
	private void updateHourText(){
		String hourStr;
		if(reconAlarm.is24hrFormat()){
			hourStr = Integer.toString(hour);
		}
		else{
			if(hour==0)
				hourStr = Integer.toString(12);
			else if (hour>12)
				hourStr = Integer.toString(hour-12);
			else
				hourStr = Integer.toString(hour);
		}
		if (hourStr.length()==1)
			if(reconAlarm.is24hrFormat())
				hourStr= "0"+hourStr;
			else
				hourStr= "  "+hourStr;
		
		hour_text.setText(hourStr);
	}
	
	private void updateMinuteText(){
		String minuteStr = Integer.toString(minute);
		if (minuteStr.length()==1)
			minuteStr= "0"+minuteStr;
		minute_text.setText(minuteStr);
	}
	
	private void toggleAMPM_up(){
		
		//if it's 24hr format, return. Since 24hr format is at the top of the list
		if (reconAlarm.is24hrFormat()){
			return;
		}
		else if(hour<12)
			reconAlarm.set24hrFormat();
		else
			hour-=12;
				
		
		updateAMPMText();
		updateHourText();
	}
	private void toggleAMPM_down(){
		
		//if it's 24 hour format, change to 12 hour format
		if (reconAlarm.is24hrFormat()){
			reconAlarm.set12hrFormat();
			if(hour>11) //if it's afternoon, force it to morning
				hour-=12;
			
		}else{
			//if it's am, +12 to change to pm
			if (hour <12){
				hour+=12;
			}else
				return; //it's at the bottom of the list, return
		}
		updateAMPMText();
		updateHourText();
	}
	
	private void updateAMPMText(){
		if (reconAlarm.is24hrFormat()){
			ampm_text.setText("24hr");
			ampm_text.setTextSize(22);
		}
		else if(hour<12){
			ampm_text.setText("AM");
			ampm_text.setTextSize(28);
		}
		else{
			ampm_text.setText("PM");
			ampm_text.setTextSize(28);
		}
	}
	
	public void onClick_accept(View view){
		Calendar cal = Calendar.getInstance();

		if(cal.get(Calendar.HOUR_OF_DAY)>hour)
			cal.add(Calendar.HOUR_OF_DAY, 24);//add 24 hours ie:next day
		else if(cal.get(Calendar.HOUR_OF_DAY)==hour)
			if(cal.get(Calendar.MINUTE)>=minute){ 
				cal.add(Calendar.HOUR_OF_DAY, 24);//add 24 hours ie:next day
			}
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);

		reconAlarm.setAlarmTime(cal.getTimeInMillis());
	
		Intent i = new Intent();
		i.putExtra("com.reconinstruments.ReconAlarm", reconAlarm);
		setResult(RESULT_OK,i);
		finish();
    }
	
	public void onClick_delete(View view){
		Intent i = new Intent();
		reconAlarm.setAlarmTime(0);
		i.putExtra("com.reconinstruments.ReconAlarm", reconAlarm);
		setResult(RESULT_OK,i);
		finish();
    }

}
