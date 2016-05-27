package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.utils.stats.StatsUtil;
import com.reconinstruments.utils.ConversionUtil;

import java.text.DecimalFormat;

public class ReconDistanceWidget extends ReconDashboardWidget {
	
	public ReconDistanceWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public ReconDistanceWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconDistanceWidget(Context context) {
		super(context);
	}
	
	@Override
	public void updateInfo(Bundle fullInfo, boolean inActivity) {
        super.updateInfo(fullInfo, inActivity);
		Bundle distBundle = (Bundle) fullInfo.get("DISTANCE_BUNDLE");
		float dst = distBundle.getFloat("Distance");
		boolean invalid = true;
        if(!mHasGps && !inActivity){ // show '---'
            fieldTextView.setText(Html.fromHtml(((inActivity) ? "---" : "<font color=\"#808181\">---</font>")));
            if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
                unitTextView.setText("KM");
            }else{
                unitTextView.setText("MI");
            }
        }else{
            if(dst > StatsUtil.INVALID_DISTANCE){
                invalid = false;
            }
            DecimalFormat df = new DecimalFormat("0.00");
            if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
                dst /= 1000.00f;
                if(invalid){
                    //specified invalid value with others metrics.
                    fieldTextView.setText(Html.fromHtml(((inActivity) ? "0.00" : "<font color=\"#808181\">0.00</font>")));
                }else{
                    fieldTextView.setText(df.format((double)dst));
                }
                unitTextView.setText("KM");
            } else {
                // IMPERIAL
                double miles = ConversionUtil.metersToMiles(dst);
                if(invalid){
                    //specified invalid value with others metrics.
                    fieldTextView.setText(Html.fromHtml(((inActivity) ? "0.00" : "<font color=\"#808181\">0.00</font>")));
                }else{
                    fieldTextView.setText(df.format(miles));
                }
                unitTextView.setText("MI");
            }
        }
	}
	
	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}
}