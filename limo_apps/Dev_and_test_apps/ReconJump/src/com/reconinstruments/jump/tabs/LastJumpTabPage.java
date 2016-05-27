package com.reconinstruments.jump.tabs;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reconinstruments.applauncher.transcend.ReconJump;
import com.reconinstruments.jump.FontSingleton;
import com.reconinstruments.jump.JumpHistoryActivity;
import com.reconinstruments.jump.R;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class LastJumpTabPage extends ReconTabPage {
	
	private Context mContext;
	private ArrayList<Bundle> jumpsList;
	
	public LastJumpTabPage(Context context, Drawable iconRegular,
			Drawable iconSelected, Drawable iconFocused, TabView hostView) {
		super(context, iconRegular, iconSelected, iconFocused, hostView, "LAST JUMP");
		
		findViewById(R.id.jump_time).setVisibility(View.GONE);
		mContext = context;
	}
	
	/**
	 * Hide run history arrow
	 */
	public void hideArrow() {
		View arrow = findViewById(R.id.arrow);
		arrow.setVisibility(View.GONE);
	}
	
	/**
	 * Show run history arrow
	 */
	public void showArrow() {
		View arrow = findViewById(R.id.arrow);
		arrow.setVisibility(View.VISIBLE);
	}
	
	
	private void launchHistoryActivity() {
		Intent i = new Intent(mContext, JumpHistoryActivity.class);
		i.putParcelableArrayListExtra("Jumps", jumpsList);
		mContext.startActivity(i);
	}
	
	public void setData(Bundle data, ArrayList<Bundle> jumpBundleList) {
		super.setData(data);
		jumpsList = jumpBundleList;
	}
	
	public boolean onSelectUp(View srcView) {
		launchHistoryActivity();
		return true;
	}
	
	public boolean onRightArrowUp(View srcView) {
		launchHistoryActivity();
		return true;
	}
}
