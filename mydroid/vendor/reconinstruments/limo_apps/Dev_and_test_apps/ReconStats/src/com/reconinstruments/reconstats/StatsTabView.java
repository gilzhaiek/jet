package com.reconinstruments.reconstats;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

import com.reconinstruments.reconstats.R;
import com.reconinstruments.reconstats.tabs.*;

public class StatsTabView extends TabView{
	
	// debugging
	private static final String TAG = "StatsTabView";
	private static final boolean D = false;
	
	ArrayList<TabPage> tabPages = new ArrayList<TabPage>(3);

	TabPage mRunHistoryTP, mSpeedTP, mAltitudeTP, mVerticalTP, mDistanceTP, mTemperatureTP, mPreferencesTP; 
	Context mContext;
	
	public StatsTabView(Context context) {
		super(context);
		mContext = context;
		createTabPages();
	}
	
	private void createTabPages() {
		Resources r = mContext.getResources();
		
		// Run History
		Drawable lastRunIconReg = r.getDrawable(R.drawable.stats_tabicon_lastrun_grey);
		Drawable lastRunIconSelected = r.getDrawable(R.drawable.stats_tabicon_lastrun_white);
		mRunHistoryTP = new LastRunTabPage(mContext, lastRunIconReg, lastRunIconSelected, lastRunIconSelected, this);
		tabPages.add(mRunHistoryTP);
		
		// Speed
		Drawable speedIconReg = r.getDrawable(R.drawable.stats_tabicon_speed_grey);
		Drawable speedIconSelected = r.getDrawable(R.drawable.stats_tabicon_speed_white);
		mSpeedTP = new SpeedTabPage(mContext, speedIconReg, speedIconSelected, speedIconSelected, this);
		tabPages.add(mSpeedTP);
		
		// Altitude
		Drawable altIconReg = r.getDrawable(R.drawable.stats_tabicon_alt_grey);
		Drawable altIconSelected = r.getDrawable(R.drawable.stats_tabicon_alt_white);
		mAltitudeTP = new AltitudeTabPage(mContext, altIconReg, altIconSelected, altIconSelected, this);
		tabPages.add(mAltitudeTP);
		
		// Vertical
		Drawable verticalIconReg = r.getDrawable(R.drawable.stats_tabicon_vrt_grey);
		Drawable verticalIconSelected = r.getDrawable(R.drawable.stats_tabicon_vrt_white);
		mVerticalTP = new VerticalTabPage(mContext, verticalIconReg, verticalIconSelected, verticalIconSelected, this);
		tabPages.add(mVerticalTP);
		
		// Distance
		Drawable distanceIconReg = r.getDrawable(R.drawable.stats_tabicon_dst_grey);
		Drawable distanceIconSelected = r.getDrawable(R.drawable.stats_tabicon_dst_white);
		mDistanceTP = new DistanceTabPage(mContext, distanceIconReg, distanceIconSelected, distanceIconSelected, this);
		tabPages.add(mDistanceTP);
		
		// Temperature
		Drawable tempIconReg = r.getDrawable(R.drawable.stats_tabicon_tmp_grey);
		Drawable tempIconSelected = r.getDrawable(R.drawable.stats_tabicon_tmp_white);
		mTemperatureTP = new TemperatureTabPage(mContext, tempIconReg, tempIconSelected, tempIconSelected, this);
		tabPages.add(mTemperatureTP);
		
		// Preferences
		Drawable preferencesIconReg = r.getDrawable(R.drawable.stats_tabicon_prefs_grey);
		Drawable preferencesIconSelected = r.getDrawable(R.drawable.stats_tabicon_prefs_white);
		mPreferencesTP = new PreferencesTabPage(mContext, preferencesIconReg, preferencesIconSelected, preferencesIconSelected, this);
		tabPages.add(mPreferencesTP);
		
		// Other init stuff
		this.setTabPages(tabPages);
		this.focusTabBar();
	}
	
	/*
	 * Sends the info bundle to all the tabs to populate the views
	 */
	public void setViewData(Bundle data) {
		if(D) Log.v(TAG, "setViewData");
		if(D) data = testBundle();
		
		((LastRunTabPage) mRunHistoryTP).setData(data);
		((SpeedTabPage) mSpeedTP).setData(data);
		((AltitudeTabPage) mAltitudeTP).setData(data);
		((VerticalTabPage) mVerticalTP).setData(data);
		((DistanceTabPage) mDistanceTP).setData(data);
		((TemperatureTabPage) mTemperatureTP).setData(data);
	}
	
	public boolean onSelectUp(View srcView) {
		return mPages.get(mSelectedTabIdx).onSelectUp(srcView);
	}
	
