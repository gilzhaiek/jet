package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.reconinstruments.dashlivestats.R;


public class ReconVrtWidget3x2 extends ReconVrtWidget {

	public ReconVrtWidget3x2(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		addAndPrepareViews(context);
	}

	public ReconVrtWidget3x2(Context context, AttributeSet attrs) {
		super(context, attrs);
		addAndPrepareViews(context);
	}

	public ReconVrtWidget3x2(Context context) {
		super(context);
		addAndPrepareViews(context);
	}
    
    private void addAndPrepareViews(Context context){
        LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_vertical_3x2, null);
        this.addView(myView);
        
        prepareInsideViews();
    }
}
