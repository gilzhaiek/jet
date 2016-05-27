package com.reconinstruments.dashboard;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.reconinstruments.applauncher.transcend.ReconChronoManager;
import com.reconinstruments.applauncher.transcend.ReconJump;
import com.reconinstruments.dashboard.widgets.FontSingleton;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ReconDashboardStats extends Activity {

	private Bundle mFullInfoBundle;
	private DetailTabView mTabView;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTabView = new DetailTabView(this);
		mFullInfoBundle = this.getIntent().getBundleExtra("FULL_INFO_BUNDLE");
		if (mFullInfoBundle != null)
			mTabView.updateTabPages(mFullInfoBundle);
		setContentView(mTabView);
	}
	

	public class DetailTabView extends TabView {

		ArrayList<TabPage> tabPages = new ArrayList<TabPage>(3);

		public DetailTabView(Context context) {
			super(context);
			createTabPages();
		}

		private void createTabPages() {
			Context context = this.getContext();
			Resources r = context.getResources();

			// Last Run Tab
			Drawable statsIconReg = r.getDrawable(R.drawable.tab_stats_grey);
			Drawable statsIconSelected = r.getDrawable(R.drawable.tab_stats_white);
			DetailTabPage LastRunDetailTabPage = new LastRunDetailTabPage(context, statsIconReg, statsIconSelected, statsIconSelected, this);
			tabPages.add(LastRunDetailTabPage);

			// Last Jump Tab
			Drawable statsJumpReg = r.getDrawable(R.drawable.tab_jump_grey);
			Drawable statsJumpSelected = r.getDrawable(R.drawable.tab_jump_white);
			DetailTabPage statsJumpPage = new LastJumpDetailTabPage(context, statsJumpReg, statsJumpSelected, statsJumpSelected, this);
			tabPages.add(statsJumpPage);
			
			// Chrono Tab
			Drawable statsChronoReg = r.getDrawable(R.drawable.tab_chrono_grey);
			Drawable statsChronoSelected = r.getDrawable(R.drawable.tab_chrono_white);
			DetailTabPage statsChronoPage = new ChronoDetailTabPage(context, statsChronoReg, statsChronoSelected, statsChronoSelected,
					this);
			tabPages.add(statsChronoPage);
			
			// Other init stuff
			this.setTabPages(tabPages);
			this.focusTabBar();
		}
		
		public void updateTabPages(Bundle b) {
			for (TabPage t : tabPages) {
				if (t instanceof DetailTabPage)
					((DetailTabPage) t).updateView(b);
			}
		}
		
		public boolean onSelectUp(View srcView) {
			Log.v("ReconDashboardStats", "onSelectUp");
			return mPages.get(mSelectedTabIdx).onSelectUp(srcView);
		}
		
		public boolean onRightArrowUp(View srcView) {
			return mPages.get(mSelectedTabIdx).onRightArrowUp(srcView);
		}
		
		public boolean onLeftArrowUp(View srcView) {
            finish();
            return true;
		}
	}

	class DetailTabPage extends TabPage {

		public DetailTabPage(Context context, Drawable iconRegular,
				Drawable iconSelected, Drawable iconFocused, TabView hostView) {
			super(context, iconRegular, iconSelected, iconFocused, hostView);
		}
		
		public void updateView(Bundle b) {};
		
	}
	
	class LastRunDetailTabPage extends DetailTabPage {

		private View mTheBigView;
		private TextView lastRunTextView, lastRunNumberTextView;
		private TextView maxSpeedTextView, avgSpeedTextView, verticalTextView,
				distanceTextView;
		private TextView maxSpeedUnitTextView, avgSpeedUnitTextView,
				verticalUnitTextView, distanceUnitTextView;

		public LastRunDetailTabPage(Context context, Drawable iconRegular,
				Drawable iconSelected, Drawable iconFocused, TabView hostView) {
			super(context, iconRegular, iconSelected, iconFocused, hostView);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mTheBigView = inflater.inflate(R.layout.details_last_run_layout,
					null);

			this.addView(mTheBigView);
			
			lastRunNumberTextView = (TextView) mTheBigView.findViewById(R.id.last_run_number);
			maxSpeedTextView = (TextView) mTheBigView.findViewById(R.id.max_speed_field);
			avgSpeedTextView = (TextView) mTheBigView.findViewById(R.id.avg_speed_field);
			verticalTextView = (TextView) mTheBigView.findViewById(R.id.vertical_field);
			distanceTextView = (TextView) mTheBigView.findViewById(R.id.distance_field);
			maxSpeedUnitTextView = (TextView) mTheBigView.findViewById(R.id.max_speed_field_unit);
			avgSpeedUnitTextView = (TextView) mTheBigView.findViewById(R.id.avg_speed_field_unit);
			verticalUnitTextView = (TextView) mTheBigView.findViewById(R.id.vertical_field_unit);
			distanceUnitTextView = (TextView) mTheBigView.findViewById(R.id.distance_field_unit);

			// Set Font
			FontSingleton font = FontSingleton.getInstance(getContext());
			ViewGroup v = (RelativeLayout) mTheBigView.findViewById(R.id.last_run_layout);
			int numChildren = v.getChildCount();
			for (int j = 0; j < numChildren; j++) {
				View child = v.getChildAt(j);
				if (child instanceof TextView)
					((TextView) child).setTypeface(font.getTypeface());
			}
			
			// set units
			if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
				maxSpeedUnitTextView.setText("km/h");
				avgSpeedUnitTextView.setText("km/h");
				verticalUnitTextView.setText("m");
				distanceUnitTextView.setText("m");
			} else {
				maxSpeedUnitTextView.setText("mph");
				avgSpeedUnitTextView.setText("mph");
				verticalUnitTextView.setText("ft");
				distanceUnitTextView.setText("ft");
			}
		}
		
		public boolean onSelectUp(View srcView) {
			// Launch stats app
			launchStatsApp();			
			return true;
		}
		
		public boolean onRightArrowUp(View srcView) {
			launchStatsApp();
			return true;
		}
		
		
		private void launchStatsApp() {
			Log.v("ReconDashboardStats", "Launch Stats App");
			
			Intent intent = new Intent("RECON_STATS");
			startActivity(intent);
		}
		
		public void updateView(Bundle infoBundle) {
			Bundle runBundle = infoBundle.getBundle("RUN_BUNDLE");
			
			ArrayList<Bundle> runs = runBundle.getParcelableArrayList("Runs");
			
			Bundle lastRun = null;
			
			if(runs.isEmpty()) {
				lastRunNumberTextView.setVisibility(View.INVISIBLE);
				return;
			}
			
			Log.v("stat view", "num runs: " + runs.size());
			lastRun = runs.get(runs.size() - 1);
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(0);

			if(lastRun.getInt("Number") == 0) {
				lastRunNumberTextView.setVisibility(View.INVISIBLE);
			} else {
				lastRunNumberTextView.setText("#" + lastRun.getInt("Number"));
				lastRunNumberTextView.setVisibility(View.VISIBLE);
			}

			if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
				maxSpeedTextView.setText(df.format(lastRun.getFloat("MaxSpeed")));
				avgSpeedTextView.setText(df.format(lastRun.getFloat("AverageSpeed")));
				verticalTextView.setText(df.format(lastRun.getFloat("Vertical")));
				distanceTextView.setText(df.format(lastRun.getFloat("Distance")));
				
				maxSpeedUnitTextView.setText("km/h");
				avgSpeedUnitTextView.setText("km/h");
				verticalUnitTextView.setText("m");
				distanceUnitTextView.setText("m");
			} else {
				double speed_imperial = ConversionUtil.kmsToMiles(lastRun.getFloat("MaxSpeed"));
				double avg_speed_imperial = ConversionUtil.kmsToMiles(lastRun.getFloat("AverageSpeed"));
				double vert_imperial = ConversionUtil.metersToFeet(lastRun.getFloat("Vertical"));
				double distance_imperial = ConversionUtil.metersToFeet(lastRun.getFloat("Distance"));
				
				maxSpeedTextView.setText(df.format(speed_imperial));
				avgSpeedTextView.setText(df.format(avg_speed_imperial));
				verticalTextView.setText(df.format(vert_imperial));
				distanceTextView.setText(df.format(distance_imperial));
				
				maxSpeedUnitTextView.setText("mph");
				avgSpeedUnitTextView.setText("mph");
				verticalUnitTextView.setText("ft");
				distanceUnitTextView.setText("ft");
			}
		}
	}

	class LastJumpDetailTabPage extends DetailTabPage {

		private View mTheBigView;
		private TextView lastJumpNumberTextView;
		private TextView airTimeTextView, jumpDistanceTextView,
				jumpHeightTextView, jumpDropTextView;
		private TextView jumpDistanceUnitTextView, jumpHeightUnitTextView,
				jumpDropUnitTextView;

		public LastJumpDetailTabPage(Context context, Drawable iconRegular,
				Drawable iconSelected, Drawable iconFocused, TabView hostView) {
			super(context, iconRegular, iconSelected, iconFocused, hostView);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mTheBigView = inflater.inflate(R.layout.details_last_jump_layout,
					null);

			this.addView(mTheBigView);

			lastJumpNumberTextView = (TextView) mTheBigView.findViewById(R.id.last_jump_number);
			airTimeTextView = (TextView) mTheBigView.findViewById(R.id.air_time_field);
			jumpDistanceTextView = (TextView) mTheBigView.findViewById(R.id.jump_distance_field);
			jumpHeightTextView = (TextView) mTheBigView.findViewById(R.id.height_field);
			jumpDropTextView = (TextView) mTheBigView.findViewById(R.id.drop_field);
			jumpDistanceUnitTextView = (TextView) mTheBigView.findViewById(R.id.jump_distance_field_unit);
			jumpHeightUnitTextView = (TextView) mTheBigView.findViewById(R.id.height_field_unit);
			jumpDropUnitTextView = (TextView) mTheBigView.findViewById(R.id.drop_field_unit);
			
			// Set Font
			FontSingleton font = FontSingleton.getInstance(getContext());
			ViewGroup v = (ViewGroup) mTheBigView.findViewById(R.id.last_jump_layout);
			int numChildren = v.getChildCount();
			for (int j = 0; j < numChildren; j++) {
				View child = v.getChildAt(j);
				if (child instanceof TextView)
					((TextView) child).setTypeface(font.getTypeface());
			}
		}

		public boolean onSelectUp(View srcView) {
			// Launch jump app
			launchJumpApp();
			return true;
		}
		
		public boolean onRightArrowUp(View srcView) {
			launchJumpApp();
			return true;
		}
		
		private void launchJumpApp() {
			Log.v("ReconDashboardStats", "Launch Jump App");

			Intent intent = new Intent("RECON_JUMP");
			startActivity(intent);
		}
		
		public void updateView(Bundle infoBundle) {
			Bundle jumpsBundle = infoBundle.getBundle("JUMP_BUNDLE");
			
			ArrayList<Bundle> jumpsBundleList = jumpsBundle.getParcelableArrayList("Jumps");

			if(jumpsBundleList.isEmpty()) { 
				lastJumpNumberTextView.setVisibility(View.INVISIBLE);
				return;
			}
			
			Bundle data = jumpsBundleList.get(jumpsBundleList.size()-1);
			
			if(data == null) return;
			
			int runNum = data.getInt("Number");
			int airTime = data.getInt("Air");
			float distance = data.getFloat("Distance");
			float height = data.getFloat("Height");
			float drop = data.getFloat("Drop");
			long date = data.getLong("Date");
			
			if(runNum != ReconJump.INVALID_NUMBER) {
				lastJumpNumberTextView.setText("#"+runNum);
			} else {
				lastJumpNumberTextView.setText("--");
			}
			
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(2);
			df.setMinimumFractionDigits(1);
			
			if(airTime != ReconJump.INVALID_AIR) {
				double airTimeSecs = ((double) airTime) / 1000;
				airTimeTextView.setText(df.format(airTimeSecs));
			} else {
				airTimeTextView.setText("--");
			}
			
			if(distance != ReconJump.INVALID_DISTANCE) {
				if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
					jumpDistanceTextView.setText(df.format(distance));
				} else {
					Double distFeet = ConversionUtil.metersToFeet((double) distance);
					jumpDistanceTextView.setText(df.format(distFeet));
				}
			} else {
				jumpDistanceTextView.setText("--");
			}
			
			if(height != ReconJump.INVALID_HEIGHT) {
				if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
					jumpHeightTextView.setText(df.format(height));
				} else {
					Double heightFeet = ConversionUtil.metersToFeet((double) height);
					jumpHeightTextView.setText(df.format(heightFeet));
				}
				
			} else {
				jumpHeightTextView.setText("--");
			}
			
			if(drop != ReconJump.INVALID_DROP) {
				if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
					jumpDropTextView.setText(df.format(drop));
				} else {
					Double dropFeet = ConversionUtil.metersToFeet((double) drop);
					jumpHeightTextView.setText(df.format(dropFeet));
				}
			} else {
				jumpDropTextView.setText("--");
			}
			
			// Set units
			if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UINTS_METRIC) {
				jumpDistanceUnitTextView.setText("m");
				jumpHeightUnitTextView.setText("m");
				jumpDropUnitTextView.setText("m");
			} else {
				jumpDistanceUnitTextView.setText("ft");
				jumpHeightUnitTextView.setText("ft");
				jumpDropUnitTextView.setText("ft");
			}
		}
	}

	class ChronoDetailTabPage extends DetailTabPage {

		private View mTheBigView;
		private TextView lastChronoNumberTextView, chronoTimeTextView, chronoTimeMsTextView, chronoStartEndTimeTextView;

		public ChronoDetailTabPage(Context context, Drawable iconRegular,
				Drawable iconSelected, Drawable iconFocused, TabView hostView) {
			super(context, iconRegular, iconSelected, iconFocused, hostView);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mTheBigView = inflater
					.inflate(R.layout.details_chrono_layout, null);

			this.addView(mTheBigView);

			lastChronoNumberTextView = (TextView) mTheBigView.findViewById(R.id.chrono_number);
			chronoTimeTextView = (TextView) mTheBigView.findViewById(R.id.chrono_time);
			chronoTimeMsTextView = (TextView) mTheBigView.findViewById(R.id.chrono_time_ms);
			chronoStartEndTimeTextView = (TextView) mTheBigView.findViewById(R.id.chrono_start_end_time);
			
			// Set Font
			FontSingleton font = FontSingleton.getInstance(getContext());
			ViewGroup v = (ViewGroup) mTheBigView.findViewById(R.id.chrono_layout);
			int numChildren = v.getChildCount();
			for (int j = 0; j < numChildren; j++) {
				View child = v.getChildAt(j);
				if (child instanceof TextView)
					((TextView) child).setTypeface(font.getTypeface());
			}
		}

		public boolean onSelectUp(View srcView) {
			launchChronoApp();
			return true;
		}
		
		public boolean onRightArrowUp(View srcView) {
			launchChronoApp();
			return true;
		}
		
		private void launchChronoApp() {
			Log.v("ReconDashboardStats", "Launch Chrono App");
			
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setComponent(new ComponentName("com.reconinstruments.chrono","com.reconinstruments.chrono.ReconChrono"));
			startActivity(intent);
		}
		
		public void updateView(Bundle infoBundle) {
			ArrayList<Bundle> trials = infoBundle.getBundle("CHRONO_BUNDLE").getParcelableArrayList("Trials");
			Bundle lastTrial = trials.get(trials.size() - 1);
			
			lastChronoNumberTextView.setText("# " + Integer.toString(trials.size()));
			
			Long elapsedTime = lastTrial.getLong("ElapsedTime");
			chronoTimeTextView.setText(ReconChronoManager.parseElapsedTime(elapsedTime, true));
			chronoTimeMsTextView.setText(ReconChronoManager.parseElapsedTime(elapsedTime, false));
			
			Date startTime = new Date(lastTrial.getLong("StartTime"));
			Date endTime = new Date(lastTrial.getLong("EndTime"));
			chronoStartEndTimeTextView.setText(DateFormat.format("h:mmaa", startTime) + " - " + DateFormat.format("h:mmaa", endTime));
		}
		
	}
}
