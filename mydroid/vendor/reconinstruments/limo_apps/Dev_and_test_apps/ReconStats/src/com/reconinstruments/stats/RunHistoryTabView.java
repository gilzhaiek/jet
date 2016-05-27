package com.reconinstruments.stats;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.reconsettings.ReconSettingsUtil;
import com.reconinstruments.reconstats.R;
import com.reconinstruments.stats.util.ConversionUtil;
import com.reconinstruments.stats.util.FontSingleton;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class RunHistoryTabView extends TabView {

	private Context mContext;
	ArrayList<TabPage> tabPages = new ArrayList<TabPage>(3);
	private ArrayList<Bundle> runs;
	
	public RunHistoryTabView(Context context, ArrayList<Bundle> runsList) {
		super(context);
		mContext = context;
		runs = runsList;
		createTabPages();
	}
	
	public void createTabPages() {
		Context context = this.getContext();
		
		// Create Runs Tabs
		Collections.reverse(runs);
		for(Bundle run : runs) {
			RunTabPage runTP = new RunTabPage(mContext, Integer.toString(run.getInt("Number")), this, run);
			tabPages.add(runTP);
		}
		
		// Other init stuff
		this.setTabPages(tabPages);
		this.focusTabBar();
	}
	
	class RunTabPage extends TabPage {

		private View mTheBigView;
		
		private TextView titleTV, avgSpdTV, avgSpdFieldTV, avgSpdUnitTV, maxSpdTV, maxSpdFieldTV, maxSpdUnitTV;
		private TextView vertTV, vertFieldTV, vertUnitTV, distTV, distFieldTV, distUnitTV;
		
		public RunTabPage(Context context, String tabTxt, TabView hostView, Bundle runBundle) {
			super(context, tabTxt, hostView);

			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mTheBigView = inflater.inflate(R.layout.tab_run_layout, null);

			this.addView(mTheBigView);
			
			titleTV = (TextView) mTheBigView.findViewById(R.id.tab_title);
			
			maxSpdTV = (TextView) mTheBigView.findViewById(R.id.max_speed);
			maxSpdFieldTV = (TextView) mTheBigView.findViewById(R.id.max_speed_field);
			maxSpdUnitTV = (TextView) mTheBigView.findViewById(R.id.max_speed_field_unit);
			
			avgSpdTV = (TextView) mTheBigView.findViewById(R.id.avg_speed);
			avgSpdFieldTV = (TextView) mTheBigView.findViewById(R.id.avg_speed_field);
			avgSpdUnitTV = (TextView) mTheBigView.findViewById(R.id.avg_speed_field_unit);
			
			vertTV = (TextView) mTheBigView.findViewById(R.id.vertical);
			vertFieldTV = (TextView) mTheBigView.findViewById(R.id.vertical_field);
			vertUnitTV = (TextView) mTheBigView.findViewById(R.id.vertical_field_unit);
			
			distTV = (TextView) mTheBigView.findViewById(R.id.distance);
			distFieldTV = (TextView) mTheBigView.findViewById(R.id.distance_field);
			distUnitTV = (TextView) mTheBigView.findViewById(R.id.distance_field_unit);
			
			// Set Font
			Typeface tf = FontSingleton.getInstance(mContext).getTypeface();
			titleTV.setTypeface(tf);
			
			maxSpdTV.setTypeface(tf);
			maxSpdFieldTV.setTypeface(tf);
			maxSpdUnitTV.setTypeface(tf);
			
			avgSpdTV.setTypeface(tf);
			avgSpdFieldTV.setTypeface(tf);
			avgSpdUnitTV.setTypeface(tf);
			
			vertTV.setTypeface(tf);
			vertFieldTV.setTypeface(tf);
			vertUnitTV.setTypeface(tf);
			
			distTV.setTypeface(tf);
			distFieldTV.setTypeface(tf);
			distUnitTV.setTypeface(tf);
			
			titleTV.setText("RUN #" + runBundle.getInt("Number"));
			
			setData(runBundle);
		}
		
		public void setData(Bundle data) {
			boolean metric = ReconSettingsUtil.getUnits(mContext) == ReconSettingsUtil.RECON_UINTS_METRIC;
			
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(0);
			df.setMinimumFractionDigits(0);
			
			if(metric) {
				maxSpdFieldTV.setText(df.format(data.getFloat("MaxSpeed")));
				maxSpdUnitTV.setText("km/h");
				
				avgSpdFieldTV.setText(df.format(data.getFloat("AverageSpeed")));
				avgSpdUnitTV.setText("km/h");
				
				vertFieldTV.setText(df.format(data.getFloat("Vertical")));
				vertUnitTV.setText("m");
				
				float dist = data.getFloat("Distance");
				if(dist < 10000) {
					distFieldTV.setText(df.format(dist));
					distUnitTV.setText("m");
				} else {
					df.setMaximumFractionDigits(2);
					df.setMinimumFractionDigits(2);
					distFieldTV.setText(df.format(dist / 1000));
					distUnitTV.setText("km");
				}
			
			} else {
				float maxSpd = (float) ConversionUtil.kmsToMiles(data.getFloat("MaxSpeed"));
				maxSpdFieldTV.setText(df.format(maxSpd));
				maxSpdUnitTV.setText("mph");
				
				float avgSpd = (float) ConversionUtil.kmsToMiles(data.getFloat("AverageSpeed"));
				avgSpdFieldTV.setText(df.format(avgSpd));
				avgSpdUnitTV.setText("mph");
				
				float vert = data.getFloat("Vertical");
				vertFieldTV.setText(df.format(vert));
				vertUnitTV.setText("ft");
				
				float dist = data.getFloat("Distance");
				if(ConversionUtil.metersToFeet(dist) < 10000) {
					distFieldTV.setText(df.format(ConversionUtil.metersToFeet(dist)));
					distUnitTV.setText("ft");
				} else {
					df.setMaximumFractionDigits(2);
					df.setMinimumFractionDigits(2);
					distFieldTV.setText(df.format(ConversionUtil.metersToMiles(dist)));
					distUnitTV.setText("mi");
				}
			}
		}
	}
}
