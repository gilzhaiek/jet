package com.reconinstruments.commonwidgets;

import java.util.ArrayList;

import android.content.Context;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

public class BreadcrumbView extends LinearLayout {

	private static final String TAG = "BreadcrumbView";

	public static final int APP_ICON = 0;
	public static final int DASH_ICON = 1;
	public static final int OTHER_ICON = 2;
	public static final int DYNAMIC_ICON = 3;
    public static final int BREADCRUMB_THICKNESS = 7;
    
    private static final int GLOBLE_BREADCRUMB_WIDTH = 428;
    private static final int DASHBOARD_BREADCRUMB_WIDTH = 6;
    private static final int DASHBOARD_BREADCRUMB_HEIGHT = 82;
    private static final int DASHBOARD_BREADCRUMB_FULL_HEIGHT = 210;
    
	int SCREEN_WIDTH = 428; 
	int pos;
	int prevPos;
	ArrayList<ImageView> list= new ArrayList<ImageView>(); 
	ImageView dynamicLight, dynamicDark;
	boolean mFullHeight = false;
	
	public BreadcrumbView(Context context, boolean orientationHorizontal, boolean fullHeight, int position, int[] icons) {
	    super(context);
	    mFullHeight = fullHeight;
	    buildBreadcrumbView(context, orientationHorizontal, position, icons);
	}
	
	public BreadcrumbView(Context context, boolean orientationHorizontal, int position, int[] icons) {
		super(context);
		buildBreadcrumbView(context, orientationHorizontal, position, icons);
	}
	
	private void buildBreadcrumbView(Context context, boolean orientationHorizontal, int position, int[] icons) {
        pos = position;
        
        this.setOrientation(orientationHorizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        if(orientationHorizontal){
            this.setPadding(4, 0, 4, 4);
        }
        this.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams dynamicParams = new LinearLayout.LayoutParams((GLOBLE_BREADCRUMB_WIDTH/(icons.length)),
                                            BREADCRUMB_THICKNESS);
        if(!orientationHorizontal){
            if(mFullHeight){
                dynamicParams = new LinearLayout.LayoutParams(DASHBOARD_BREADCRUMB_WIDTH, (DASHBOARD_BREADCRUMB_FULL_HEIGHT/(icons.length)));
            }else{
                dynamicParams = new LinearLayout.LayoutParams(DASHBOARD_BREADCRUMB_WIDTH, (DASHBOARD_BREADCRUMB_HEIGHT/(icons.length)));
            }
        }
        
        for(int i=0; i<icons.length; i++) {
            ImageView imgView = null;
            
            switch(icons[i]) {
            case APP_ICON:
                imgView = new ImageView(context);
                imgView.setImageResource((position == i) ? R.drawable.breadcrumb_apps_white : R.drawable.breadcrumb_apps_grey);
                list.add(imgView); 
                break;
                
            case DASH_ICON:
                imgView = new ImageView(this.getContext());
                imgView.setImageResource((position == i) ? R.drawable.breadcrumb_dash_white : R.drawable.breadcrumb_dash_grey);
                list.add(imgView);
                break;
                
            case OTHER_ICON:
                imgView = new ImageView(this.getContext());
                imgView.setImageResource((position == i) ? R.drawable.breadcrumb_other_white : R.drawable.breadcrumb_other_grey);
                list.add(imgView);
                break;
            
            case DYNAMIC_ICON: 
                imgView = new ImageView(this.getContext()); 
                imgView.setLayoutParams(dynamicParams);
                imgView.setImageResource((position == i) ? R.drawable.breadcrumb_other_white : R.drawable.breadcrumb_dark);
                imgView.setScaleType(ScaleType.FIT_XY);
                if(orientationHorizontal){
                    imgView.setPadding(4, 0, 4, 4);
                }else{
                    imgView.setPadding(0, 2, 0, 2);
                }
                list.add(imgView);
                break; 

            }
            if(list.get(i) != null) {
                if(orientationHorizontal){
                    this.setPadding(4, 0, 4, 4);
                }
            }
            
        }
        
        this.drawBreadcrumbs();
	}
	
	public void redrawBreadcrumbs(Context context, int position){
		for(int i=0; i<list.size(); i++){
			if(i == position){
				list.get(i).setImageResource(R.drawable.breadcrumb_other_white); 
			} else {
				list.get(i).setImageResource(R.drawable.breadcrumb_dark);	
			}
		}
		
	}
	
	public void drawBreadcrumbs(){
		for(int i=0; i<list.size(); i++){
			if(list.get(i) != null){
				this.addView(list.get(i));
			}
		}
	}
}
