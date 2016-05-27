package com.reconinstruments.reconstats.tabs;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.reconsettings.ReconSettingsUtil;
import com.reconinstruments.reconstats.R;
import com.reconinstruments.reconstats.RunHistoryActivity;
import com.reconinstruments.reconstats.util.*;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class LastRunTabPage extends TabPage {

	private Context mContext;
	private View mTheBigView;
	
	private TextView titleTV, lastRunTV, lastRunFieldTV, maxSpdTV, maxSpdFieldTV, maxSpdUnitTV;
	private TextView vertTV, vertFieldTV, vertUnitTV, distTV, distFieldTV, distUnitTV;
	
	private ArrayList<Bundle> runsList = null;
	
	public LastRunTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);
		
		mContext = context;
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTheBigView = inflater.inflate(R.layout.tab_lastrun_layout, null);

		this.addView(mTheBigView);
		
		// Populate view
		titleTV = (TextView) mTheBigView.findViewById(R.id.tab_title);
		
		lastRunTV = (TextView) mTheBigView.findViewById(R.id.last_run);
		lastRunFieldTV = (TextView) mTheBigView.findViewById(R.id.last_run_field);
		
		maxSpdTV = (TextView) mTheBigView.findViewById(R.id.max_speed);
		maxSpdFieldTV = (TextView) mTheBigView.findViewById(R.id.max_speed_field);
		maxSpdUnitTV = (TextView) mTheBigView.findViewById(R.id.max_speed_field_unit);
		
		vertTV = (TextView) mTheBigView.findViewById(R.id.vertical);
		vertFieldTV = (TextView) mTheBigView.findViewById(R.id.vertical_field);
		vertUnitTV = (TextView) mTheBigView.findViewById(R.id.vertical_field_unit);
		
		distTV = (TextView) mTheBigView.findViewById(R.id.distance);
		distFieldTV = (TextView) mTheBigView.findViewById(R.id.distance_field);
		distUnitTV = (TextView) mTheBigView.findViewById(R.id.distance_field_unit);
		
		// Set Font
		Typeface tf = FontSingleton.getInstance(mContext).getTypeface();
		titleTV.setTypeface(tf);
		
		lastRunTV.setTypeface(tf);
		lastRunFieldTV.setTypeface(tf);
		
		maxSpdTV.setTypeface(tf);
		maxSpdFieldTV.setTypeface(tf);
		maxSpdUnitTV.setTypeface(tf);
		
		vertTV.setTypeface(tf);
		vertFieldTV.setTypeface(tf);
		vertUnitTV.setTypeface(tf);
		
		distTV.setTypeface(tf);
		distFieldTV.setTypeface(tf);
		distUnitTV.setTypeface(tf);
		
		titleTV.setText("RUN HISTORY");	
	}

	public boolean onSelectUp(View srcView) {
		if(runsList != null && runsList.size() > 1) {
			Log.d("LastRunTabPage", "Launch Run History");
			Intent i = new Intent(mContext, RunHistoryActivity.class);
			i.putParcelableArrayListExtra("Runs", runsList);
			mContext.startActivity(i);
		}
		return true;
	}
	
	public void setData(Bundle data) {
		Bundle runsBundle = data.getBundle("RUN_BUNDLE");
		runsList = runsBundle.getParcelableArrayList("Runs");
		
		lastRunFieldTV.setText("#" + Integer.toString(runsList.size()));
		
		if(runsList.size() > 1) {
			mTheBigView.findViewById(R.id.arrow).setVisibility(View.VISIBLE);
		} else {
			mTheBigView.findViewById(R.id.arrow).setVisibility(View.GONE);
		}
		
		if(runsList.size() < 1)
			return;
		
		Bundle mostRecentRun = runsList.get(runsList.size() - 1);
		
		boolean metric = ReconSettingsUtil.getUnits(mContext) == ReconSettingsUtil.RECON_UINTS_METRIC;
			
		if(metric) {
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(0);
			df.setMinimumFractionDigits(0);
			
			maxSpdFieldTV.setText(df.format(mostRecentRun.getFloat("MaxSpeed")));
			maxSpdUnitTV.setText("km/h");
			
			vertFieldTV.setText(df.format(mostRecentRun.getFloat("Vertical")));
			vertUnitTV.setText("m");
			
			float dist = mostRecentRun.getFloat("Distance");
			if(dist < 10000) {
				distFieldTV.setText(df.format(mostRecentRun.getFloat("Distance")));
				distUnitTV.setText("m");
			} else {
				float distKm = dist / 1000;
				
				df.setMinimumFractionDigits(2);
				df.setMaximumFractionDigits(2);
				
				distFieldTV.setText(df.format(distKm));
				distUnitTV.setText("km");
			}
		} else {
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(0);
			df.setMinimumFractionDigits(0);
			
			float speed = mostRecentRun.getFloat("MaxSpeed");
			speed = (float) ConversionUtil.kmsToMiles(speed);
			
			maxSpdFieldTV.setText(df.format(speed));
			maxSpdUnitTV.setText("mph");
			
			float vert = mostRecentRun.getFloat("Vertical");
			vert = (float) ConversionUtil.metersToFeet(vert);
			
			vertFieldTV.setText(df.format(vert));
			vertUnitTV.setText("ft");
			
			float dist = mostRecentRun.getFloat("Distance");
			if(ConversionUtil.metersToFeet(dist) < 10000) {
				distFieldTV.setText(df.format(ConversionUtil.metersToFeet(dist)));
				distUnitTV.setText("ft");
			} else {
				df.setMinimumFractionDigits(2);
				df.setMaximumFractionDigits(2);
				
				distFieldTV.setText(df.format(ConversionUtil.metersToMiles(dist)));
				distUnitTV.setText("mi");
			}
		}
	}
}
