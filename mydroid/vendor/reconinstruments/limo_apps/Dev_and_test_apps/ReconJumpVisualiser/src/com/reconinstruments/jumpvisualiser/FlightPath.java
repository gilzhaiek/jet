package com.reconinstruments.jumpvisualiser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;

/**
 * 
 * @author: Leif Stroman * 
 * @description: This class encapsulates all the graphical representation functionality of displaying jumps and other data. 
 *
 */
public class FlightPath
{
	private Point screenBounds;
	
	// Rectangular bound objects for drawing space and flight path
	private RectF mBounds, pBounds;
	
	// Point array that holds the 2D flight paths points
	private int numPoints;
	private PointF[] mPoints;
	private float[] fPoints;

	// Draw objects/variables
	private int topIndex = -1, midIndex = -1;
	// -- Background color
	private int backgroundColor = Color.BLACK;
	// -- Paint objects used
	private Paint pPaint = null, tPaint = null, blur1Paint = null, blur2Paint = null, gPaint1 = null, gPaint2 = null, pFillPaint = null;
	// -- Flight paths path object
	private Path mPath1, mPath2;
	
	private float[] vrtArray;

	// Canvas and bitmap objects used for generating the static jump image
	private Canvas dCanvas, genCanvas;
	private Bitmap dSurface, genSurface;
	
	public Jump mJump;
	
	private Bitmap distIcon, dropIcon, timeIcon, reconHeader;
	
	public static final int COLOR_MAIN_F = 0xffff50cc;
	public static final int COLOR_MAIN_H = 0xbbff50cc;
	public static final int COLOR_MAIN_T = 0x00aa00ff; 
	
	private LightingColorFilter lightingFilter;
	
