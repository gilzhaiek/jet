package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.utils.stats.StatsUtil;

public class ReconCalorieWidget extends ReconDashboardWidget {

	private static final String TAG = ReconCalorieWidget.class.getSimpleName();

	public ReconCalorieWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	public ReconCalorieWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconCalorieWidget(Context context) {
		super(context);
	}

	
	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
		Bundle tempBundle = (Bundle) fullinfo.get("CALORIE_BUNDLE");
		float value = tempBundle.getFloat("TotalCalories");
	    if(value != StatsUtil.INVALID_CALORIES){
			fieldTextView.setText(prefixZeroes(value, inActivity));
		} else {
			fieldTextView.setText(Html.fromHtml(((inActivity) ? "000" : "<font color=\"#808181\">000</font>")));
		}
		unitTextView.setText("");
		fieldTextView.invalidate();
	}

	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}

}
