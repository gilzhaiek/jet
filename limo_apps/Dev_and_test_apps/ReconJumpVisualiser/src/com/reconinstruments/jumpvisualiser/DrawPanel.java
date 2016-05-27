package com.reconinstruments.jumpvisualiser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DrawPanel extends SurfaceView implements SurfaceHolder.Callback
{
	// Paint objects
	private Paint mPaint = null, tPaint = null;
	
	// FlightPath object - main graphical component
	private FlightPath flightPath = null; 
	
	// Animation thread
	private CanvasThread mThread = null;
	
	// Number of points in the flight paths simulation line
	private int numPoints = 100;
	
	// Constructor
	public DrawPanel(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		getHolder().addCallback(this);
		setFocusable(true);
	}
	
	public void initialize(Point screenBounds)
	{
		// Initialise paint objects
		mPaint = new Paint();
		mPaint.setColor(0xFF00FF00);
		mPaint.setAntiAlias(true);
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(3.0f);
		
		tPaint = new Paint();
		tPaint.setColor(0xFFFF5555);
		tPaint.setAntiAlias(true);
		tPaint.setTextSize(32.0f);
		tPaint.setTypeface(Typeface.DEFAULT_BOLD);		
		tPaint.setTextAlign(Align.CENTER);
		
		// Initialise new FlightPath 
		flightPath = new FlightPath(getContext(), screenBounds, numPoints, new RectF(25, 70, screenBounds.x - 25, 180));	
	}

	public void setJump(Jump jump)
	{
		flightPath.setJump(jump);
	}
	
	public void generateFlightPath()
	{
		// Simulate the physical flight path and generate the points in it
		flightPath.generateFlightPath();
	}
	
	public void saveAndUploadFlightPath()
	{
		Util.saveBitmapToFile(getJumpBitmap());
		Util.uploadJumpImageToFB(flightPath.mJump, getContext());
	}	
	
	public void generateBitmap()
	{
		// Generates the graphical components of the jump and draws them to its bitmap object
		flightPath.generateBitmap();
	}
	
	public void beginAnimatingFlightPath()
	{
		// Kill animation thread as it could cause a shared resource issue with the draw canvas
		killAnimationThread(); 
	
		// Generates the graphical components of the jump and draws them to its bitmap object
		flightPath.generateBitmap();
		
		// Start the animation thread 
		startAnimationThread();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){ } // Do Nothing

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		// Draw initial canvas frame with a label indicating 'No Jump Data'
		SurfaceHolder s = getHolder();
		Canvas c = s.lockCanvas(null); 
		synchronized(s)
		{ 
			flightPath.drawNoData(c);
			s.unlockCanvasAndPost(c); 
		}	
		
		/*
		// --- TEMPORARY --- //
		// Generates, saves and uploads a fake jump (for testing purposes).
		Jump testJump = new Jump(1877, 8.4f, 2.1f, 3.4f);
		//generateFlightPath(testJump);
		setJump(testJump);
		killAnimationThread(); 
		generateFlightPath();
		//saveAndUploadFlightPath();
		//beginAnimatingFlightPath();		
		generateBitmap();
		startAnimationThread();
		*/
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		killAnimationThread();
	}
	
	// Initialises a new animation thread 
	public void startAnimationThread()
	{
		mThread = new CanvasThread(getHolder(), flightPath, numPoints);
		mThread.setRunning(true);
		mThread.start();
	}
	
	// Kills the current running animation thread	
	public void killAnimationThread()
	{
		if(mThread == null){ return; }
		mThread.setRunning(false);
		while(true)
		{
			try
			{
				mThread.join();
				break;
			} 
			catch (InterruptedException e){	}
		}
	}
	
	//@Override
	// Main draw function
	public void onDraw(Canvas canvas, int count)
	{ 
		// Draw flightPath object
		flightPath.draw(canvas, count); 
	}	
	
	public Bitmap getJumpBitmap()
	{
		return flightPath.getJumpBitmap(numPoints - 1);
	}
}