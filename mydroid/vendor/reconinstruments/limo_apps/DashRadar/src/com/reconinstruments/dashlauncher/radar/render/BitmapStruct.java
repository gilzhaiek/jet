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

public class BitmapStruct
{
	public Bitmap mBitmap = null;
	public int mWidth = 0;
	public int mHeight = 0;
	public int mSide = 32;

	public BitmapStruct() {}
}
