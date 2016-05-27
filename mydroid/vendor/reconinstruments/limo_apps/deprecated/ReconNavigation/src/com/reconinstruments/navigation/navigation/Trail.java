/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.reconinstruments.navigation.routing.IEdgeRenderer;

public class Trail implements IEdgeRenderer
{
	static final int	PATH_ALPHA = 255;
	static final int	HILITE_PATH_ALPHA = 255;
	static final float  STROKE_WIDTH = 10.f;
	static final int    STROKE_COLOR = 0xffff0000;
	static final int    RED_TRAIL_COLOR = 0xffff0000;
	static final int    BLUE_TRAIL_COLOR = 0xff0000ff;
	static final int	GREEN_TRAIL_COLOR = 0xff00ff00;
	static final int    BLACK_TRAIL_COLOR = 0xff000000;
	static final int 	DARK_MASK_COLOR = 0xff606060;
	static final int 	BRIGHT_MASK_COLOR = 0xffc0c0c0;
	static final int    LIFT_COLOR = 0xff960000;
	static final int    CHWAY_COLOR = 0xff7D7D7D;//0xff00ffff;
	static final int	HILITE_TRAIL_COLOR = 0xffffff00;
	static final int    WALKWAY_COLOR = 0xff707070;
	static final float	TRAIL_WIDTH = 3.f/(float)Util.DISTANCE_PER_PIXEL;   
	static final float  TRUNK_WIDTH = 6.f/(float)Util.DISTANCE_PER_PIXEL;
	static final float  WALKWAY_WIDTH = 2.f/(float)Util.DISTANCE_PER_PIXEL;
	static final float  CHWAY_WIDTH = 4.f/(float)Util.DISTANCE_PER_PIXEL;
	static final float  LIFT_WIDTH = 2.f/(float)Util.DISTANCE_PER_PIXEL;
	static final float  TRAIL_NAME_HOFFSET = 0.f;
	static final float  TRAIL_NAME_VOFFSET = 5.f;
	
	
	//the threshold  for  dropping the trail name rendering
	//if length between the trail.startpoint and trail.endpoint is larger
	//then the threshold, the trail name will be rendered
	static final float  MINI_LEN_FOR_TRAIL_NAME = 100;			
	
	static final int    NAME_TEXT_SIZE = 20;
	static final int    NAME_TEXT_COLOR = 0xffffffff;
	
	//enum of different trail types that we are interested on
	static final int	TRAIL_INVALID = -1;
	static final int    GREEN_TRAIL = 0;
	static final int    BLUE_TRAIL = 1;
	static final int    BLACK_TRAIL = 2;
	static final int    DBLBLACK_TRAIL = 3;
	static final int    RED_TRAIL = 4;
	static final int    GREEN_TRUNK = 5;
	static final int    BLUE_TRUNK = 6;
	static final int    BLACK_TRUNK = 7;
	static final int    DBLBLACK_TRUNK = 8;
	static final int    RED_TRUNK = 9;
	static final int    SKI_LIFT = 10;
	static final int	CHWY_RESID_TRAIL = 11;			//highway or residential trial
	static final int	WALKWAY_TRAIL = 12;		//walkway trail
	static final int	NUM_TRAIL_TYPES = 13;

	
	//store different style of paint for all trail types
	public static Paint[] sTrailPaints = null;
	public static Paint[] sTrailHilitePaints = null;
	public static Paint sTextPaint = null;
	public static Paint sTwoWayLiftPaint = null;
	protected static PathDashPathEffect sTrailArrowEffect  = null;
	protected static PathDashPathEffect sTrunkArrowEffect = null;
	protected static PathDashPathEffect sLiftArrowEffect = null;
	protected static DashPathEffect  sLiftDashEffect = null;
	protected static boolean sArrowEffectOn = false;
	
	//bounding box of the trail
	public RectF mBBox = null;
	
	//trail name
	public String mName;
	
	//one of the pre-defined trail type
	private int mType;
	
	//the android path submitted for rendering
	private Path mPath = null;
	
	//the inversed path for rendering the text if necessary
	private Path mInversePath = null;
	
	//the paint style for rendering the trail
	private Paint mPaint = null;
	
