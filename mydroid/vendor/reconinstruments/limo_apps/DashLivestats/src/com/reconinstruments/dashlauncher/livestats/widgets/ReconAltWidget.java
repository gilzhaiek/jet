package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.utils.ConversionUtil;


public class ReconAltWidget extends ReconDashboardWidget {
	
	public ReconAltWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public ReconAltWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconAltWidget(Context context) {
		super(context);
	}
	
	@Override
	public void updateInfo(Bundle fullInfo, boolean inActivity) {
        super.updateInfo(fullInfo, inActivity);
		Bundle altBundle = (Bundle) fullInfo.get("ALTITUDE_BUNDLE");
		float alt = altBundle.getFloat("Alt");
		boolean isUncertain = altBundle.getInt("HeightOffsetN") < 500;
		if(isUncertain) {
			fieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
		} else {
			if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
				fieldTextView.setText(prefixZeroes(alt, inActivity));
				unitTextView.setText("M");
			} else {
				// write imperial units
				fieldTextView.setText(prefixZeroes((float)ConversionUtil.metersToFeet((double) alt), inActivity));
				unitTextView.setText("FT");
			}
		}
	}
	
	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}
}