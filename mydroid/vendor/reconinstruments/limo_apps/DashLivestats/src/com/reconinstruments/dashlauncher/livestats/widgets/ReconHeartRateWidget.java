package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;

public class ReconHeartRateWidget extends ReconDashboardWidget {

	private static final String TAG = ReconHeartRateWidget.class.getSimpleName();
	
	public ReconHeartRateWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	public ReconHeartRateWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconHeartRateWidget(Context context) {
		super(context);
	}

	
	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
		Bundle tempBundle = (Bundle) fullinfo.get("HR_BUNDLE");
		int value = tempBundle.getInt("HeartRate");
		if(value != 255) {
			fieldTextView.setText(prefixZeroes(value, inActivity));
		} else {
			fieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
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
