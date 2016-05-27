package com.reconinstruments.breadcrumb;

import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.FrameLayout;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.view.Gravity;
import android.view.Menu;
import android.widget.Toast;

public class ReconBreadcrumbs {
    public static void showBreadcrumb(Context context, Toast breadcrumbToast, int totalX, int currentX, int totalY, int currentY) {
		int[] dashFragsX = new int[totalX];
		dashFragsX[0]=BreadcrumbView.OTHER_ICON;
		for(int i=1; i<dashFragsX.length; i++)
			dashFragsX[i] = BreadcrumbView.OTHER_ICON;

		int[] dashFragsY = new int[totalY];
		dashFragsY[0]=BreadcrumbView.OTHER_ICON;
		for(int i=1; i<dashFragsY.length; i++)
			dashFragsY[i] = BreadcrumbView.OTHER_ICON;


		BreadcrumbView mBreadcrumbViewHorz = new BreadcrumbView(context, 0, currentX, dashFragsX);
		mBreadcrumbViewHorz.invalidate();
		BreadcrumbView mBreadcrumbViewVert = new BreadcrumbView(context, 1, currentY, dashFragsY);
		mBreadcrumbViewVert.invalidate();
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		RelativeLayout overlayView = (RelativeLayout)inflater.inflate(R.layout.full_breadcrumb_layout, null);
		if (totalY > 1)
		    ((FrameLayout)overlayView.getChildAt(0)).addView(mBreadcrumbViewVert);
		if (totalX > 1)
		    ((FrameLayout)overlayView.getChildAt(1)).addView(mBreadcrumbViewHorz);
		
		// if(orientation == 0){
		// 	breadcrumbToast.setGravity(Gravity.BOTTOM, 0, 0);
		// }else{
		// 	breadcrumbToast.setGravity(Gravity.RIGHT, 0, 0);
		// }
		breadcrumbToast.setView(overlayView);
		breadcrumbToast.setMargin(0, 0);
		breadcrumbToast.show();

	}
}
