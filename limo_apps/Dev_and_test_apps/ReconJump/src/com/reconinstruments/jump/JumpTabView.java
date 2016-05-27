package com.reconinstruments.jump;

import java.util.ArrayList;

import com.reconinstruments.jump.tabs.BestAirTabPage;
import com.reconinstruments.jump.tabs.BestDistanceTabPage;
import com.reconinstruments.jump.tabs.BestDropTabPage;
import com.reconinstruments.jump.tabs.BestHeightTabPage;
import com.reconinstruments.jump.tabs.LastJumpTabPage;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class JumpTabView extends TabView {
	ArrayList<TabPage> tabPages = new ArrayList<TabPage>(3);

	TabPage mLastJumpTP, mBestAirTP, mBestDistanceTP, mBestHeightTP, mBestDropTP;
	Context mContext;
	ArrayList<Bundle> jumpsList = null;
	
	public JumpTabView(Context context) {
		super(context);
		mContext = context;
		createTabPages();
	}

	private void createTabPages() {
		Context context = this.getContext();
		Resources r = context.getResources();
		
		// Last Jump Tab
		Drawable lastJumpIconReg = r.getDrawable(R.drawable.jump_tab_lastjump_grey);
		Drawable lastJumpIconIconSelected = r.getDrawable(R.drawable.jump_tab_lastjump_white);
		mLastJumpTP = new LastJumpTabPage(context, lastJumpIconReg, lastJumpIconIconSelected, lastJumpIconIconSelected, this);
		tabPages.add(mLastJumpTP);

		// Best Air Tab
		Drawable airIconReg = r.getDrawable(R.drawable.jump_tab_airtime_grey);
		Drawable airIconSelected = r.getDrawable(R.drawable.jump_tab_airtime_white);
		mBestAirTP = new BestAirTabPage(context, airIconReg, airIconSelected, airIconSelected, this);
		tabPages.add(mBestAirTP);
		
		// Best Distance Tab
		Drawable distIconReg = r.getDrawable(R.drawable.jump_tab_distance_grey);
		Drawable distIconSelected = r.getDrawable(R.drawable.jump_tab_distance_white);
		mBestDistanceTP = new BestDistanceTabPage(context, distIconReg, distIconSelected, distIconSelected, this);
		tabPages.add(mBestDistanceTP);
		
		// Best Height Tab
		Drawable heightIconReg = r.getDrawable(R.drawable.jump_tab_height_grey);
		Drawable heightIconSelected = r.getDrawable(R.drawable.jump_tab_height_white);
		mBestHeightTP = new BestHeightTabPage(context, heightIconReg, heightIconSelected, heightIconSelected, this);
		tabPages.add(mBestHeightTP);
		
		// Best Drop Tab
		Drawable dropIconReg = r.getDrawable(R.drawable.jump_tab_drop_grey);
		Drawable dropIconSelected = r.getDrawable(R.drawable.jump_tab_drop_white);
		mBestDropTP = new BestDropTabPage(context, dropIconReg, dropIconSelected, dropIconSelected, this);
		tabPages.add(mBestDropTP);
		
		// Other init stuff
		this.setTabPages(tabPages);
		this.focusTabBar();
	}
	
	public void setViewData(Bundle data) {
		if(data == null) return;
		
		Bundle jumpBundle = data.getBundle("JUMP_BUNDLE");

		if(jumpBundle == null) {
			Log.d("JumpTabView", "JUMP_BUNDLE is null");
			return;
		}
		
		jumpsList = jumpBundle.getParcelableArrayList("Jumps");
		
		if(jumpsList == null) {
			Log.d("JumpTabView", "jumpsList is null");
			((LastJumpTabPage) mLastJumpTP).hideArrow();
			return;
		}
		
		if(jumpsList.size() > 1) {
			((LastJumpTabPage) mLastJumpTP).showArrow();
		} else {
			((LastJumpTabPage) mLastJumpTP).hideArrow();
		}
		
		Bundle bestAirBundle = null;
		Bundle bestDistanceBundle = null;
		Bundle bestDropBundle = null;
		Bundle bestHeightBundle = null;
		Bundle lastJumpBundle = null;
		
		// Iterate through the list of jumps and find the last jump and best jumps
		int i = 1;
		for(Bundle jump : jumpsList) {
			//Log.d("JumpTabView", Integer.toString(jump.getInt("Air")));
			
			if(bestAirBundle == null || bestAirBundle.getInt("Air") < jump.getInt("Air")) {
				bestAirBundle = jump;
			}
			
			if(bestDistanceBundle == null || bestDistanceBundle.getFloat("Distance") < jump.getFloat("Distance")) {
				bestDistanceBundle = jump;
			}
			
			if(bestDropBundle == null || bestDropBundle.getFloat("Drop") < jump.getFloat("Drop")) {
				bestDropBundle = jump;
			}
			
			if(bestHeightBundle == null || bestHeightBundle.getFloat("Height") < jump.getFloat("Height")) {
				bestHeightBundle = jump;
			}
			
			if(lastJumpBundle == null || lastJumpBundle.getInt("Number") < jump.getInt("Number")) {
				lastJumpBundle = jump;
			}
			
			i++;
		}
		
		if(lastJumpBundle != null) ((LastJumpTabPage)mLastJumpTP).setData(lastJumpBundle, jumpsList);
		if(bestAirBundle != null) ((BestAirTabPage)mBestAirTP).setData(bestAirBundle);
		if(bestDistanceBundle != null) ((BestDistanceTabPage)mBestDistanceTP).setData(bestDistanceBundle);
		if(bestDropBundle != null) ((BestDropTabPage)mBestDropTP).setData(bestDropBundle);
		if(bestHeightBundle != null) ((BestHeightTabPage)mBestHeightTP).setData(bestHeightBundle);
	}
	
	public boolean onSelectUp(View srcView) {
		return mPages.get(mSelectedTabIdx).onSelectUp(srcView);
	}
	
	public boolean onRightArrowUp(View srcView) {
		return mPages.get(mSelectedTabIdx).onRightArrowUp(srcView);
	}
	
	public boolean onLeftArrowUp(View srcView) {
        return true;
	}
}
