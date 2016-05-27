package com.reconinstruments.dashboard.layouts;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.reconinstruments.dashboard.hashmaps.ReconDashboardHashmap;
import com.reconinstruments.dashboard.widgets.ReconChronoWidget4x1;
import com.reconinstruments.dashboard.widgets.ReconDashboardWidget;
import com.reconinstruments.dashboard.widgets.ReconWidgetHolder;

public class ReconDashboardLayout {
	public ReconDashboardHashmap dashboardhash;
	public View mTheBigView;
	public ArrayList<ReconDashboardWidget> mAllWidgets;
	public String id;
	
	public ReconDashboardLayout(String layout_id, Context c){
		dashboardhash = new ReconDashboardHashmap();
		id = layout_id;
		mAllWidgets = new ArrayList<ReconDashboardWidget>();
		//Let's inflate the baby:
		LayoutInflater inflater = (LayoutInflater)c.getSystemService
	      (Context.LAYOUT_INFLATER_SERVICE);
        mTheBigView = inflater.inflate(dashboardhash.LayoutHashMap.get(layout_id), null);
	}
	
	public void populate(){
		//Goes through all
		for (int i=0; i< mAllWidgets.size(); i++){
			((ReconWidgetHolder) mTheBigView.findViewById(dashboardhash.PlaceholderMap.get(i))).addView(mAllWidgets.get(i));
			
		}
	}
	
	public void updateInfo(Bundle fullInfo){
		for (int i=0; i< mAllWidgets.size(); i++){
			mAllWidgets.get(i).updateInfo(fullInfo);
		}
	}
	
	public boolean hasChrono() {
		for(ReconDashboardWidget r : mAllWidgets) {
			if(r instanceof ReconChronoWidget4x1) {
				return true;
			}
		}
		return false;
	}
}