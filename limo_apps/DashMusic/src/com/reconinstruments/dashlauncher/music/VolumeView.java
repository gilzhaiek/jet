package com.reconinstruments.dashlauncher.music;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

public class VolumeView extends View {

	private static float STARTING_RADIUS = 83f;
	private static float RADIUS_INCREMENT = 10f;

	float centerX;
	float centerY;
	Paint circlePaint;
	Paint dotPaint;

	MusicControllerActivity musicController;

	public VolumeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public VolumeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	//initialise for drawing here
	public void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec,heightMeasureSpec); // Important !!!
		
		//final int width = getMeasuredHeight();
		//final int height = getMeasuredHeight();
		// do you background stuff
		centerX =  getMeasuredWidth() / 2;
		centerY =  getMeasuredHeight() /2;

		circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circlePaint.setStyle(Style.STROKE);
		circlePaint.setStrokeWidth((float) 3.0);
		circlePaint.setDither(true);
		circlePaint.setShader(new LinearGradient(centerX, centerY-80, centerX, centerY+70, new int[] {0x00FFFFFF, Color.GRAY, 0x00FFFFFF}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));

		// Draw dot markers
		dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		dotPaint.setStyle(Style.FILL);
		dotPaint.setColor(Color.GRAY);
	}

	@Override
	public void onDraw(Canvas canvas) {
		
		for(int i=0; i<MusicHelper.getVolumeInt(); i++) {
			canvas.drawCircle(centerX, centerY, STARTING_RADIUS + (i * RADIUS_INCREMENT), circlePaint);
		}

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
