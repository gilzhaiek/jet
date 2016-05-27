package com.reconinstruments.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class TabListView extends ListView {

	View selectedView;
	
	public TabListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public TabListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public TabListView(Context context) {
		super(context);
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,	Rect previouslyFocusedRect) {
		if(!gainFocus) { // losing focus
			selectedView = this.getSelectedView();
		} else { // gaining focus
			if(selectedView != null) selectedView.requestFocus();
		}
		this.invalidateViews();
	}
}