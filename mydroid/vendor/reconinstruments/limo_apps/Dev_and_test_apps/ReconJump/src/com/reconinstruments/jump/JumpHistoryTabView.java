package com.reconinstruments.jump;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reconinstruments.applauncher.transcend.ReconJump;
import com.reconinstruments.jump.tabs.ReconTabPage;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class JumpHistoryTabView extends TabView {

	ArrayList<TabPage> tabPages = new ArrayList<TabPage>(3);
	private Context mContext;
	
	public JumpHistoryTabView(Context context, ArrayList<Bundle> jumps) {
		super(context);
		mContext = context;
		createTabPage(jumps);
	}
	
	private void createTabPage(ArrayList<Bundle> jumps) {
		Collections.reverse(jumps);
		for(Bundle jump : jumps) {
			JumpTabPage jumpTP = new JumpTabPage(mContext, Integer.toString(jump.getInt("Number")), this, jump);
			tabPages.add(jumpTP);
		}
		
		this.setTabPages(tabPages);
		this.focusTabBar();
	}

	class JumpTabPage extends ReconTabPage {
		
		public JumpTabPage(Context context, String tabTxt, TabView hostView, Bundle jumpData) {
			super(context, tabTxt, hostView, "Jump #"+jumpData.getInt("Number"));
			setData(jumpData);
			
			this.lastJumpNumberTextView.setVisibility(View.GONE);
		}
	}
}
