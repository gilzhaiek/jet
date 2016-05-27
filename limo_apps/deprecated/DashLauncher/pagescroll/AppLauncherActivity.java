package com.reconinstruments.dashlauncher.applauncher.pagescroll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.reconinstruments.dashlauncher.BoardActivity;
import com.reconinstruments.dashlauncher.R;
import com.reconinstruments.dashlauncher.applauncher.ApplicationInfo;

public class AppLauncherActivity extends ListActivity {
	public static final String TAG = "AppLauncherActivity";
	
	private GridListAdapter mAppAdapter;
	
	private ArrayList<ApplicationInfo> mApplications = new ArrayList<ApplicationInfo>();

	private Toast mToast;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.app_grid_layout);

	    // get applications list
	    PackageManager manager = getPackageManager();
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
    	
	    //final ListView gridview = (ListView) findViewById(R.id.listview);
    	//mAppAdapter = new GridListAdapter(this);
	    //gridview.setAdapter(mAppAdapter);
	    this.setListAdapter(new GridListAdapter(this));
	}

	public ArrayList<ApplicationInfo> getApps(){
		return mApplications;
	}
	public void onPause() {
		super.onPause();
		if(mToast != null) mToast.cancel();
	}
}
