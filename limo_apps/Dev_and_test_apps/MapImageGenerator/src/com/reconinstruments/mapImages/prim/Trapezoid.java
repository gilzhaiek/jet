package com.reconinstruments.mapImages.prim;

import java.util.ArrayList;

import android.graphics.PointF;
import android.util.Log;

public class Trapezoid {
	private final static String TAG = "Trapezoid";
	
    public static final int TOP_LEFT		= 0;
    public static final int TOP_RIGHT		= 1;
    public static final int BOTTOM_RIGHT	= 2;
    public static final int BOTTOM_LEFT		= 3;
    public static final int SIDES			= 4;
	
    protected ArrayList<PointF> mPoints = null;
        
    public Trapezoid( ArrayList<PointF> points )
    {
    	mPoints = points;
    }
    
    public Trapezoid()
    {
    	mPoints = new ArrayList<PointF>();
    	
    	for(int i = 0; i < SIDES; i++) {
    		mPoints.add(i, new PointF(0.0f, 0.0f));
    	}
    }
    
    public void Set(PointF topLeft, PointF topRight, PointF bottomRight, PointF bottomLeft)
    {
    	Set(TOP_LEFT,		topLeft.x,		topLeft.y);
    	Set(TOP_RIGHT,		topRight.x,		topRight.y);
    	Set(BOTTOM_RIGHT, 	bottomRight.x,	bottomRight.y);
    	Set(BOTTOM_LEFT, 	bottomLeft.x,	bottomLeft.y);
    }
    
    public PointF Get(int index){
    	return mPoints.get(index);
    }
    
    public void Set(int index, float xVal, float yVal)
    {
    	mPoints.get(index).x = xVal;
    	mPoints.get(index).y = yVal;
    }
    
    public boolean Contains( float x, float y )
    {
    	boolean oddTransitions = false;
        for( int i = 0, j = (SIDES-1); i < SIDES; j = i++ )
        {
            if( ( mPoints.get(i).y < y && mPoints.get(j).y >= y ) || ( mPoints.get(j).y < y && mPoints.get(i).y >= y ) )
            {
                if( mPoints.get(i).x + ( y - mPoints.get(i).y ) / ( mPoints.get(j).y - mPoints.get(i).y ) * ( mPoints.get(j).x - mPoints.get(i).x ) < x )
                {
                    oddTransitions = !oddTransitions;          
                }
            }
        }
        return oddTransitions;
    }
}
