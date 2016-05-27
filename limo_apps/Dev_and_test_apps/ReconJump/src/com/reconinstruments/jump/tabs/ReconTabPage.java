package com.reconinstruments.jump.tabs;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.applauncher.transcend.ReconJump;
import com.reconinstruments.reconsettings.ReconSettingsUtil;
import com.reconinstruments.jump.ConversionUtil;
import com.reconinstruments.jump.FontSingleton;
import com.reconinstruments.jump.R;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class ReconTabPage extends TabPage {

	private Context mContext;
	private View mTheBigView;
	protected TextView jumpTitleTextView, lastJumpNumberTextView, jumpDateTextView;
	protected TextView airTimeTextView, jumpDistanceTextView,	jumpHeightTextView, jumpDropTextView;
	protected TextView jumpDistanceUnitTextView, jumpHeightUnitTextView, jumpDropUnitTextView;
	
	public ReconTabPage(Context context, Drawable iconRegular, Drawable iconSelected, Drawable iconFocused, TabView hostView, String pageTitle) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);
		mContext = context;
		init(pageTitle);
	}

	public ReconTabPage(Context context, String text, TabView hostView, String pageTitle) {
		super(context, text, hostView);
		mContext = context;
		init(pageTitle);
	}
	
	private void init(String pageTitle) {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mTheBigView = inflater.inflate(R.layout.detail_layout, null);

		this.addView(mTheBigView);
		
		jumpTitleTextView = (TextView) mTheBigView.findViewById(R.id.jump_title);
		lastJumpNumberTextView = (TextView) mTheBigView.findViewById(R.id.last_jump_number);
		jumpDateTextView = (TextView) mTheBigView.findViewById(R.id.jump_time);
		airTimeTextView = (TextView) mTheBigView.findViewById(R.id.air_time_field);
		jumpDistanceTextView = (TextView) mTheBigView.findViewById(R.id.jump_distance_field);
		jumpHeightTextView = (TextView) mTheBigView.findViewById(R.id.height_field);
		jumpDropTextView = (TextView) mTheBigView.findViewById(R.id.drop_field);
		jumpDistanceUnitTextView = (TextView) mTheBigView.findViewById(R.id.jump_distance_field_unit);
		jumpHeightUnitTextView = (TextView) mTheBigView.findViewById(R.id.height_field_unit);
		jumpDropUnitTextView = (TextView) mTheBigView.findViewById(R.id.drop_field_unit);
		
		//Hide arrow
		((ImageView) mTheBigView.findViewById(R.id.arrow)).setVisibility(View.GONE);
		
		// Populate fields
		jumpTitleTextView.setText(pageTitle);
		
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
	
	/**
	 * Sets the data in this view
	 * @param data an individual jump bundle
	 */
	public void setData(Bundle data) {
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
		
		if(date != ReconJump.INVALID_DATE) {
			long currentDateInMillis = (new Date()).getTime();
			long eventDateInMillis = date;
			
			int diffInDays = (int) Math.floor((currentDateInMillis - eventDateInMillis) / (24 * 60 * 60 * 1000));
			if(diffInDays == 0) {
				SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa", Locale.US);
				jumpDateTextView.setText(sdf.format(new Date(date)));
			} else {
				SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.yy", Locale.US);
				jumpDateTextView.setText(sdf.format(new Date(date)));
			}
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