	//the paint styel for rendering hilited trail
	private Paint mHilitePaint = null;
	
	//the start point
	private PointF mStartPoint = null;
	
	//the end point
	private PointF mEndPoint = null;
	
	//true if the current trail is culled by a viewport
	public boolean mIsCulled = false;
	

	//if this trail is oneWay or not
	private boolean mOneWay = false;	
	
	static public void InitPaints( Context context )
	{
		//double check make sure it has not already been initialized
		if( sTrailPaints != null )
		{
			return;
		}
		
		//create the paint style for rendering the trails
		sTrailPaints = new Paint[ Trail.NUM_TRAIL_TYPES ];
		
		//create the paint style for rendering the hilited trails
		sTrailHilitePaints = new Paint[ Trail.NUM_TRAIL_TYPES ];
		
		sTrailArrowEffect  = new PathDashPathEffect(Util.makeArrowPath( TRAIL_WIDTH ), TRAIL_WIDTH*2, 0, PathDashPathEffect.Style.MORPH);
		sTrunkArrowEffect = new PathDashPathEffect(Util.makeArrowPath( TRUNK_WIDTH ), TRUNK_WIDTH*2, 0, PathDashPathEffect.Style.MORPH);
		sLiftArrowEffect = new PathDashPathEffect(Util.makeArrowPath( LIFT_WIDTH ), LIFT_WIDTH*2, 0, PathDashPathEffect.Style.MORPH);
		sLiftDashEffect  = new DashPathEffect( new float[] {30, 20, 10, 20}, 0 );
		
		sTextPaint = new Paint( );
		sTextPaint.setTextSize(NAME_TEXT_SIZE);
		sTextPaint.setTextAlign(Align.CENTER);
		sTextPaint.setAntiAlias(true);
		sTextPaint.setTypeface(Typeface.SANS_SERIF);
		sTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
		sTextPaint.setColor(Util.WHITE_COLOR);

		for( int idx = 0; idx < Trail.NUM_TRAIL_TYPES; ++idx )
		{
			Paint paint = new Paint( );
			paint.setAlpha(PATH_ALPHA);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStrokeJoin(Paint.Join.ROUND);
			paint.setAntiAlias(true);
			
			//paint style for hilited trails
			Paint hilitePaint = new Paint();
			hilitePaint.setAlpha(HILITE_PATH_ALPHA);
			hilitePaint.setStyle(Paint.Style.STROKE);
			hilitePaint.setStrokeCap(Paint.Cap.ROUND);
			hilitePaint.setStrokeJoin(Paint.Join.ROUND);
			hilitePaint.setAntiAlias(true);
			
			//create the paint style for rendering the trail name 
			/*sTextPaint[idx] = new Paint( );
			sTextPaint[idx].setTextSize(NAME_TEXT_SIZE);
			sTextPaint[idx].setTextAlign(Align.CENTER);
			sTextPaint[idx].setAntiAlias(true);
			sTextPaint[idx].setTypeface(Typeface.SANS_SERIF);
			sTextPaint[idx].setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));*/			

			switch( idx )
			{
			case Trail.GREEN_TRAIL:
				paint.setColor(Trail.GREEN_TRAIL_COLOR);
				paint.setStrokeWidth(Trail.TRAIL_WIDTH);
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.TRAIL_WIDTH);
				//sTextPaint[idx].setColor(Trail.GREEN_TRAIL_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;
			
			case Trail.GREEN_TRUNK:
				paint.setColor(Trail.GREEN_TRAIL_COLOR);
				paint.setStrokeWidth(Trail.TRUNK_WIDTH);
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.TRUNK_WIDTH);
				//sTextPaint[idx].setColor(Trail.GREEN_TRAIL_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;
			
			case Trail.BLUE_TRAIL:
				paint.setColor(Trail.BLUE_TRAIL_COLOR);
				paint.setStrokeWidth(Trail.TRAIL_WIDTH);
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.TRAIL_WIDTH);
				//sTextPaint[idx].setColor(Trail.BLUE_TRAIL_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;
			
