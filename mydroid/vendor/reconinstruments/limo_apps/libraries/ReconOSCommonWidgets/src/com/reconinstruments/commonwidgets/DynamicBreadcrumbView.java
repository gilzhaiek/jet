package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;

public class DynamicBreadcrumbView extends LinearLayout {
	
	int SCREEN_WIDTH = 428; 
	int pos;
	int prevPos;
	
	public DynamicBreadcrumbView(Context context, int position, int[] icons){
		super(context);
		
		pos = position;
		
		this.setOrientation(LinearLayout.HORIZONTAL);
		this.setPadding(4, 0, 4, 4);
		this.setGravity(Gravity.CENTER);
		
		LinearLayout.LayoutParams dynamicParams = new LinearLayout.LayoutParams((428/icons.length), 10);
		
		for(int i=0; i<icons.length; i++) {
			ImageView imgView = null;
	
			if( i == pos ){
				imgView = new ImageView(this.getContext()); 
				imgView.setLayoutParams(dynamicParams);
				imgView.setImageResource(R.drawable.breadcrumb_other_white);
				imgView.setScaleType(ScaleType.FIT_XY);
			} 
			
			if( i == prevPos ){
				imgView = new ImageView(this.getContext());
				imgView.setLayoutParams(dynamicParams);
				imgView.setImageResource(R.drawable.breadcrumb_dark);
				imgView.setScaleType(ScaleType.FIT_XY);
			}
			
			if(imgView != null) {
				imgView.setPadding(4, 0, 4, 4);
				
				this.addView(imgView);
			}
			
		}
		
		prevPos = position;
	}
}
