package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;

public class HideBreadcrumbView extends LinearLayout {

	public HideBreadcrumbView(Context context) {
		super(context);
		
		LinearLayout.LayoutParams hideParams = new LinearLayout.LayoutParams(428, 10);
		
		ImageView imgView = new ImageView(this.getContext()); 
		imgView.setLayoutParams(hideParams); 
		imgView.setImageResource(R.drawable.breadcrumb_dark);
		//imgView.setAlpha(INVISIBLE);
		imgView.setScaleType(ScaleType.FIT_XY);
		
	}

}
