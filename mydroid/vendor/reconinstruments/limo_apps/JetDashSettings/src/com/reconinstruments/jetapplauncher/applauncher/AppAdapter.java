package com.reconinstruments.jetapplauncher.applauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.reconinstruments.jetapplauncher.R;
import com.reconinstruments.commonwidgets.CustomTextView;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AppAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<ApplicationInfo> mApplications = new ArrayList<ApplicationInfo>();
    
    public AppAdapter(Context c) {
        mContext = c;
        
        PackageManager manager = c.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
    	mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
    	Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));
    	
    	for(ResolveInfo resolveInfo : apps) {
    		ApplicationInfo application = new ApplicationInfo();
    		
    		application.title = resolveInfo.loadLabel(manager);
    		
    		application.setActivity(new ComponentName(
    				resolveInfo.activityInfo.applicationInfo.packageName,
    				resolveInfo.activityInfo.name),
			Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    		application.icon = resolveInfo.activityInfo.loadIcon(manager);

    		mApplications.add(application);
    	}
    }

    public int getCount() {
        return mApplications.size();
    }

    public Object getItem(int position) {
        return mApplications.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
    	  View row = convertView;
    	  if (row == null) {
	    	   LayoutInflater inflater = ((Activity) this.mContext).getLayoutInflater();
	    	   row = inflater.inflate(R.layout.app_carousel_item, parent, false);
	    	  ImageView iconView = (ImageView) row.findViewById(R.id.item_image);
	    	  CustomTextView tv = (CustomTextView) row.findViewById(R.id.item_text);
	    	  tv.setText(mApplications.get(position).title);
	    	  iconView.setImageDrawable(mApplications.get(position).icon);
//	    	  iconView.setAlpha(1.0f);
    	  }
    	  return row;
    	  
    	
//    	LinearLayout linLayout = new LinearLayout(this.mContext);
//    	linLayout.setOrientation(LinearLayout.VERTICAL);
//    	linLayout.setGravity(Gravity.CENTER);
//    	linLayout.setLayoutParams(new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//    	
//    	ImageView iconView = new ImageView(mContext);
//    	iconView.setImageDrawable(mApplications.get(position).icon);
//    	iconView.setScaleType(ScaleType.CENTER_INSIDE);
//    	iconView.setPadding(0, 11, 0, 0);
//    	iconView.setLayoutParams(new GridView.LayoutParams(60, 60));
//    	
//    	CustomTextView tv = new CustomTextView(mContext);
//    	tv.setText(mApplications.get(position).title);
//    	tv.setTextSize(15);
//    	tv.setGravity(Gravity.CENTER_HORIZONTAL);
//    	tv.setTextAppearance(mContext, R.style.recon_selectable_text_shadow);
//    	tv.setTypeface(Typeface.DEFAULT_BOLD);
//    	tv.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//    	tv.setSingleLine(true);
//    	tv.setPadding(8, 0, 8, 11);
//    	linLayout.addView(iconView);
//    	linLayout.addView(tv);
    	
//	    return linLayout;
    }
}
