package com.reconinstruments.currentvoltage;

import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.reconinstruments.currentvoltage.CurrentVoltage.GraphType;

public class TopOverlayService extends Service {
	private static final String TAG = TopOverlayService.class.getSimpleName();
	
	private static final int REFRESH_DELAY_MS = 200;
	
	private final Handler mHandler = new Handler();
	private Runnable mTimerUpdateValue;
	
	private GraphViewSeries mVoltageSeries;
	private GraphViewSeries mCurrentSeries;
	
	private float averageVoltage = 0;
	private float averageCurrent = 0;
	
	private int averageCounter = 0;
	
	ArrayList<GraphViewData> currentStack = new ArrayList<GraphViewData>();
	ArrayList<GraphViewData> voltageStack = new ArrayList<GraphViewData>();
	
	private static long lastXValue = 0;
	
	private WindowManager windowManager;
	private LineGraphView mVoltageView;
	private TextView mVoltageTextView;
	private LineGraphView mCurrentView;
	private TextView mCurrentTextView;
	
	private FrameLayout currentFrame;
	private FrameLayout voltageFrame;
	
	@Override
	public void onCreate() {
		super.onCreate();

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		mCurrentSeries = new GraphViewSeries(new GraphViewData[] {});
		mVoltageSeries = new GraphViewSeries(new GraphViewData[] {});
		
		mCurrentView = Util.getGraphView(this, mCurrentSeries, "CurrentGraph", GraphType.CURRENT);
		
		currentFrame = new FrameLayout(this);
		currentFrame.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
		currentFrame.setBackgroundColor(0x80000000);
		currentFrame.addView(mCurrentView);
		
		mVoltageView = Util.getGraphView(this, mVoltageSeries, "VoltageGraph", GraphType.VOLTAGE);
		
		voltageFrame = new FrameLayout(this);
		voltageFrame.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
		voltageFrame.setBackgroundColor(0x80000000);
		voltageFrame.addView(mVoltageView);
		
		if (CurrentVoltage.averageMode) {
			mCurrentTextView = new TextView(this);
			mCurrentTextView.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
			mCurrentTextView.setTextColor(0xFFfe9600);
			mCurrentTextView.setTextSize(40);
			currentFrame.addView(mCurrentTextView);
			mCurrentTextView.setText("C:");
			mVoltageTextView = new TextView(this);
			mVoltageTextView.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT));
			mVoltageTextView.setTextColor(0xFFfe9600);
			mVoltageTextView.setTextSize(40);
			voltageFrame.addView(mVoltageTextView);
			mVoltageTextView.setText("V:");
			
		}
		
		WindowManager.LayoutParams paramsVolt = new WindowManager.LayoutParams(	228,100,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		WindowManager.LayoutParams paramsCur = new WindowManager.LayoutParams(	228,100,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
		paramsVolt.x = 200;
		paramsVolt.y = -30;
		paramsCur.x = 200;
		paramsCur.y = 120;

//		windowManager.addView(ll, params);
		
		windowManager.addView(currentFrame, paramsVolt);
		windowManager.addView(voltageFrame, paramsCur);
	}

	@Override
	  public void onDestroy() {
	    super.onDestroy();
	    if (currentFrame != null)
	    	windowManager.removeView(currentFrame);
	    if (voltageFrame != null)
	    	windowManager.removeView(voltageFrame);
	    
	    mHandler.removeCallbacks(mTimerUpdateValue);
	  }

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		mTimerUpdateValue = new Runnable() {
			@Override
			public void run() {
				if (CurrentVoltage.averageMode) {
					mVoltageTextView.setText("V:"+Util.getVoltage());
					mCurrentTextView.setText("C:"+(-1 * Util.getCurrentAvg()));
					mHandler.postDelayed(this, CurrentVoltage.refreshRate * 1000);
				} else {
					if (voltageStack.size() >= (CurrentVoltage.refreshRate * (1000/REFRESH_DELAY_MS)) 
							|| averageCounter >= (CurrentVoltage.refreshRate * (1000/REFRESH_DELAY_MS)) ){
						for (int i = 0 ; i < voltageStack.size() ; i++) {
							mVoltageSeries.appendData(voltageStack.get(i), true, 100);
							mCurrentSeries.appendData(currentStack.get(i), true, 100);
						}
						
						voltageStack.clear();
						currentStack.clear();
					}
					
					voltageStack.add(new GraphViewData(lastXValue, Util.getVoltage()));
					currentStack.add(new GraphViewData(lastXValue, -1 * Util.getCurrentNow()));
								
					lastXValue++;
					mHandler.postDelayed(this, REFRESH_DELAY_MS);
				}
			}
		};
		
		mHandler.postDelayed(mTimerUpdateValue, REFRESH_DELAY_MS);
		
		return START_STICKY;
	}

}
