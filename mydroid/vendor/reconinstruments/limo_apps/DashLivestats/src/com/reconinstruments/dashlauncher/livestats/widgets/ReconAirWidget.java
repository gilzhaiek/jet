package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ReconAirWidget extends ReconDashboardWidget {
	
	private String oneZero = "0";
	
	public ReconAirWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public ReconAirWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconAirWidget(Context context) {
		super(context);
	}
	
	@Override
	public void updateInfo(Bundle fullInfo, boolean inActivity) {
	    super.updateInfo(fullInfo, inActivity);
		Bundle jumpBundle = (Bundle) fullInfo.get("JUMP_BUNDLE");
		ArrayList<Bundle> jumpBundles = jumpBundle.getParcelableArrayList("Jumps");
		if(jumpBundles.isEmpty()) {
			fieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
			unitTextView.setText("");
		} else {
			Bundle jump = jumpBundles.get(jumpBundles.size() - 1);
			Double time = ((double) jump.getInt("Air")) / 1000;
			
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(1);
			df.setMinimumFractionDigits(1);
			
			if(time < 10){
				fieldTextView.setText(Html.fromHtml("<font color=\"#808181\">0</font>" + df.format(time)));
			}
			else {
				fieldTextView.setText(df.format(time));
			}
			unitTextView.setText("S");
		}
	}
	
	@Override
	public void prepareInsideViews() {
	    super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}
}