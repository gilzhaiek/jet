/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.views;

/**
 * This view for rendering the zoom scale, which will be used on the MapZoomView
 */

import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ZoomView extends View
{
	private float mZoomLevel;
	private float mMinZoomLevel = 0;
	private float mMaxZoomLevel;
	private Bitmap mZoomInIcon;
	private Bitmap mZoomOutIcon;
	private Bitmap mZoomIndicatorIcon;
	private Paint mPaint;
	
	static final int LEVEL_WIDTH = 30;
	
	public ZoomView(Context context ) 
	{
		super(context);
		InitView(  context );
	}

	public ZoomView(Context context, AttributeSet attrs ) 
	{
		super(context, attrs);		
		InitView(  context );
	}
	
	public void setZoomRange( float minLevel, float maxLevel , float zoomLevel )
	{
		mMinZoomLevel = minLevel;
		mMaxZoomLevel = maxLevel;
		mZoomLevel = zoomLevel;
	}
	
	private void InitView( Context context )
	{		
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(0xffffffff);

		
		try
		{
			InputStream stream = context.getAssets().open( "icons/" + "zoomIn.png");
			mZoomInIcon = BitmapFactory.decodeStream(stream);
			stream.close();
			
			stream = context.getAssets().open( "icons/" + "zoomOut.png");
			mZoomOutIcon = BitmapFactory.decodeStream(stream);
			stream.close();
			
			stream = context.getAssets().open( "icons/" + "zoomIndicator.png");
			mZoomIndicatorIcon = BitmapFactory.decodeStream(stream);
			stream.close();

		} 
		catch (Exception e) 
		{
			e.printStackTrace(System.out);
			Log.e("ZoomView", "ZoomView - Loading zoom icons failed", e);
		}
	}
	
	public void setZoomLevel( float zoomLevel )
	{
		mZoomLevel = zoomLevel < mMinZoomLevel ? mMinZoomLevel : zoomLevel > mMaxZoomLevel ? mMaxZoomLevel : zoomLevel;
		invalidate();
	}
	
	public float getZoomLevel( )
	{
		return mZoomLevel;
	}
	
	@Override
	protected void onDraw(Canvas canvas) 
	{
		int vPadding = 5;
		super.onDraw(canvas);
		
		//draw zoomIn icon
		int x = (getWidth() -  mZoomInIcon.getWidth())/2;
		int y1 = getHeight() - mZoomInIcon.getHeight() - vPadding;		
		canvas.drawBitmap(mZoomInIcon, x, y1, mPaint );
		
		//draw the zoom out Icon
		int y2 = vPadding;
		canvas.drawBitmap(mZoomOutIcon, x, y2, mPaint );
		
		//draw a vertical line		
/*		
 		y2 += mZoomOutIcon.getHeight();
		x += mZoomInIcon.getWidth()/2;		
		canvas.drawLine(x, y1, x, y2, mPaint);
*/		
		x = (getWidth() - mZoomIndicatorIcon.getWidth())/2;
		y2 += mZoomOutIcon.getHeight();						//the higher range
		y1 -= mZoomIndicatorIcon.getHeight();				//the lower range 
		//draw the zoom indicator	
		int y = (int)(y2 + (y1-y2)*(mMaxZoomLevel - mZoomLevel)/(mMaxZoomLevel - mMinZoomLevel));
		canvas.drawBitmap( mZoomIndicatorIcon, x, y, mPaint );
		
		//canvas.drawLine(x - LEVEL_WIDTH/2, y, x + LEVEL_WIDTH/2, y, mPaint);
		
	}

}