package com.reconinstruments.dashlauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow.LayoutParams;

public class BreadcrumbView extends LinearLayout {

	private static final String TAG = "BreadcrumbView";

	public static final int APP_ICON = 0;
	public static final int DASH_ICON = 1;
	public static final int OTHER_ICON = 2;
	
	int pos;
	
	public BreadcrumbView(Context context, boolean orientationHorizontal, int position, int[] icons) {
		super(context);
		
		pos = position;
		
		this.setOrientation(orientationHorizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
		this.setPadding(7, 4, 7, 4);
		this.setGravity(Gravity.CENTER);
		
		for(int i=0; i<icons.length; i++) {
			ImageView imgView = null;
			
			switch(icons[i]) {
			case APP_ICON:
				imgView = new ImageView(context);
				imgView.setImageResource((position == i) ? R.drawable.breadcrumb_apps_white : R.drawable.breadcrumb_apps_grey);
				break;
				
			case DASH_ICON:
				imgView = new ImageView(this.getContext());
				imgView.setImageResource((position == i) ? R.drawable.breadcrumb_dash_white : R.drawable.breadcrumb_dash_grey);
				break;
				
			case OTHER_ICON:
				imgView = new ImageView(this.getContext());
				imgView.setImageResource((position == i) ? R.drawable.breadcrumb_other_white : R.drawable.breadcrumb_other_grey);
				break;
			}
			
			if(imgView != null) {
				imgView.setPadding(6, 4, 6, 4);
				
				this.addView(imgView);
			}
			
		}
	}
}
