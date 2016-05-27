//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.utils.ConversionUtil;

public class ReconElevationGainWidget extends ReconDashboardWidget {

    private static final String TAG = ReconElevationGainWidget.class.getSimpleName();
        
    public ReconElevationGainWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public ReconElevationGainWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReconElevationGainWidget(Context context) {
        super(context);
    }

        
    @Override
    public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
        Bundle tempBundle = (Bundle) fullinfo.get("VERTICAL_BUNDLE");
        float value = tempBundle.getFloat("ElevGain");
        int unit = ReconSettingsUtil.getUnits(this.getContext());
            if(Float.compare(value, 0.0f) > 0) {
                if(unit == ReconSettingsUtil.RECON_UNITS_METRIC) {
                    fieldTextView.setText(prefixZeroes(value, inActivity));
                    unitTextView.setText("M");
                }else{
                    double ft = ConversionUtil.metersToFeet(value);
                    fieldTextView.setText(prefixZeroes((float)ft, inActivity));
                    unitTextView.setText("FT");
                }
            } else {
                fieldTextView.setText(Html.fromHtml(((inActivity) ? "000" : "<font color=\"#808181\">000</font>")));
                if(unit == ReconSettingsUtil.RECON_UNITS_METRIC) {
                    unitTextView.setText("M");
                }else{
                    unitTextView.setText("FT");
                }
            }
        fieldTextView.invalidate();
    }

    @Override
    public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
    }

}
