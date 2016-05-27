package com.reconinstruments.dashlauncher.livestats;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class RadarView extends View {

	private static float STARTING_RADIUS = 83f;
	private static float RADIUS_INCREMENT = 10f;
	
	// default number of rings
	// TODO: get volume from HQMobile
	private int rings = 0;
	private int maxRing = 3;
	
	public RadarView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public RadarView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setRings(int speedRings) {
		rings = speedRings;
		invalidate(); // trigger view to redraw
	}
	
	public void setMaxRing(int maxRing) {
		this.maxRing = maxRing;
	}
	
	@Override
	public void onDraw(Canvas canvas) {

		float centerX =  this.getMeasuredWidth() / 2;
		float centerY =  this.getMeasuredHeight() * 0.7f;
		
		Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circlePaint.setStyle(Style.STROKE);
		circlePaint.setStrokeWidth((float) 2.0);
		circlePaint.setDither(true);
		circlePaint.setShader(new LinearGradient(centerX, centerY-60, centerX, centerY+60, 
				new int[] {0x00FFFFFF, Color.GRAY, 0x00FFFFFF}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
		
		Paint greenCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		greenCirclePaint.setStyle(Style.STROKE);
		greenCirclePaint.setStrokeWidth((float) 5.0);
		greenCirclePaint.setDither(true);
		greenCirclePaint.setShader(new LinearGradient(centerX, centerY-60, centerX, centerY+60, 
				new int[] {0x006ABD45, 0xFF6ABD45, 0x006ABD45}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
		
		for(int i=0; i<10; i++) {
			if(i == maxRing)
				canvas.drawCircle(centerX, centerY, STARTING_RADIUS + (i * RADIUS_INCREMENT), greenCirclePaint);
			else if(i < rings)
				canvas.drawCircle(centerX, centerY, STARTING_RADIUS + (i * RADIUS_INCREMENT), circlePaint);
		}
		
		// Draw dot markers
		Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		dotPaint.setStyle(Style.FILL);
		dotPaint.setColor(Color.GRAY);
		
		// draw dots
		for(int i=0; i<10; i++) {
			float posLeft = centerX + STARTING_RADIUS + (i * RADIUS_INCREMENT);
			float posRight = centerX - STARTING_RADIUS - (i * RADIUS_INCREMENT);
			if(i == 4 || i == 9) { // Make 5th and 10th larger
				canvas.drawCircle(posLeft, centerY, 2f, dotPaint);
				canvas.drawCircle(posRight, centerY, 2f, dotPaint);
			} else {
				canvas.drawCircle(posLeft, centerY, 1f, dotPaint);
				canvas.drawCircle(posRight, centerY, 1f, dotPaint);
			}
		}
	}

	
}
