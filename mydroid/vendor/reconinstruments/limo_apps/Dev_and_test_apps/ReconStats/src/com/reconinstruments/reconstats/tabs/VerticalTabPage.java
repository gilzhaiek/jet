package com.reconinstruments.reconstats.tabs;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.reconsettings.ReconSettingsUtil;
import com.reconinstruments.reconstats.R;
import com.reconinstruments.reconstats.util.*;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class VerticalTabPage extends TabPage {

	//private static final String TAG = "VerticalTabPage";
	
	private Context mContext;
	private View mTheBigView;
	
	private TextView titleTV, totalVrtTV, totalVrtFieldTV, totalVrtUnitTV, runsTV, runsFieldTV;
	private TextView allTimeVrtTV, allTimeVrtFieldTV, allTimeVrtUnitTV;
	private TextView allTimeRunsTV, allTimeRunsFieldTV;
	
	public VerticalTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);
		
		mContext = context;
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTheBigView = inflater.inflate(R.layout.tab_vertical_layout, null);

		this.addView(mTheBigView);
		
		// Populate view
		titleTV = (TextView) mTheBigView.findViewById(R.id.tab_title);
		
		totalVrtTV = (TextView) mTheBigView.findViewById(R.id.total_vert);
		totalVrtFieldTV = (TextView) mTheBigView.findViewById(R.id.total_vert_field);
		totalVrtUnitTV = (TextView) mTheBigView.findViewById(R.id.total_vert_field_unit);
		
		runsTV = (TextView) mTheBigView.findViewById(R.id.runs);
		runsFieldTV = (TextView) mTheBigView.findViewById(R.id.runs_field);
		
		allTimeVrtTV = (TextView) mTheBigView.findViewById(R.id.all_time_vert);
		allTimeVrtFieldTV = (TextView) mTheBigView.findViewById(R.id.all_time_vert_field);
		allTimeVrtUnitTV = (TextView) mTheBigView.findViewById(R.id.all_time_vert_field_unit);
		
		allTimeRunsTV = (TextView) mTheBigView.findViewById(R.id.all_time_runs);
		allTimeRunsFieldTV = (TextView) mTheBigView.findViewById(R.id.all_time_runs_field);
		
		// Set Font
		Typeface tf = FontSingleton.getInstance(mContext).getTypeface();
		titleTV.setTypeface(tf);
		
		totalVrtTV.setTypeface(tf);
		totalVrtFieldTV.setTypeface(tf);
		totalVrtUnitTV.setTypeface(tf);
		
		runsTV.setTypeface(tf);
		runsFieldTV.setTypeface(tf);
		
		allTimeVrtTV.setTypeface(tf);
		allTimeVrtFieldTV.setTypeface(tf);
		allTimeVrtUnitTV.setTypeface(tf);
		
		allTimeRunsTV.setTypeface(tf);
		allTimeRunsFieldTV.setTypeface(tf);
		
		titleTV.setText("VERTICAL");	
	}

	public void setData(Bundle data) {
		Bundle runBundle = data.getBundle("RUN_BUNDLE");
		Bundle vertBundle = data.getBundle("VERTICAL_BUNDLE");
		
		if(runBundle == null && vertBundle == null) return;
		
		boolean metric = ReconSettingsUtil.getUnits(mContext) == ReconSettingsUtil.RECON_UINTS_METRIC;
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(0);
		df.setMinimumFractionDigits(0);
		
		if(metric) {
			if(vertBundle != null) {
				totalVrtFieldTV.setText(df.format(vertBundle.getFloat("Vert")));
				totalVrtUnitTV.setText("m");
				
				allTimeVrtFieldTV.setText(df.format(vertBundle.getFloat("AllTimeVert")));
				allTimeVrtUnitTV.setText("m");
			}
		} else {
			if(vertBundle != null) {
				float vrt = (float) ConversionUtil.metersToFeet(vertBundle.getFloat("Vert"));
				totalVrtFieldTV.setText(df.format(vrt));
				totalVrtUnitTV.setText("ft");
				
				float allVrt = (float) ConversionUtil.metersToFeet(vertBundle.getFloat("Vert"));
				allTimeVrtFieldTV.setText(df.format(allVrt));
				allTimeVrtUnitTV.setText("ft");
			}
		}
		
		if(runBundle != null) {
			ArrayList<Bundle> runs = runBundle.getParcelableArrayList("Runs");
			runsFieldTV.setText(Integer.toString(runs.size()));
			
			allTimeRunsFieldTV.setText(Integer.toString(runBundle.getInt("AllTimeTotalNumberOfSkiRuns")));
		}
	}
}
