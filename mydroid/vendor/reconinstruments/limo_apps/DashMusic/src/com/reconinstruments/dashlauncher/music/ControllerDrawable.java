package com.reconinstruments.dashlauncher.music;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;

public class ControllerDrawable extends Drawable {
	
	public final static float BASE_RADIUS = 70f;
	
	public void draw(Canvas canvas) {
		float centerX =  canvas.getClipBounds().centerX();// / 2;
		float centerY =  canvas.getClipBounds().centerY();// * 1.3f;//this.getIntrinsicHeight() * 0.65f;
		
		// Create glow
		Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		glowPaint.setStyle(Style.STROKE);
		glowPaint.setStrokeWidth(30f);
		glowPaint.setDither(true);
		glowPaint.setShader(new RadialGradient(centerX, centerY, BASE_RADIUS + 30f, (new int[] {Color.WHITE, 0x00FFFFFF}), (new float[] {0.3f, 0.8f}), Shader.TileMode.CLAMP));
		canvas.drawCircle(centerX, centerY, BASE_RADIUS, glowPaint);
		
		// Draw circle fills
		Paint circleGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circleGradientPaint.setStyle(Style.FILL);
		circleGradientPaint.setStrokeWidth((float) 2.0);
		circleGradientPaint.setDither(true);
		//0xffb3b3b3, 0xFF555555
		circleGradientPaint.setShader(new LinearGradient(centerX, centerY-70, centerX, centerY+50, 0xff9c90b0, 0xFF555555, Shader.TileMode.CLAMP));
		canvas.drawCircle(centerX, centerY, BASE_RADIUS - 10f, circleGradientPaint);
		circleGradientPaint.setShader(new LinearGradient(centerX, centerY-35, centerX, centerY+20, 0xFFFFFFFF, 0xFF555555, Shader.TileMode.CLAMP));
		canvas.drawCircle(centerX, centerY, BASE_RADIUS - 50f, circleGradientPaint);
		
		// Draw circle lines
		Paint blackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		blackPaint.setStyle(Style.STROKE);
		blackPaint.setStrokeWidth(1.0f);
		blackPaint.setColor(Color.BLACK);
		canvas.drawCircle(centerX, centerY, BASE_RADIUS - 50f, blackPaint);
		canvas.drawCircle(centerX, centerY, BASE_RADIUS - 10f, blackPaint);
		
		// Draw outer circle lines with gradient
		Paint ringShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		ringShadowPaint.setStyle(Style.STROKE);
		ringShadowPaint.setStrokeWidth(2f);
		ringShadowPaint.setShader(new LinearGradient(centerX, centerY-60, centerX, centerY+60, 0xFF000000, Color.WHITE, Shader.TileMode.CLAMP));
		canvas.drawCircle(centerX, centerY, BASE_RADIUS - 9f, ringShadowPaint);
		ringShadowPaint.setShader(new LinearGradient(centerX, centerY-20, centerX, centerY+20, 0xFF000000, Color.WHITE, Shader.TileMode.CLAMP));
		canvas.drawCircle(centerX, centerY, BASE_RADIUS - 49f, ringShadowPaint);
		
		blackPaint.setStyle(Style.FILL);
		canvas.drawPath(new Path(), blackPaint);
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(int alpha) {}

	@Override
	public void setColorFilter(ColorFilter cf) {}
}