	public FlightPath(Context context, Point screenBounds, int numPoints, RectF pBounds)
	{
		this.screenBounds = screenBounds;
		
		// Initialise bounds
		mBounds = new RectF();
		this.pBounds = pBounds;

		// Initialise paths paint
		pPaint = new Paint();
		pPaint.setAntiAlias(true);
		pPaint.setStyle(Style.STROKE);
		pPaint.setStrokeWidth(9.0f);
		pPaint.setDither(true);
		pPaint.setFilterBitmap(true);

		// Blur Paints
	    blur1Paint = new Paint();
	    blur1Paint.set(pPaint);
	    blur1Paint.setDither(true);
	    blur1Paint.setAntiAlias(true);
	    blur1Paint.setStrokeWidth(12.0f);
	    blur1Paint.setMaskFilter(new BlurMaskFilter(12.0f, BlurMaskFilter.Blur.NORMAL));
    
	    blur2Paint = new Paint();
	    blur2Paint.set(pPaint);
	    blur2Paint.setColor(COLOR_MAIN_H);
	    blur2Paint.setDither(true);
	    blur2Paint.setAntiAlias(true);
	    blur2Paint.setStrokeWidth(12.0f);
	    blur2Paint.setMaskFilter(new BlurMaskFilter(12.0f, BlurMaskFilter.Blur.NORMAL));	    

		// Initialise gradient draw path paints
		gPaint1 = new Paint();
		gPaint1.setAntiAlias(true);
		gPaint1.setStrokeWidth(9.0f);
		gPaint1.setDither(true);
		gPaint1.setFilterBitmap(true);
		
		gPaint2 = new Paint();
		gPaint2.setColor(COLOR_MAIN_F);
		gPaint2.setAntiAlias(true);
		gPaint2.setStrokeWidth(9.0f);
		gPaint2.setDither(true);
		gPaint2.setFilterBitmap(true);

		// Initialise text paint
		Typeface myTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/Eurostib_1.TTF");
		
		tPaint = new Paint();
		tPaint.setColor(0xffffffff);//0xffff50cc);
		tPaint.setAntiAlias(true);
		tPaint.setTextSize(35.0f);
		tPaint.setTypeface(myTypeface);		
		tPaint.setTextAlign(Align.CENTER);
		
		// Set the color tint filter
		lightingFilter = new LightingColorFilter(COLOR_MAIN_F, 1);
		
		// Initialise black path paint
		pFillPaint = new Paint();
		pFillPaint.setColor(0xff000000);
		pFillPaint.setAntiAlias(true);
		pFillPaint.setDither(true);
		pFillPaint.setStyle(Style.STROKE);
		pFillPaint.setStrokeWidth(6.0f);
		pFillPaint.setFilterBitmap(true);	
		
		// Initialise point array
		this.numPoints = numPoints;
		mPoints = new PointF[numPoints];
		for(int x = 0; x < numPoints; x++)
		{
			mPoints[x] = new PointF();
		}
		fPoints = new float[numPoints * 4];
		
		// Initialise path objects
		mPath1 = new Path();
		mPath2 = new Path();
		
		// Initialise a drawable bitmap surface		
		dSurface = Bitmap.createBitmap(screenBounds.x, screenBounds.y, Config.ARGB_8888);
		dCanvas = new Canvas(dSurface);		
		
		// Init vrt array to store the simulated vertical-drop values along flight path curve 
		vrtArray = new float[numPoints];
		
		// Init display graphics
		distIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.distance);
		dropIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.drop);
		timeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.clock);
		
		// Header image to add to facebook upload
		reconHeader = BitmapFactory.decodeResource(context.getResources(), R.drawable.recon_header);
		
		// Initialise a generate-able canvas + surface pair for uploading
		// !! -- Must be called after reconHeader bitmap has been initialised
		genSurface = Bitmap.createBitmap(screenBounds.x, screenBounds.y + reconHeader.getHeight() + 5, Config.ARGB_8888);
		genCanvas = new Canvas(genSurface);
	}
	
	// Draw flight path -- and all the flare that goes with it
	public void draw(Canvas canvas, int count)
	{
		draw(canvas, dSurface, count);
	}
	
	// Draw flight path -- and all the flare that goes with it
	public void draw(Canvas canvas, Bitmap background, int count)
	{
		// Draw background bitmap
		canvas.drawBitmap(background, 0, 0, pPaint);

		float circleRadius = 3.0f + (6.0f*((float)count/(float)numPoints));
		float glowRadius   = 6.0f - (3.0f*((float)count/(float)numPoints));
		
		if(count < midIndex)
		{
			// Draw line
			canvas.drawLines(fPoints, 0, count*4, gPaint1);
			
			// Draw moving circle
			canvas.drawCircle(mPoints[count].x, mPoints[count].y, circleRadius, gPaint1);		
			canvas.drawCircle(mPoints[count].x, mPoints[count].y, circleRadius, blur1Paint);
		}
		else
		{
			// Draw line
			int midPoint = (int)midIndex*4;
			canvas.drawLines(fPoints, 0, midPoint, gPaint1);
			canvas.drawLines(fPoints, midPoint, count*4 - midPoint, gPaint2);
			
			// Draw moving circle
			canvas.drawCircle(mPoints[count].x, mPoints[count].y, circleRadius, gPaint2);		
			canvas.drawCircle(mPoints[count].x, mPoints[count].y, glowRadius, blur2Paint);
		}	
		
		float ratio = (float)count/((float)numPoints - 1);
		
		String msg = Util.roundTo1D(mJump.mDistance*ratio) + "m";
		tPaint.setTextAlign(Align.LEFT);
		canvas.drawText(msg, 25 + distIcon.getWidth() + 10, 48, tPaint);
		canvas.drawBitmap(distIcon, 25, 25, tPaint);
		
		msg = Util.roundTo1D(vrtArray[count]) + "m";
		tPaint.setTextAlign(Align.RIGHT);
		canvas.drawText(msg, screenBounds.x - 25, 48, tPaint);
		canvas.drawBitmap(dropIcon, screenBounds.x - tPaint.measureText(msg) - dropIcon.getWidth() - 35, 25, tPaint);
		
		msg = Util.roundTo1D((mJump.mAir*ratio)/1000) + "s";
		tPaint.setTextAlign(Align.CENTER);
		Util.drawCenteredText(msg, screenBounds.x/2, screenBounds.y - 35, canvas, tPaint);
		canvas.drawBitmap(timeIcon, (screenBounds.x - tPaint.measureText(msg))/2 - timeIcon.getWidth() - 10, screenBounds.y - 40, tPaint);
	}
	
	public void drawNoData(Canvas canvas)
	{
		canvas.drawColor(Color.BLACK);
		
		// Draw text message indicating that there is not jump data
		float centerX = pBounds.left + (pBounds.width()/2);
		float centerY = pBounds.top  + (pBounds.height()/2);
		canvas.drawText("NO JUMP DATA", centerX, centerY + 10, tPaint);
		
		//canvas.drawRect(pBounds, tPaint);
	}	
	
	// Generates the array of 2D points pertaining to the last jumps flight path
	public void generateFlightPath()
	{
		// Gets the mJumps relative bounds so that a realistic flight path can be generated
		float dWidth  = mJump.mDistance;
		float dHeight = Math.max(mJump.mDrop, mJump.mHeight);		
		float left = 0, top = 0, right = 0, bottom = 0;		
		if((dHeight/dWidth) < (pBounds.height()/pBounds.width()))
		{
			left  = pBounds.left;
			right = pBounds.right;
		
			float midY  = pBounds.top + (pBounds.height())/2;
			float yDist = (right - left)*(dHeight/dWidth);
			
			top = (int)(midY - yDist/2);
			bottom = (int)(midY + yDist/2);
		}
		else
		{
			top    = pBounds.top;
			bottom = pBounds.bottom;
		
			float midX  = pBounds.left + (pBounds.width())/2;
			float xDist = (bottom - top)*(dWidth/dHeight);
			
			left = (int)(midX - xDist/2);
			right = (int)(midX + xDist/2);
		}		
		mBounds.set(left, top, right, bottom);

		// Generate the 2D points within the calculated bounds that represent the realistic (physical) flight path
		// Physics used :
		//            -->   vY = sqrt(2 * accel * distY)     // y velocity is equal to the square root of 2 x accel of gravity x the vertical distance 
		//            -->   time = vY/accelY
		float panelH = bottom - top;
		float panelW = right - left;
		
		float a = 9.81f;
		float vY = Util.getVelocity(a, mJump.mHeight);

		float maxVrt = Math.max(mJump.mDrop, mJump.mHeight);	
		float yPos = mJump.mHeight;
		
		float t1 = Util.getTime(a, mJump.mHeight);
		float t2 = Util.getTime(a, mJump.mDrop);
		float tF = t1 + t2;
		
		int count = 0;
		for(float t = 0; count < mPoints.length - 1; t += tF/(numPoints - 1))
		{
			yPos = mJump.mHeight - (vY * t) + ((a * (t*t))/2);
			mPoints[count++].set(left + panelW*(t/tF), bottom - ((panelH) * (1.0f - (yPos / maxVrt))));
		}
		yPos = mJump.mHeight - (vY * tF) + ((a * (tF*tF))/2);
		mPoints[count].set(left + panelW, bottom - ((panelH) * (1.0f - (yPos / maxVrt))));
		
		// Set float point list
		for(int x = 0; x < mPoints.length - 1; x++)
		{
			fPoints[x*4 + 0] = mPoints[x + 0].x;
			fPoints[x*4 + 1] = mPoints[x + 0].y;
			fPoints[x*4 + 2] = mPoints[x + 1].x;
			fPoints[x*4 + 3] = mPoints[x + 1].y;
		}
		
		// Set vertical value array
		topIndex = (int)(t1/(tF/(numPoints - 1)));
		for(int x = 0; x < topIndex; x++)
		{
			vrtArray[x] = 0;
		}
		count = topIndex;
		for(float t = 0; count < numPoints; t += tF/(numPoints - 1))
		{
			vrtArray[count++] = Util.getDistance(a, t);
		}
		vrtArray[numPoints - 1] = mJump.mDrop;
	}
	
	public void generateBitmap()
	{
		generateBitmap(dCanvas);
	}
	
	// Creates the visual path object based on the current array of 2D points last generated (in above function)
	public void generateBitmap(Canvas canvas)
	{
		// Finds the best middle point at which to center the gradience
		midIndex = numPoints/2;
		if(mJump.mDistance == 0 || (mBounds.bottom - mBounds.top) / (mBounds.right - mBounds.left) > 10)
		{
			midIndex = topIndex;
		}
		
		// Generate jumps path object
		mPath1.rewind();		
		PointF p = mPoints[0];
		mPath1.moveTo(p.x, p.y);		
		for(int x = 1; x < midIndex; x++)
		{
			p = mPoints[x];
			mPath1.lineTo(p.x, p.y);
		}
		
		mPath2.rewind();
		p = mPoints[midIndex];
		if(midIndex > 0){ midIndex--; } // -- Handles the out of bounds error when height == 0
		mPath2.moveTo(p.x, p.y);		
		for(int x = midIndex; x < numPoints; x++)
		{
			p = mPoints[x];
			mPath2.lineTo(p.x, p.y);
		}
		
		// Draw background colour
		canvas.drawColor(backgroundColor);

		// Set gradient draw path paints
		gPaint1.setShader(new LinearGradient(mPoints[0].x, mPoints[0].y, mPoints[midIndex].x, mPoints[midIndex].y, 
											 new int[]{COLOR_MAIN_T, COLOR_MAIN_F}, null, TileMode.CLAMP));	
		
		// Draw white path lines
		pPaint.setShader(new LinearGradient(mPoints[0].x, mPoints[0].y, mPoints[midIndex].x, mPoints[midIndex].y, 
											new int[]{0x00ffffff, 0xffffffff, 0xffffffff}, null, TileMode.CLAMP)); 
		canvas.drawPath(mPath1, pPaint);
		canvas.drawPath(mPath1, pFillPaint);
				
		pPaint.setShader(new LinearGradient(mPoints[midIndex].x, mPoints[midIndex].y, mPoints[numPoints - 1].x, mPoints[numPoints - 1].y,
											new int[]{0xffffffff, 0xffffffff, 0x00ffffff}, null, TileMode.CLAMP)); 
		canvas.drawPath(mPath2, pPaint);
		canvas.drawPath(mPath2, pFillPaint);
		
		// Draw blur path
		blur1Paint.setShader(new LinearGradient(mPoints[0].x, mPoints[0].y, mPoints[midIndex].x, mPoints[midIndex].y, 
											    new int[]{COLOR_MAIN_T, COLOR_MAIN_H}, null, TileMode.CLAMP)); 
		canvas.drawPath(mPath1, blur1Paint);		
		blur1Paint.setShader(new LinearGradient(mPoints[midIndex].x, mPoints[midIndex].y, mPoints[numPoints - 1].x, mPoints[numPoints - 1].y,
				   							    new int[]{COLOR_MAIN_H, COLOR_MAIN_T}, null, TileMode.CLAMP));
		canvas.drawPath(mPath2, blur1Paint);
	} 
	
	// Returns the bitmap object with rendered jump at the given draw index (index is between 0 and numPoints)
	public Bitmap getJumpBitmap(int index)
	{		
		// Created main drawing surface
		//genSurface = Bitmap.createBitmap(screenBounds.x, screenBounds.y + reconHeader.getHeight() + 5, Config.ARGB_8888);
		//genCanvas = new Canvas(genSurface);
		
		// Generate jump graphic
		Bitmap backSurface = Bitmap.createBitmap(screenBounds.x, screenBounds.y, Config.ARGB_8888);
		Canvas backCanvas = new Canvas(backSurface);		
		pFillPaint.setAlpha(0);
		backgroundColor = Color.WHITE;		
		generateBitmap(backCanvas);
		pFillPaint.setAlpha(255);
		backgroundColor = Color.BLACK;
		
		// Draw completed jump + stats on jump graphic
		toggleColorFilter(true);
		draw(backCanvas, backSurface, index);
		toggleColorFilter(false);
		
		// Draw background color
		genCanvas.drawColor(Color.WHITE);
		// Draw jump graphic on main surface
		genCanvas.drawBitmap(backSurface, 0, reconHeader.getHeight() + 5, pPaint);
		// Draw Recon header icon
		genCanvas.drawBitmap(reconHeader, 0, 0, pPaint);
		
		return genSurface;
	}
	
	public void toggleColorFilter(boolean state)
	{
		if(state)
		{
			tPaint.setColorFilter(lightingFilter);
			return;
		}
		tPaint.setColorFilter(null);
	}
	
	public void setJump(Jump jump)
	{
		mJump = jump;
	}
}