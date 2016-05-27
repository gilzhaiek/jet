package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.TextView;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.dashlauncher.livestats.ReconSettingsUtil;
import com.reconinstruments.dashlivestats.R;
import com.reconinstruments.utils.ConversionUtil;

import java.text.DecimalFormat;


public class ReconVrtWidget extends ReconDashboardWidget {
	
	private static final String TAG = ReconVrtWidget.class.getSimpleName();

    private TextView prFieldTextView;

	public ReconVrtWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ReconVrtWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconVrtWidget(Context context) {
		super(context);
	}

	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
		Bundle vrtBundle = (Bundle) fullinfo.get("VERTICAL_BUNDLE");
		float vrt = vrtBundle.getFloat("Vert");
		float prVRT = vrtBundle.getFloat("AllTimeVert");
		
		DecimalFormat df = new DecimalFormat();
		
		if(ReconSettingsUtil.getUnits(this.getContext()) == ReconSettingsUtil.RECON_UNITS_METRIC) {
//			int vrtInt = (int)vrt;
//			vrtInt = vrtInt%
			fieldTextView.setText(prefixZeroes(vrt, inActivity));
			if(prFieldTextView != null){
			    prFieldTextView.setText(Integer.toString((int)prVRT));
			}
			unitTextView.setText("M");
		} else {
			// IMPERIAL
			double ft = ConversionUtil.metersToFeet(vrt);
			df.setMaximumFractionDigits(0);
			fieldTextView.setText(prefixZeroes((float)ft, inActivity));
			if(prFieldTextView != null){
    			ft = ConversionUtil.metersToFeet(prVRT);
                df.setMaximumFractionDigits(0);
                prFieldTextView.setText(df.format(prVRT));
			}
			unitTextView.setText("FT");
		}
	}

	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
		prFieldTextView = (TextView) findViewById(R.id.pr_vrt_field);
	}

}
