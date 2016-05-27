package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class ToastViewPager extends ViewPager {
	
	public boolean toastPresent = false; 
	private Toast breadcrumbToast;
    private BreadcrumbView mBreadcrumbView;
	
	public ToastViewPager(Context context) {
		super(context);
	}
	
	public ToastViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	    
	@Override
	protected void onDraw (Canvas canvas){
		Log.v("Tester", "draw canvas"); 
		if( !toastPresent ){
			showHorizontalBreadcrumb(getContext(), this.getAdapter().getCount(), this.getCurrentItem());
			toastPresent = true;  
			this.postDelayed(hide, 2100);
		}
		super.onDraw(canvas);
	}
	
	protected void onDestroy(Context context){
		Log.v("ToastView", "On Pause"); 
	}
	
    public void showHorizontalBreadcrumb(Context context, int size, int currentPosition){
    	int[] dashFrags = new int[size];
        dashFrags[0]=BreadcrumbView.DYNAMIC_ICON;
        for(int i=1; i<dashFrags.length; i++)
            dashFrags[i] = BreadcrumbView.DYNAMIC_ICON;

        mBreadcrumbView = new BreadcrumbView(context, true, currentPosition, dashFrags);
        mBreadcrumbView.invalidate();
        
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
//		RelativeLayout overlayView = (RelativeLayout)inflater.inflate(R.layout.dynamic_breadcrumb_layout, null);
//		((FrameLayout)overlayView.getChildAt(0)).addView(mBreadcrumbView);

        if(breadcrumbToast == null) 
            breadcrumbToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);

        breadcrumbToast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
        breadcrumbToast.setView(mBreadcrumbView);
        breadcrumbToast.show();
    }
    
    Runnable hide = new Runnable(){
    	public void run(){
    		breadcrumbToast.cancel(); 
    		toastPresent = false; 
    	}
    };
    
}
