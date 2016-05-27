package com.reconinstruments.breadcrumb;

import android.content.Context;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class BreadcrumbView extends LinearLayout {

	private static final String TAG = "BreadcrumbView";

	public static final int APP_ICON = 0;
	public static final int DASH_ICON = 1;
	public static final int OTHER_ICON = 2;
	
	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;
	
	int pos;
	
	public BreadcrumbView(Context context, int orientation, int position, int[] icons) {
		super(context);
		
		pos = position;
		if(orientation > 1)
			orientation = 1;
		if(orientation < 0)
			orientation = 0;
		this.setOrientation(orientation);
		if (orientation == HORIZONTAL)
		    this.setPadding(7, 4, 7, 4);
		else
		    this.setPadding(4, 7, 4, 7);
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
				imgView.setImageResource((position == i) ? R.drawable.breadcrumb_square_on : R.drawable.breadcrumb_square_off);
				break;
			}
			
			if(imgView != null) {
			    if (orientation == HORIZONTAL)
				imgView.setPadding(6, 1, 6, 1);
			    else
				imgView.setPadding(1, 6, 1, 6);
				
			    this.addView(imgView);
			}
			
		}
	}
}
