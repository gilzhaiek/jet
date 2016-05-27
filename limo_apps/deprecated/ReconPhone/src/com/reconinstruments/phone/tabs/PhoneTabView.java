package com.reconinstruments.phone.tabs;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import com.reconinstruments.phone.R;
import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class PhoneTabView extends TabView {

	ArrayList<TabPage> tabPages = new ArrayList<TabPage>(6);
	Context mContext;
	//PhoneCache mPhoneCache;
	int maxListSize = 10;
	
	private CallTabPage mCallTabPage;
	private SMSTabPage mSMSTabPage;

	public PhoneTabView(Context context) {
		super(context);
		createTabPages();
		mContext = context;
	}
	
	public void setCallHistory(ArrayList<Bundle> calls) {
		for(TabPage t : tabPages) {
			if(t instanceof CallTabPage) ((CallTabPage) t).setData(calls);
		}
	}
	
	public void setSMSHistory(ArrayList<Bundle> sms) {
		for(TabPage t : tabPages) {
			if(t instanceof SMSTabPage) ((SMSTabPage) t).setData(sms);
		}
	}

	private void createTabPages() {
		Resources r = getContext().getResources();
		
		Drawable callGrey = r.getDrawable(R.drawable.call_icon_grey);
		Drawable callBlue = r.getDrawable(R.drawable.call_icon_blue);
		Drawable callWhite = r.getDrawable(R.drawable.call_icon_white);
		
		Drawable smsBlue = r.getDrawable(R.drawable.sms_icon_blue);
		Drawable smsGrey = r.getDrawable(R.drawable.sms_icon_grey);
		Drawable smsWhite = r.getDrawable(R.drawable.sms_icon_white);
		
		mCallTabPage = new CallTabPage(getContext(), callGrey, callBlue, callWhite, this);
		tabPages.add(mCallTabPage);
		
		mSMSTabPage = new SMSTabPage(getContext(), smsGrey, smsBlue, smsWhite, this);
		tabPages.add(mSMSTabPage);

		this.setTabPages(tabPages);
		this.focusTabBar();
	}
	
	public boolean onSelectUp(View srcView) {
		return tabPages.get(mSelectedTabIdx).onSelectUp(srcView);
	}
	
	public boolean onRightArrowUp(View srcView) {
		return tabPages.get(mSelectedTabIdx).onSelectUp(srcView);
	}
	
	public void setPhoneConnectionState(boolean connected) {
		mCallTabPage.setPhoneConnectionState(connected);
	}
}