			case Trail.BLUE_TRUNK:
				paint.setColor(Trail.BLUE_TRAIL_COLOR);
				paint.setStrokeWidth(Trail.TRUNK_WIDTH);	
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.TRUNK_WIDTH);	
				//sTextPaint[idx].setColor(Trail.BLUE_TRAIL_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;
			
			case Trail.BLACK_TRAIL:
			case Trail.DBLBLACK_TRAIL:
				paint.setColor(Trail.BLACK_TRAIL_COLOR);
				paint.setStrokeWidth(Trail.TRAIL_WIDTH);
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.TRAIL_WIDTH);
				///sTextPaint[idx].setColor(Trail.BLACK_TRAIL_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;
			
			case Trail.BLACK_TRUNK:
			case Trail.DBLBLACK_TRUNK:
				paint.setColor(Trail.BLACK_TRAIL_COLOR);
				paint.setStrokeWidth(Trail.TRUNK_WIDTH);
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.TRUNK_WIDTH);
				//sTextPaint[idx].setColor(Trail.BLACK_TRAIL_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;
			
			case Trail.RED_TRAIL:
				paint.setColor(Trail.RED_TRAIL_COLOR);
				paint.setStrokeWidth(TRAIL_WIDTH);
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.TRAIL_WIDTH);
				//sTextPaint[idx].setColor(Trail.RED_TRAIL_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;
			
			case Trail.RED_TRUNK:
				paint.setColor(Trail.RED_TRAIL_COLOR);
				paint.setStrokeWidth(Trail.TRUNK_WIDTH);
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.TRUNK_WIDTH);	
				//sTextPaint[idx].setColor(Trail.RED_TRAIL_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;

			case Trail.CHWY_RESID_TRAIL:
				paint.setColor(Trail.CHWAY_COLOR);
				paint.setStrokeWidth(Trail.CHWAY_WIDTH);
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.CHWAY_WIDTH);
				//sTextPaint[idx].setColor(Trail.CHWAY_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;
			
			case Trail.WALKWAY_TRAIL:
				paint.setColor(WALKWAY_COLOR);
				paint.setStrokeWidth(Trail.WALKWAY_WIDTH);
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.WALKWAY_WIDTH);
				//sTextPaint[idx].setColor(Trail.WALKWAY_COLOR | Trail.BRIGHT_MASK_COLOR);
				
			break;
			
			case Trail.SKI_LIFT:
				paint.setColor(Trail.LIFT_COLOR);
				paint.setStrokeWidth(Trail.LIFT_WIDTH);
				//DashPathEffect pathEffect = new DashPathEffect( new float[] {10, 5, 5, 5}, 0 );
				paint.setPathEffect(sLiftDashEffect);		
				
				hilitePaint.setColor(Trail.HILITE_TRAIL_COLOR);
				hilitePaint.setStrokeWidth(Trail.LIFT_WIDTH);
				hilitePaint.setPathEffect(sLiftDashEffect);
				//sTextPaint[idx].setColor(Trail.LIFT_COLOR | Trail.BRIGHT_MASK_COLOR);
			break;
			
			}
			
			sTrailPaints[idx] = paint;
			sTrailHilitePaints[idx] = hilitePaint;
			
		}
		
		//the paint style for two-way lift
		sTwoWayLiftPaint = new Paint();
		sTwoWayLiftPaint.setAlpha(PATH_ALPHA);
		sTwoWayLiftPaint.setStyle(Paint.Style.STROKE);
		sTwoWayLiftPaint.setStrokeCap(Paint.Cap.ROUND);
		sTwoWayLiftPaint.setStrokeJoin(Paint.Join.ROUND);
		sTwoWayLiftPaint.setAntiAlias(true);
		sTwoWayLiftPaint.setColor(Trail.LIFT_COLOR);
		sTwoWayLiftPaint.setStrokeWidth(Trail.LIFT_WIDTH);
		sTwoWayLiftPaint.setPathEffect(sLiftDashEffect);			

		
		//bind the arrow effect if needed
		setArrowEffect( sArrowEffectOn );
	}
	
	static public void setArrowEffect( boolean on )
	{
		sArrowEffectOn = on;
		
		//if paint style not intialized yet, then
		//just do nothing
		if( sTrailPaints == null )
		{
			return;
		}
		
		for( int idx = 0; idx < sTrailPaints.length; ++idx )
		{
			Paint paint = sTrailPaints[idx];
			Paint hilitPaint = sTrailHilitePaints[idx];
			switch( idx )
			{
			case Trail.GREEN_TRAIL:
			case Trail.BLUE_TRAIL:
			case Trail.BLACK_TRAIL:
			case Trail.DBLBLACK_TRAIL:
			case Trail.RED_TRAIL:
				if( Trail.sArrowEffectOn )
				{
					//turn off path effect
					paint.setPathEffect( sTrailArrowEffect );
					hilitPaint.setPathEffect( sTrailArrowEffect );
				}
				else
				{
					paint.setPathEffect( null );
					hilitPaint.setPathEffect( null );
				}
			break;
			
			case Trail.GREEN_TRUNK:
			case Trail.BLUE_TRUNK:
			case Trail.BLACK_TRUNK:
			case Trail.DBLBLACK_TRUNK:
			case Trail.RED_TRUNK:
				if( Trail.sArrowEffectOn )
				{
					//turn off path effect
					paint.setPathEffect( sTrunkArrowEffect );
					hilitPaint.setPathEffect( sTrunkArrowEffect );
				}
				else
				{
					paint.setPathEffect( null );
					hilitPaint.setPathEffect( null );
				}
			break;
			
			case Trail.SKI_LIFT:
				if( Trail.sArrowEffectOn )
				{
					//turn off path effect
					paint.setPathEffect( sLiftArrowEffect  );
					hilitPaint.setPathEffect( sLiftArrowEffect );
				}
				else
				{
					paint.setPathEffect( sLiftDashEffect );
					hilitPaint.setPathEffect( sLiftDashEffect );
				}
			break;
			}			
		}	
	}
	static public void ToggleArrowEffect( )
	{
		Trail.sArrowEffectOn = !Trail.sArrowEffectOn;
		setArrowEffect( Trail.sArrowEffectOn );		
		
	}
	
	static public boolean IsArrowEffectOn( )
	{
		return Trail.sArrowEffectOn;
	}
	
	public Trail( ArrayList<PointF> line, String name, int trailType, boolean oneWay )
	{
		mPath = new Path( );
		mInversePath = new Path( );
		mOneWay = oneWay;
	
		//hint mPath and mInversePath the points to be added to
		mPath.incReserve(line.size());
		mInversePath.incReserve(line.size());
		
		mName = name;
		mType = trailType;
		mStartPoint = line.get(0);
		mEndPoint = line.get( line.size() - 1 );
		

		//for two way lift, using a separate paint
		//since they should always using the dash-effect
		//no matter if Arrow-Effect is on or not
		if( trailType == Trail.SKI_LIFT && oneWay == false )
		{
			mPaint = sTwoWayLiftPaint;
		}
		else
		{
			mPaint = sTrailPaints[mType];
		}
		
		mHilitePaint = sTrailHilitePaints[mType];
		
		//create the path for rendering
		int idx = 0;
		for( PointF p : line )
		{
			if( idx == 0 )
			{				
				mPath.moveTo( p.x, p.y );
			}
			else
			{
				mPath.lineTo(p.x, p.y);
			}
			++idx;
		}

		int numPoints = line.size();
		//create the inversed path for rendering name label on it
		for( idx = numPoints - 1; idx >=0; --idx )
		{
			PointF p = line.get(idx);
			if( idx == numPoints - 1 )
			{				
				mInversePath.moveTo( p.x, p.y );
			}
			else
			{
				mInversePath.lineTo(p.x, p.y);
			}						
		}		

		
		
		mBBox = new RectF( );
		mPath.computeBounds(mBBox, true);
		
		//calcLength( line, name );
		
		
	}
	
	public int getType( )
	{
		return mType;
	}
	
	public void setRenderWidth( int width )
	{
		mPaint.setStrokeWidth(width);
	}
	
	public void setRenderColor( int color )
	{
		mPaint.setColor(color);
	}
	
	public void setRenderJoinStyle( Paint.Join jointStyle )
	{
		mPaint.setStrokeJoin( jointStyle );
	}
	
	public void drawTrail( Canvas canvas, Matrix transform )
	{
		canvas.drawPath(mPath, mPaint);
	}
		
	public void drawName( Canvas canvas, Matrix transform )
	{
		//find the proper way for drawing the name on the path
		float[] start = { mStartPoint.x, mStartPoint.y };
		float[] end = { mEndPoint.x, mEndPoint.y };

		transform.mapPoints( start );
		transform.mapPoints( end );

		float x = end[0] - start[0];
		float y = end[1] - start[1];
		float len = PointF.length(x, y);
		x /= len;
		y /= len;

		if( len > Trail.MINI_LEN_FOR_TRAIL_NAME )
		{
			
			if( Math.abs(x)*3 > Math.abs(y) )
			{
				if( x > 0 )
				{
					canvas.drawTextOnPath( mName, mPath, TRAIL_NAME_HOFFSET, -TRAIL_NAME_VOFFSET, Trail.sTextPaint );
				}
				else
				{
					canvas.drawTextOnPath( mName, mInversePath, TRAIL_NAME_HOFFSET, -TRAIL_NAME_VOFFSET, Trail.sTextPaint );
				}
			}
			else
			{
				if( y > 0 )
				{
					canvas.drawTextOnPath( mName, mPath, TRAIL_NAME_HOFFSET, -TRAIL_NAME_VOFFSET, Trail.sTextPaint );
				}
				else
				{
					canvas.drawTextOnPath( mName, mInversePath, TRAIL_NAME_HOFFSET, -TRAIL_NAME_VOFFSET, Trail.sTextPaint );
				}			
			}
		}

	}
	
	private void calcLength( ArrayList<PointF> line, String name )
	{
		float len = 0;
		float x,y;
		for( int i = 0; i < line.size() - 1; ++i )
		{
			PointF p1 = line.get(i);
			PointF p2 = line.get(i+1);
			
			x = p1.x - p2.x;
			y = p1.y - p2.y;
			
			float le = (float)Math.sqrt(x*x + y*y);
			len += le;
		}
		
		len *= Util.DISTANCE_PER_PIXEL;
		Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Trail " + name + " has length of: " + len );
	}
	

    static public void updateHiliteTrailWidth( float scaleFactor )
    {
    	//make sure the minmum width to draw for the hilite trail
    	//is that the one-third of the default width
    	float minWidth = 1.f/3.f;
    	if( scaleFactor < minWidth )
    	{
    		float scaleUp = minWidth/scaleFactor;
    		
    		for( int idx = 0; idx < Trail.NUM_TRAIL_TYPES; ++idx )
    		{
    			Paint paint = sTrailHilitePaints[idx];
    			switch( idx )
    			{
    			case Trail.GREEN_TRAIL:
    			case Trail.BLUE_TRAIL:
    			case Trail.BLACK_TRAIL:
    			case Trail.DBLBLACK_TRAIL:
    			case Trail.RED_TRAIL:
    				paint.setStrokeWidth(Trail.TRAIL_WIDTH*scaleUp);
    			break;
    			
    			case Trail.GREEN_TRUNK:
    			case Trail.BLUE_TRUNK:
    			case Trail.BLACK_TRUNK:
    			case Trail.DBLBLACK_TRUNK:
    			case Trail.RED_TRUNK:
    				paint.setStrokeWidth(Trail.TRUNK_WIDTH*scaleUp);
    			break;

    			case Trail.CHWY_RESID_TRAIL:
    				paint.setStrokeWidth(Trail.CHWAY_WIDTH*scaleUp);
    			break;
    			
    			case Trail.WALKWAY_TRAIL:
    				paint.setStrokeWidth(Trail.WALKWAY_WIDTH*scaleUp);
    			break;
    			
    			case Trail.SKI_LIFT:
    				paint.setStrokeWidth(Trail.LIFT_WIDTH*scaleUp);
    			break;
    			
    			}
    		}
    	}
    }
    
    //implemented IEdgeRenderer interface
	public void drawHilite( Canvas canvas, Matrix transform )
	{
		canvas.drawPath(mPath, mHilitePaint);
	}

	public RectF getBBox()
	{
		return mBBox;
	}
	

}