	public boolean onRightArrowUp(View srcView) {
		return mPages.get(mSelectedTabIdx).onRightArrowUp(srcView);
	}
	
	/*
	 * Returns a fake bundle for testing
	 */
	private Bundle testBundle() {
		Bundle testBundle = new Bundle();
		
		Random ran = new Random();
		
		// Runs
		Bundle runBundle = new Bundle();
		runBundle.putInt("AllTimeTotalNumberOfSkiRuns", 666);
		ArrayList<Bundle> runs = new ArrayList<Bundle>();
		for(int i=0; i < 10; i++) {
			Bundle r = new Bundle();
			
			r.putInt("Number", i+1);
			r.putInt("Start", 100000000);
			r.putFloat("AverageSpeed", ran.nextFloat() * 100);
			r.putFloat("MaxSpeed", ran.nextFloat() * 100);
			r.putFloat("Distance", ran.nextFloat() * 15000);
			r.putFloat("Vertical",  ran.nextFloat() * 15000);
			
			runs.add(r);
		}
		
		runBundle.putParcelableArrayList("Runs", runs);
		testBundle.putBundle("RUN_BUNDLE", runBundle);
		
		// Speed
		Bundle speedBundle = new Bundle();
		speedBundle.putFloat("HorzSpeed", ran.nextFloat() * 100);
		speedBundle.putFloat("VertSpeed", ran.nextFloat() * 100);
		speedBundle.putFloat("Speed", ran.nextFloat() * 100);
		speedBundle.putFloat("MaxSpeed", ran.nextFloat() * 100);
		speedBundle.putFloat("AllTimeMaxSpeed", ran.nextFloat() * 100);
		speedBundle.putFloat("AverageSpeed", ran.nextFloat() * 100);
		testBundle.putBundle("SPEED_BUNDLE", speedBundle);
		
		//Altitude
		Bundle altBundle = new Bundle();
		altBundle.putFloat("PreviousAlt", ran.nextFloat() * 1000);
		altBundle.putFloat("Alt",ran.nextFloat() * 1000);
		altBundle.putFloat("AllTimeMaxAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("AllTimeMinAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("MaxAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("MinAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("PreviousMaxAlt", ran.nextFloat() * 1000);
		altBundle.putFloat("PreviousMinAlt", ran.nextFloat() * 1000);
		altBundle.putFloat("MaxPressureAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("MinPressureAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("PreviousMaxPressureAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("PreviousMinPressureAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("HeightOffsetAvg",ran.nextFloat() * 1000);
		altBundle.putInt("HeightOffsetN",ran.nextInt());
		altBundle.putFloat("GpsAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("PreviousGpsAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("PressureAlt",ran.nextFloat() * 1000);
		altBundle.putFloat("PreviousPressureAlt",ran.nextFloat() * 1000);
		altBundle.putBoolean("IsCallibrating",ran.nextBoolean());
		altBundle.putBoolean("IsInitialized",ran.nextBoolean());
		altBundle.putFloat("Pressure", ran.nextFloat() * 100);
		testBundle.putBundle("ALTITUDE_BUNDLE", altBundle);
		
		// Distance
		Bundle distBundle = new Bundle();
		distBundle.putFloat("HorzDistance", ran.nextFloat() * 15000);
		distBundle.putFloat("VertDistance", ran.nextFloat() * 15000);
		distBundle.putFloat("Distance", ran.nextFloat() * 15000);
		distBundle.putFloat("AllTimeDistance", ran.nextFloat() * 15000);
		testBundle.putBundle("DISTANCE_BUNDLE", distBundle);
		
		// Vert Bundle
		Bundle vertBundle = new Bundle();
		vertBundle.putFloat("Vert", ran.nextFloat() * 5000);
		vertBundle.putFloat("PreviousVert", ran.nextFloat() * 5000);
		vertBundle.putFloat("AllTimeVert", ran.nextFloat() * 5000);
		testBundle.putBundle("VERTICAL_BUNDLE", vertBundle);
		
		// Temp Bundle
		Bundle tempBundle = new Bundle();
		tempBundle.putInt("MaxTemperature", ran.nextInt(100) - 50);
		tempBundle.putInt("MinTemperature", ran.nextInt(100) - 50);
		tempBundle.putInt("AllTimeMaxTemperature", ran.nextInt(100) - 50);
		tempBundle.putInt("AllTimeMinTemperature", ran.nextInt(100) - 50);
		tempBundle.putInt("Temperature", ran.nextInt(100) - 50);
		testBundle.putBundle("TEMPERATURE_BUNDLE", tempBundle);
		
		return testBundle;
	}
}
