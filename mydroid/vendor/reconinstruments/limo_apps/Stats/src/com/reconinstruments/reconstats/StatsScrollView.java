package com.reconinstruments.reconstats;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class StatsScrollView extends ScrollView {

	public StatsScrollView(Context context) {
		super(context);
	}
	
	public StatsScrollView(Context context, AttributeSet attr) {
		super(context, attr);
	}
	
	public void scrollToElement(View element) {
		Rect r = new Rect();

		element.getLocalVisibleRect(r);
    	int offset = this.computeScrollDeltaToGetChildRectOnScreen(r);
    	scrollTo(0, offset);
    	invalidate();
	}
}
