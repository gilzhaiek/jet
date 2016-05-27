package com.reconinstruments.commonwidgets;

import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.widget.ListView;
import com.reconinstruments.utils.DeviceUtils;

public class TwoOptionsJumpFixer {
	private Handler mHandler = new Handler();
	private Runnable mRequestFocusTask;
	
	public TwoOptionsJumpFixer(final View firstItem, final View secondItem){
	    if (!DeviceUtils.isLimo()) {
		firstItem.setFocusable(false);
		secondItem.setFocusable(false);
		mRequestFocusTask = new Runnable() {
	        public void run() {
	        	firstItem.setFocusable(true);
	        	secondItem.setFocusable(true);
	        	secondItem.requestFocus();
	        }
	     };
	    }
	}
	
	public TwoOptionsJumpFixer(final ListView listView){
	    if (!DeviceUtils.isLimo()) {
		listView.setFocusable(false);
		mRequestFocusTask = new Runnable() {
	        public void run() {
	        	listView.setFocusable(true);
	    		if(listView.getCount() == 2){
	    			listView.setSelection(1);
	    		}
	        }
	     };
	    }
	}
	
	public void start(){
	    if (!DeviceUtils.isLimo()) {
		mHandler.removeCallbacks(mRequestFocusTask);
		mHandler.postDelayed(mRequestFocusTask, 550);
	    }
	}
	
	public void stop(){
	    if (!DeviceUtils.isLimo()) {
		mHandler.removeCallbacks(mRequestFocusTask);
	    }
	}
}
