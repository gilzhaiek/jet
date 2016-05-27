package com.reconinstruments.jetapplauncher.applauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class MyLinearLayout extends LinearLayout {

	private float scale = 1.0f;
	
	public MyLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MyLinearLayout(Context context) {
		super(context);
	}
	
	public void setScaleWidthHeight(float scale){
		this.scale = scale;
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		int w = this.getWidth();
		int h = this.getHeight();
		canvas.scale(scale, scale, w/2, h/2);

		super.onDraw(canvas);
	}
	
	public void setLayoutScale(float scale){
		this.scale = scale;
	}
	
	public float getLayoutScale(){
		return this.scale;
	}
}
