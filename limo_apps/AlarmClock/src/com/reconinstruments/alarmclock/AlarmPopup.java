package com.reconinstruments.alarmclock;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TextView;

public class AlarmPopup extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.alarm_popup);
		// Bundle b = this.getIntent().getExtras();
		// float[] alarmInfo = b.getFloatArray("alarm_info");
		// Toast.makeText(this, "Inside My Alert activity() "+ alarmInfo[1],
		// Toast.LENGTH_LONG).show();

		Bundle b = getIntent().getExtras();
//		ReconAlarm reconAlarm = b.getParcelable("com.reconinstruments.ReconAlarm");
		boolean timeFormat24hr = b.getBoolean("timeFormat24hr");
		ReconAlarm reconAlarm = new ReconAlarm();
		
		
		if(timeFormat24hr)
			reconAlarm.set24hrFormat();
		
		String timestr = AlarmClockActivity.generateTimeString(reconAlarm);
		if (!reconAlarm.is24hrFormat())
			if (reconAlarm.getAlarmHour(24) < 12)
				timestr = timestr + " am";
			else
				timestr = timestr + " pm";
		TextView alarmTime = (TextView) findViewById(R.id.alarmTime);
		alarmTime.setText(timestr);

		Typeface font = Typeface.createFromAsset(getAssets(),
				"DroidSans_Bold.ttf");
		alarmTime.setTypeface(font);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
			finish();
		}
		
		return super.onKeyDown(keyCode, event);
	}
}