package com.reconinstruments.jumpvisualiser;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.view.SurfaceHolder;

public class CanvasThread extends Thread
{
	private SurfaceHolder mSurfaceHolder;
	//private DrawPanel mPanel;
	private FlightPath mFlightPath;
	private boolean isRunning = false;
	
	private int numPoints = -1;
	
	public CanvasThread(SurfaceHolder surfaceHolder, FlightPath flightPath, int numPoints)
	{
		mSurfaceHolder = surfaceHolder;
		//mPanel = panel;
		
		mFlightPath = flightPath;
		
		this.numPoints = numPoints;
	}

	public void setRunning(boolean state)
	{
		isRunning = state;
	}

	@Override
	public void run()
	{
		Canvas c;
		int count = 0;
		int delay = 50, delayCount = 0;;
		while(isRunning && count < numPoints)
		{
			c = null;
			try
			{
				c = mSurfaceHolder.lockCanvas(null); // Lock the canvas object by the surfaceHolder passed in by panel
				synchronized (mSurfaceHolder)
				{ 
					//mPanel.onDraw(c, count);
					mFlightPath.draw(c, count);
				}
				
				if(count < numPoints - 1)
				{ 
					count++; 
				}
				else if(delayCount < delay)
				{
					delayCount++;
					//mFlightPath.setTextColor(FlightPath.COLOR_MAIN_F);
					
					mFlightPath.toggleColorFilter(true);
				}
				else
				{
					count = 0;
					delayCount = 0;
					//mFlightPath.setTextColor(0xffffffff);
					
					mFlightPath.toggleColorFilter(false);
				}
			}
			finally
			{
				// do this in a finally so that if an exception is thrown during the above, we don't leave the Surface in an inconsistent state
				if (c != null)
				{
					mSurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}