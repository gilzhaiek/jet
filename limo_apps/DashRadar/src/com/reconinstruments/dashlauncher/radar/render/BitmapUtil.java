package com.reconinstruments.dashlauncher.radar.render;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Shader;
import android.util.Log;

public class BitmapUtil
{
	private static String TAG = "BitmapUtil";	
	private static boolean IsInitialized = false;
	
	private static Bitmap mGenSurface;
	private static Canvas mGenCanvas;
	private static Paint mTextPaint, mFillPaint, mBorderPaint, mGlyphPaint;

	public static final int TEXTURE_SIZE = 240;

	public static Rect getStringRect(String str)
	{
		Rect tBounds = new Rect();
		mTextPaint.getTextBounds(str, 0, str.length(), tBounds);
		return tBounds;
	}

	public static void InitializePaints()
	{
		IsInitialized = true;

		mTextPaint = new Paint();
		mTextPaint.setColor(0xffcdcdcd);
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTextSize(18.0f);
		mTextPaint.setTypeface(Typeface.SANS_SERIF);
		mTextPaint.setTextAlign(Align.CENTER);
		mTextPaint.setShadowLayer(1, 1, 1, Color.BLACK);

		mFillPaint = new Paint();
		mFillPaint.setColor(Color.BLACK);
		mFillPaint.setAlpha(255);
		mFillPaint.setAntiAlias(true);
		mFillPaint.setDither(true);

		mBorderPaint = new Paint();
		mBorderPaint.setColor(Color.WHITE);
		mBorderPaint.setStyle(Style.STROKE);
		mBorderPaint.setStrokeWidth(2.0f);
		mBorderPaint.setAlpha(255);
		mBorderPaint.setAntiAlias(true);
		mBorderPaint.setDither(true);
		
		mGlyphPaint = new Paint();
		mGlyphPaint.setColor(Color.WHITE);
		mGlyphPaint.setAntiAlias(true);
		mGlyphPaint.setTextSize(18.0f);
		mGlyphPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mGlyphPaint.setTextAlign(Align.CENTER);
		//mGlyphPaint.setShadowLayer(1, 1, 1, Color.BLACK);
	}

	public static Bitmap getIconBitmap(Bitmap icon)
	{
		if (!IsInitialized)
			InitializePaints();

		int width  = icon.getWidth()  + 6;
		int height = icon.getHeight() + 6;

		mGenSurface = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		mGenCanvas  = new Canvas(mGenSurface);

		mGenCanvas.drawBitmap(icon, 3, 3, null);

		return mGenSurface;
	}

	public static Bitmap getGlyph(String glyph, int fontSize)
	{
		if (!IsInitialized)
			InitializePaints();
		
		//Log.v(TAG, "getGlyph('" + glyph + "', " + fontSize + ")");
		
		Rect tBounds = new Rect();

		mGlyphPaint.setTextSize(fontSize);
		mGlyphPaint.getTextBounds(glyph, 0, 1, tBounds);

		boolean isThin = glyph.equals("i") || glyph.equals("l");

		int width = Math.max(4, (int) ((float) tBounds.width() * (isThin ? 2 : 1.2)));

		boolean isShort = glyph.equals("a") || glyph.equals("c") || glyph.equals("e") || glyph.equals("m") || glyph.equals("n") || glyph.equals("o")
					   || glyph.equals("r") || glyph.equals("s") || glyph.equals("u") || glyph.equals("v") || glyph.equals("x") || glyph.equals("z");

		int height = Math.max(4, (int) (((float) tBounds.height() * (isShort ? 1.3 : 1.1)) * 1.1f));
		int mid_w  = width / 2;

		mGenSurface = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		mGenCanvas = new Canvas(mGenSurface);

		mGenCanvas.drawText(glyph, mid_w, height - 2, mGlyphPaint);
		return mGenSurface;
	}
	
	public static BitmapStruct getAsciiChar(String glyph, int fontSize)
	{
		if (!IsInitialized)
			InitializePaints();
		
		Rect tBounds = new Rect();

		mGlyphPaint.setTextSize(fontSize);
		mGlyphPaint.getTextBounds(glyph, 0, 1, tBounds);
		
		float textWidth  = tBounds.width();
		float textHeight = tBounds.height();

		BitmapStruct bitmapStruct = new BitmapStruct();
		bitmapStruct.mWidth =  Math.max(4, tBounds.width() + 1);
		bitmapStruct.mHeight = fontSize + 6;

		bitmapStruct.mBitmap = Bitmap.createBitmap(bitmapStruct.mSide, bitmapStruct.mSide, Config.ARGB_8888);
		mGenCanvas = new Canvas(bitmapStruct.mBitmap);

		mGenCanvas.drawText(glyph, (bitmapStruct.mWidth - textWidth/2), bitmapStruct.mHeight - 6, mGlyphPaint);
		return bitmapStruct;
	}
}
