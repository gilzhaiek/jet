package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;

public class ReconHeartRateAvgWidget extends ReconDashboardWidget {

	private static final String TAG = ReconHeartRateAvgWidget.class.getSimpleName();
	
	public ReconHeartRateAvgWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	public ReconHeartRateAvgWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconHeartRateAvgWidget(Context context) {
		super(context);
	}

	
	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
		Bundle tempBundle = (Bundle) fullinfo.get("HR_BUNDLE");
		int value = tempBundle.getInt("AverageHeartRate");
		if(!inActivity){ // show '---'
	        fieldTextView.setText(Html.fromHtml(mInvalidGreyString));
	    }else{
	        if(value != 255) {
	            fieldTextView.setText(prefixZeroes(value, inActivity));
	        } else {
	            fieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
	        }
	    }
		unitTextView.setText("BPM");
		fieldTextView.invalidate();
	}

	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}

}
