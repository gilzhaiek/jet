package com.reconinstruments.phone.tabs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ListView;

import com.reconinstruments.widgets.TabPage;
import com.reconinstruments.widgets.TabView;

public class PhoneTabPage extends TabPage {

	ListView mListView;

	public PhoneTabPage(Context context, Drawable iconRegular, Drawable iconSelected, Drawable iconFocused, TabView hostView, ListView lv) {
		super(context, iconRegular, iconSelected, iconFocused, hostView);
		mListView = lv;
		mListView.setFocusable(true);
		this.addView(lv);
		this.setFocusable(true);
	}

	@Override
	public void setFocus() {
		super.setFocus();
		mListView.requestFocus();
	}
	
}
