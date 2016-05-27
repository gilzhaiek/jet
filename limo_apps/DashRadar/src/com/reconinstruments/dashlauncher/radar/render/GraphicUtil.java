package com.reconinstruments.dashlauncher.radar.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;

public class GraphicUtil
{
	private static boolean isInitialized = false; 
	private static Bitmap genSurface;
	private static Canvas genCanvas;
	private static Paint textPaint, bFill, bBorder;

	public static final int TEXTURE_SIZE = 240;
	
	public static Rect getStringRect(String str)
	{
		Rect tBounds = new Rect();
		textPaint.getTextBounds(str, 0, str.length(), tBounds);
		return tBounds;
	}
	

	public static void initializeRenderPaints()
	{
		textPaint = new Paint();
		textPaint.setColor(0xffcdcdcd);
		textPaint.setAntiAlias(true);
		textPaint.setTextSize(18.0f);
		textPaint.setTypeface(Typeface.SANS_SERIF);
		textPaint.setTextAlign(Align.CENTER);
		textPaint.setShadowLayer(1, 1, 1, Color.BLACK);

		bFill = new Paint();
		bFill.setColor(Color.BLACK);
		bFill.setAlpha(255);
		bFill.setAntiAlias(true);
		bFill.setDither(true);

		bBorder = new Paint();
		bBorder.setColor(Color.WHITE);
		bBorder.setStyle(Style.STROKE);
		bBorder.setStrokeWidth(2.0f);
		bBorder.setAlpha(255);
		bBorder.setAntiAlias(true);
		bBorder.setDither(true);
		
		isInitialized = true;
	}
	
	public static Bitmap getIconBitmap(Bitmap icon, int color)
	{
		if(!isInitialized) initializeRenderPaints();
		
		int width = icon.getWidth() + 6;
		int height = icon.getHeight() + 10 + 20; // 20 pixels for the arrow graphic

		genSurface = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		genCanvas = new Canvas(genSurface);

		Path pa = new Path();
		pa.moveTo(0, 0);
		pa.lineTo(width, 0);
		pa.lineTo(width, width);
		pa.lineTo(width / 2, height);
		pa.lineTo(0, width);
		pa.lineTo(0, 0);

		bFill.setShader(new LinearGradient(0, height, 0, 0, 0xff191e20, 0xff636363, Shader.TileMode.CLAMP));
		bBorder.setShader(new LinearGradient(0, height, 0, 0, 0xff636363, Color.WHITE, Shader.TileMode.CLAMP));
		genCanvas.drawPath(pa, bFill);
		genCanvas.drawPath(pa, bBorder);

		genCanvas.drawBitmap(icon, 3, 3, null);

		return genSurface;
	}

	public static Bitmap getTextBubbleBitmap(String name)
	{
		if(!isInitialized) initializeRenderPaints();
		
		Rect tBounds = new Rect();
		textPaint.getTextBounds(name, 0, name.length(), tBounds);

		int width = Math.max(tBounds.width() + 20, TEXTURE_SIZE);
		int mid = width / 2;
		int offset = 30, offsetH = offset / 2;

		genSurface = Bitmap.createBitmap(width, width, Config.ARGB_8888);
		genCanvas = new Canvas(genSurface);

		int l = mid - tBounds.width() / 2 - 5;
		int t = (int) (mid - textPaint.getTextSize() / 2 - offset);
		int r = mid + tBounds.width() / 2 + 5;
		int b = mid - offsetH;

		float left = mid - 7.5f;
		float right = mid + 7.5f;
		bFill.setShader(new LinearGradient(l, b + offsetH, l, t, 0xff191e20, 0xff636363, Shader.TileMode.CLAMP));
		bBorder.setShader(new LinearGradient(l, b + offsetH, l, t, 0xff636363, Color.WHITE, Shader.TileMode.CLAMP));
		
		Path pa = new Path();
		pa.moveTo(l, t);
		pa.lineTo(r, t);
		pa.lineTo(r, b);
		pa.lineTo(right, b);
		pa.lineTo(mid, b + offsetH);
		pa.lineTo(left, b);
		pa.lineTo(l, b);
		pa.lineTo(l, t);

		genCanvas.drawPath(pa, bFill);
		genCanvas.drawPath(pa, bBorder);
		genCanvas.drawText(name, mid, b - textPaint.getTextSize() / 3, textPaint);
		return genSurface;
	}

	public static Bitmap getLngLatBitmap(String str)
	{
		if(!isInitialized) initializeRenderPaints();

		Rect tBounds = new Rect();
		textPaint.getTextBounds(str, 0, str.length(), tBounds);

		int width = tBounds.width() + 20;
		int height = tBounds.height() + 10;
		// int width = TEXTURE_SIZE;
		int mid_w = width / 2;

		genSurface = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		genCanvas = new Canvas(genSurface);

		genCanvas.drawText(str, mid_w, height - textPaint.getTextSize() / 2, textPaint);
		return genSurface;
	}

}
