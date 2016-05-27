package com.reconinstruments.dashlauncher.settings;

import java.text.DecimalFormat;
import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.reconinstruments.dashlauncher.R;

public class ManualTimeActivity extends Activity implements ViewSwitcher.ViewFactory{

	private TextSwitcher hourSwitcher;
	private TextSwitcher minSwitcher;
	private TextSwitcher meriSwitcher;

	private LinearLayout hourLayout;
	private LinearLayout minLayout;
	private LinearLayout meriLayout;

	private boolean mIs24;

	private int hr;
	private int baseHr;
	private int baseMnt;

	private int focus;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		setContentView(R.layout.setting_time_layout);
		
		TextView title = (TextView) findViewById(R.id.setting_title);
		title.setText("Manually Set Time");

		hourSwitcher = (TextSwitcher) findViewById(R.id.hour_switcher);
		minSwitcher = (TextSwitcher) findViewById(R.id.min_switcher);
		meriSwitcher = (TextSwitcher) findViewById(R.id.meri_switcher);

		ImageView iv;
		
		hourLayout = (LinearLayout) findViewById(R.id.setting_time_hour);
		iv = (ImageView) hourLayout.findViewById(R.id.hour_up);
		iv.setAlpha(100);
		iv = (ImageView) hourLayout.findViewById(R.id.hour_down);
		iv.setAlpha(100);
		
		minLayout = (LinearLayout) findViewById(R.id.setting_time_min);
		iv = (ImageView) minLayout.findViewById(R.id.min_up);
		iv.setAlpha(100);
		iv = (ImageView) minLayout.findViewById(R.id.min_down);
		iv.setAlpha(100);
		
		meriLayout = (LinearLayout) findViewById(R.id.setting_time_meridiem);
		iv = (ImageView) meriLayout.findViewById(R.id.meri_up);
		iv.setAlpha(100);
		iv = (ImageView) meriLayout.findViewById(R.id.meri_down);
		iv.setAlpha(100);

		focus = 0;

		hourLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				LinearLayout ll = (LinearLayout) v;

				ImageView up = (ImageView) ll.findViewById(R.id.hour_up);
				ImageView down = (ImageView) ll.findViewById(R.id.hour_down);
				TextView tv = (TextView) hourSwitcher.getCurrentView();

				if (hasFocus) {
					focus = 0;
					up.setAlpha(255);
					down.setAlpha(255);
				}
				else {
					up.setAlpha(100);
					down.setAlpha(100);
				}
			}
		});

		minLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				LinearLayout ll = (LinearLayout) v;

				ImageView up = (ImageView) ll.findViewById(R.id.min_up);
				ImageView down = (ImageView) ll.findViewById(R.id.min_down);
				TextView tv = (TextView) minSwitcher.getCurrentView();

				if (hasFocus) {
					focus = 1;
					up.setAlpha(255);
					down.setAlpha(255);
				}
				else {
					up.setAlpha(100);
					down.setAlpha(100);
				}
			}
		});

		meriLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				LinearLayout ll = (LinearLayout) v;

				ImageView up = (ImageView) ll.findViewById(R.id.meri_up);
				ImageView down = (ImageView) ll.findViewById(R.id.meri_down);


				if (hasFocus) {
					focus = 2;
					up.setAlpha(255);
					down.setAlpha(255);
				}
				else {
					up.setAlpha(100);
					down.setAlpha(100);
				}
			}
		});


		mIs24 = DateFormat.is24HourFormat(this);
		if (mIs24){
			meriLayout.setVisibility(View.GONE);
		}

		Animation in = AnimationUtils.loadAnimation(this,
				android.R.anim.fade_in);
		Animation out = AnimationUtils.loadAnimation(this,
				android.R.anim.fade_out);

		hourSwitcher.setFactory(this);
		hourSwitcher.setInAnimation(in);
		hourSwitcher.setOutAnimation(out);

		minSwitcher.setFactory(this);
		minSwitcher.setInAnimation(in);
		minSwitcher.setOutAnimation(out);

		meriSwitcher.setFactory(this);
		meriSwitcher.setInAnimation(in);
		meriSwitcher.setOutAnimation(out);

		initTime(this);
		update();

		hourLayout.requestFocus();

	}

	private void update() {
		DecimalFormat df = new DecimalFormat();
		df.setMinimumIntegerDigits(2);
		if (mIs24)
			hourSwitcher.setText(String.valueOf(hr));
		else{
			hourSwitcher.setText(String.valueOf(baseHr));
			if (hr >= 12)
				meriSwitcher.setText("pm");
			else
				meriSwitcher.setText("am");
		}
		minSwitcher.setText(df.format(baseMnt));

	}


	private void initTime(Context context) 
	{
		hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		baseHr = Calendar.getInstance().get(Calendar.HOUR);
		baseMnt = Calendar.getInstance().get(Calendar.MINUTE);
	}



	private void setTime(int hourOfDay, int minute) {
		Calendar c = Calendar.getInstance();

		c.set(Calendar.HOUR_OF_DAY, hourOfDay);
		c.set(Calendar.MINUTE, minute);
		long when = c.getTimeInMillis();

		boolean set = false;
		if (when / 1000 < Integer.MAX_VALUE) {
			set = SystemClock.setCurrentTimeMillis(when);
		}              

		Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
		sendBroadcast(timeChanged);

		// We don't need to call timeUpdated() here because the TIME_CHANGED
		// broadcast is sent by the AlarmManager as a side effect of setting the
		// SystemClock time.
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.v("focus", ""+focus);
		switch (keyCode){
		case KeyEvent.KEYCODE_DPAD_UP:
			switch (focus){
			case 0:
				if (hr < 23){
					hr++;
					if (hr > 12)
						baseHr = hr - 12;
					else
						baseHr = hr;
				}
				else{
					hr -= 23;
					baseHr = 12;
				}
				update();
				break;
			case 1:
				if (baseMnt < 59)
					baseMnt++;
				else
					baseMnt -= 59;
				update();
				break;
			case 2:
				if (((TextView)meriSwitcher.getCurrentView()).getText().toString().equalsIgnoreCase("pm")){
					meriSwitcher.setText("am");
					hr -= 12;
				}
				else{
					meriSwitcher.setText("pm");
					hr += 12;
				}
				break;
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			Log.v("focus", ""+focus);
			switch (focus){
			case 0:
				if (hr > 0){
					hr--;
					if (hr > 12)
						baseHr = hr - 12;
					else
						baseHr = hr;
				}
				else{
					hr = 23;
					baseHr = 11;
				}
				update();
				break;
			case 1:
				if (baseMnt > 0)
					baseMnt--;
				else
					baseMnt = 59;
				update();
				break;
			case 2:
				if (((TextView)meriSwitcher.getCurrentView()).getText().toString().equalsIgnoreCase("pm")){
					meriSwitcher.setText("am");
					hr -= 12;
				}
				else{
					meriSwitcher.setText("pm");
					hr += 12;
				}
				break;
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			setTime(hr,baseMnt);
			finish();
			return true;

		}

		return super.onKeyDown(keyCode, event);
	}


	public View makeView() {

		TextView t = new TextView(this);

		t.setShadowLayer(1.5f, 2.0f, 2.0f, Color.GRAY);
		t.setGravity(Gravity.RIGHT);
		t.setTextColor(Color.WHITE);
		t.setTextSize(36);
		t.setTypeface(Typeface.SANS_SERIF);

		return t;
	}

}
