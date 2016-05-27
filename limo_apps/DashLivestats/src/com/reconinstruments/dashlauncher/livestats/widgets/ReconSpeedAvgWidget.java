package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.ConversionUtil;

public class ReconSpeedAvgWidget extends ReconDashboardWidget {

	private static final String TAG = ReconSpeedAvgWidget.class.getSimpleName();
	private TextView maxFieldTextView;
	
	public ReconSpeedAvgWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	public ReconSpeedAvgWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconSpeedAvgWidget(Context context) {
		super(context);
	}

	
	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
		Bundle tempBundle = (Bundle) fullinfo.get("SPEED_BUNDLE");
		float spd = tempBundle.getFloat("AverageSpeed");
		float maxSPD = tempBundle.getFloat("MaxSpeed");
		if(!inActivity){ // show '---'
		    fieldTextView.setText(Html.fromHtml(mInvalidGreyString));
		    if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
		        unitTextView.setText("KM/H");
		    }else{
		        unitTextView.setText("MPH");
		    }
		}else{
	        if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
	            if(spd > -1) {
	                fieldTextView.setText(prefixZeroes(spd, inActivity));
	            } else {
	                fieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
	            }
	            if(maxFieldTextView != null){
	                if(maxSPD > -1) {
	                    maxFieldTextView.setText(Integer.toString((int)maxSPD));
	                } else {
	                    maxFieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
	                }
	            }
	            unitTextView.setText("KM/H");
	        } else {
	            if(spd > -1) {
	                fieldTextView.setText(prefixZeroes((float)ConversionUtil.kmsToMiles(spd), inActivity));
	            } else {
	                fieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
	            }
	            if(maxFieldTextView != null){
	                if(maxSPD > -1) {
	                    maxFieldTextView.setText(Integer.toString((int)ConversionUtil.kmsToMiles(maxSPD)));
	                } else {
	                    maxFieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
	                }
	            }
	            unitTextView.setText("MPH");
	        }
		}
		fieldTextView.invalidate();
	}

	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
		maxFieldTextView = (TextView) findViewById(R.id.max_speed);
		if(isJet){
		    TextView subTitleTextView = (TextView) findViewById(R.id.sub_title);
		    if(subTitleTextView != null)
		        subTitleTextView.setVisibility(View.INVISIBLE);
		    if(subTitleTextView != null)
		        maxFieldTextView.setVisibility(View.INVISIBLE);
		}
	}

}
