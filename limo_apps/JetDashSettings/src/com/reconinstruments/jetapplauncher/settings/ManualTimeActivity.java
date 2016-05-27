package com.reconinstruments.jetapplauncher.settings;

import java.text.DecimalFormat;
import java.util.Calendar;

import com.reconinstruments.jetapplauncher.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnFocusChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.widget.ViewSwitcher.ViewFactory;
import com.reconinstruments.utils.SettingsUtil;


public class ManualTimeActivity extends Activity{

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
		
      	Window mainWindow = getWindow();
	    int dimAmt = 60;
	    mainWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
	    WindowManager.LayoutParams params = mainWindow.getAttributes();
	    params.dimAmount = dimAmt / 100f;
	    mainWindow.setAttributes(params);

	    TextView title = (TextView) findViewById(R.id.pop_up_title);
		title.setText("MANUALLY SET TIME");

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
				if (hasFocus) {
					focus = 0;
					up.setAlpha(255);
					down.setAlpha(255);
					up.setVisibility(View.VISIBLE);
					down.setVisibility(View.VISIBLE);
					((TextView)hourSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_over));
				}
				else {
					((TextView)hourSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
					up.setAlpha(100);
					down.setAlpha(100);
					up.setVisibility(View.GONE);
					down.setVisibility(View.GONE);
				}
			}
		});

		minLayout.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				LinearLayout ll = (LinearLayout) v;

				ImageView up = (ImageView) ll.findViewById(R.id.min_up);
				ImageView down = (ImageView) ll.findViewById(R.id.min_down);
				if (hasFocus) {
					focus = 1;
					((TextView)minSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_over));
					up.setAlpha(255);
					down.setAlpha(255);
					up.setVisibility(View.VISIBLE);
					down.setVisibility(View.VISIBLE);
				}
				else {
					((TextView)minSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
					up.setAlpha(100);
					down.setAlpha(100);
					up.setVisibility(View.GONE);
					down.setVisibility(View.GONE);
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
					((TextView)meriSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_over));
					up.setVisibility(View.VISIBLE);
					down.setVisibility(View.VISIBLE);
					up.setAlpha(255);
					down.setAlpha(255);
				}
				else {
					((TextView)meriSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_up));
					up.setAlpha(100);
					down.setAlpha(100);
					up.setVisibility(View.GONE);
					down.setVisibility(View.GONE);
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

//		hourSwitcher.setFactory(this);
		hourSwitcher.setInAnimation(in);
		hourSwitcher.setOutAnimation(out);

//		minSwitcher.setFactory(this);
		minSwitcher.setInAnimation(in);
		minSwitcher.setOutAnimation(out);

//		meriSwitcher.setFactory(this);
		meriSwitcher.setInAnimation(in);
		meriSwitcher.setOutAnimation(out);

		initTime(this);
		update();

		hourLayout.requestFocus();
		((TextView)hourSwitcher.getChildAt(0)).setTextColor(getResources().getColor(R.color.recon_switcher_over));
	}

	private void update() {
		DecimalFormat df = new DecimalFormat();
		df.setMinimumIntegerDigits(2);
		if (mIs24){
			if(hr < 10){
				((TextView)hourSwitcher.getChildAt(0)).setText("0" + String.valueOf(hr));
			}else{
				((TextView)hourSwitcher.getChildAt(0)).setText(String.valueOf(hr));
			}
			
//			hourSwitcher.setText(String.valueOf(hr));
		}
		else{
			if(baseHr < 10){
				((TextView)hourSwitcher.getChildAt(0)).setText("0" + String.valueOf(baseHr));
			}else{
				((TextView)hourSwitcher.getChildAt(0)).setText(String.valueOf(baseHr));
			}
			
//			hourSwitcher.setText(String.valueOf(baseHr));
			if (hr >= 12){
				((TextView)meriSwitcher.getChildAt(0)).setText("PM");
//				meriSwitcher.setText("PM");
			}
			else{
				((TextView)meriSwitcher.getChildAt(0)).setText("AM");
//				meriSwitcher.setText("AM");
			}
		}
		if(baseMnt < 10){
			((TextView)minSwitcher.getChildAt(0)).setText(String.valueOf("0" + baseMnt));
		}else{
			((TextView)minSwitcher.getChildAt(0)).setText(String.valueOf(baseMnt));
		}
		
//		minSwitcher.setText(df.format(baseMnt));

	}


	private void initTime(Context context) 
	{
		hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		baseHr = Calendar.getInstance().get(Calendar.HOUR);
		if(baseHr == 0){
			baseHr = 12;
		}
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

        SettingsUtil.setTimeAuto(getBaseContext(), false); // turn off auto GPS
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
				if (((TextView)meriSwitcher.getChildAt(0)).getText().toString().equalsIgnoreCase("pm")){
					//meriSwitcher.setText("AM");
					((TextView)meriSwitcher.getChildAt(0)).setText("AM");
					hr -= 12;
				}
				else{
					((TextView)meriSwitcher.getChildAt(0)).setText("PM");
					//meriSwitcher.setText("PM");
					hr += 12;
				}
				update();
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
					else if(hr==0)
						baseHr = 12;
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
				if (((TextView)meriSwitcher.getChildAt(0)).getText().toString().equalsIgnoreCase("pm")){
					//meriSwitcher.setText("AM");
					((TextView)meriSwitcher.getChildAt(0)).setText("AM");
					hr -= 12;
				}
				else{
					//meriSwitcher.setText("PM");
					((TextView)meriSwitcher.getChildAt(0)).setText("PM");
					hr += 12;
				}
				update();
				break;
			}
			return true;
		case KeyEvent.KEYCODE_ENTER:
		case KeyEvent.KEYCODE_DPAD_CENTER:
			setTime(hr,baseMnt);
			finish();
			return true;

		}

		return super.onKeyDown(keyCode, event);
	}


//	public View makeView() {
//
//		TextView t = new TextView(this);
//
//      t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 50);
//      t.setTypeface(Typeface.SANS_SERIF);
//      t.setTextColor(getResources().getColorStateList(R.drawable.switcher_color));
//		
////		t.setShadowLayer(1.5f, 2.0f, 2.0f, Color.GRAY);
////		t.setGravity(Gravity.RIGHT);
////		t.setTextColor(Color.WHITE);
////		t.setTextSize(36);
////		t.setTypeface(Typeface.SANS_SERIF);
////		t.setTextColor(getResources().getColorStateList(R.drawable.switcher_color));
//		return t;
//	}

}
