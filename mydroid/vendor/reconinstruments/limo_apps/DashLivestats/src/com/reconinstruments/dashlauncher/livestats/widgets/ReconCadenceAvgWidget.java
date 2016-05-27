package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;

public class ReconCadenceAvgWidget extends ReconDashboardWidget {

	private static final String TAG = ReconCadenceAvgWidget.class.getSimpleName();
	
	public ReconCadenceAvgWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	public ReconCadenceAvgWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconCadenceAvgWidget(Context context) {
		super(context);
	}

	
	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
		Bundle tempBundle = (Bundle) fullinfo.get("CADENCE_BUNDLE");
		int value = tempBundle.getInt("AverageCadence");
        if(!inActivity){ // show '---'
            fieldTextView.setText(Html.fromHtml(mInvalidGreyString));
        }else{
            if(value != 65535) {
                fieldTextView.setText(prefixZeroes(value, inActivity));
            } else {
                fieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
            }
        }
		unitTextView.setText("RPM");
		fieldTextView.invalidate();
	}

	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}

}
