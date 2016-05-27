package com.reconinstruments.dashlauncher.livestats.widgets;


import java.text.DecimalFormat;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import com.reconinstruments.commonwidgets.AutofitHelper;

public class ReconTerrainGradeWidget extends ReconDashboardWidget {

	private static final String TAG = ReconTerrainGradeWidget.class.getSimpleName();
	
	public ReconTerrainGradeWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	public ReconTerrainGradeWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ReconTerrainGradeWidget(Context context) {
		super(context);
	}

	
	@Override
	public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
		Bundle tempBundle = (Bundle) fullinfo.get("GRADE_BUNDLE");
		float value = tempBundle.getFloat("TerrainGrade") * 100.0f;
		if(Float.compare(value, -10000f) > 0) {
			fieldTextView.setText(prefixZeroesForGrade(value, inActivity));
		} else {
			fieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
		}
		unitTextView.setText("%");
		fieldTextView.invalidate();
	}

	@Override
	public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
	}

    
    protected CharSequence prefixZeroesForGrade(float input, boolean inActivity){
        CharSequence output;
        if(Math.abs(input) < 0.5f) {
            output = Html.fromHtml(((inActivity) ? "0.0" : "<font color=\"#808181\">0.0</font>"));
        }else{
            DecimalFormat df = new DecimalFormat("0.0");
            output = Html.fromHtml(((inActivity) ? df.format(input) : "<font color=\"#808181\">" + df.format(input) + "</font>"));
        }
        return output;
    }
}
