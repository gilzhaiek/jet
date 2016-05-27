package com.reconinstruments.dashlauncher.applauncher.pagescroll;

import java.util.ArrayList;

import com.reconinstruments.dashlauncher.applauncher.ApplicationInfo;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class AppGridAdapter extends BaseAdapter {
    private AppLauncherActivity context;
    private int offset;
    
    public AppGridAdapter(AppLauncherActivity context,int offset) {
    	this.context = context;
    	this.offset = offset;
    }

    public int getCount() {
        return 6;
    }
    public ApplicationInfo getItem(int position) {
        return context.getApps().get(offset*6+position);
    }
    public long getItemId(int position) {
        return 0;
    }
	public ArrayList<ApplicationInfo> getApps(){
		return context.getApps();
	}
	public ApplicationInfo getApp(int position){
		return context.getApps().get(offset*6+position);
	}
    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
    	
    	TextView tv = new TextView(context);
    	tv.setText(""+offset*6+position);
    	tv.setLayoutParams(new GridView.LayoutParams(125, 95));
    	
    	return tv;
    	/*
    	ImageView i = new ImageView(context);

    	//i.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.apps_selector));
	    
    	i.setImageDrawable(getItem(position).icon);
	    i.setLayoutParams(new GridView.LayoutParams(125, 95));
	    i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
	    //i.setBackgroundDrawable(mApplications.get(position).icon);
	    
	    return i;*/
    }
}