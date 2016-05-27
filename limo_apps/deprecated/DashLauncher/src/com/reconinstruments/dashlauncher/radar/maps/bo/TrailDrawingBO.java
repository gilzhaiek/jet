package com.reconinstruments.dashlauncher.radar.maps.bo;

import java.util.ArrayList;

import com.reconinstruments.dashlauncher.radar.maps.drawings.TrailDrawing;
import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortInfo;
import com.reconinstruments.dashlauncher.radar.maps.objects.Trail;
import com.reconinstruments.dashlauncher.radar.prim.PointD;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.Paint.Align;

public class TrailDrawingBO {
	private int[] 				mTrailColors = null; 
	private float[] 			mTrailWidth = null;
	private Paint[]				mTrailPaints = null;
	
	public static Paint 		mTextPaint = null;
	
	protected static Paint 		mTwoWayLiftPaint = null;
	
	public static final int		PATH_ALPHA = 255;
	public static final float 	DISTANCE_PER_PIXEL = LocationTransformer.DISTANCE_PER_PIXEL;
	public static final float	TRAIL_WIDTH = 20.f/DISTANCE_PER_PIXEL;
	public static final float	TRUNK_WIDTH = 20.f/DISTANCE_PER_PIXEL;
	public static final float	WALKWAY_WIDTH = 10.f/DISTANCE_PER_PIXEL;
	public static final float	CHWAY_WIDTH = 30.f/DISTANCE_PER_PIXEL;
	public static final float	LIFT_WIDTH = 30.f/DISTANCE_PER_PIXEL;
		
	public static final float	STROKE_WIDTH = 10.f;
	public static final int		STROKE_COLOR = 0xffff0000;
	   
	public static final float	TRAIL_NAME_HOFFSET = 0.f;
	public static final float	TRAIL_NAME_VOFFSET = 5.f;
	
	
	//the threshold  for  dropping the trail name rendering
	//if length between the trail.startpoint and trail.endpoint is larger
	//then the threshold, the trail name will be rendered
	public static final float	MINI_LEN_FOR_TRAIL_NAME = 100;			
	
	public static final int		NAME_TEXT_SIZE = 20;
	public static final int		NAME_TEXT_COLOR = 0xffffffff;
	

	public boolean	mInitialized = false;
	
	private LocationTransformer mLocationTransformer = null;
	
	public TrailDrawingBO(LocationTransformer locationTransformer) {
		mLocationTransformer = locationTransformer;
	}
	
	public void InitPaint(){
		if(mInitialized)
			return;
		
		mTextPaint = new Paint( );
		mTextPaint.setTextSize(NAME_TEXT_SIZE);
		mTextPaint.setTextAlign(Align.CENTER);
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTypeface(Typeface.SANS_SERIF);
		mTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
		mTextPaint.setColor(Color.WHITE);		
				
		// TODO change this to resource
		mTrailColors = new int[Trail.NUM_TRAIL_TYPES];
		mTrailColors[Trail.GREEN_TRAIL]			= 0xff00A651;//0xff00ff00;
		mTrailColors[Trail.BLUE_TRAIL]			= 0xff0095DA;//0xff0000ff;
		mTrailColors[Trail.BLACK_TRAIL]			= 0xff000000;
		mTrailColors[Trail.DBLBLACK_TRAIL]		= 0xff000000;
		mTrailColors[Trail.RED_TRAIL]			= 0xffff0000;
		mTrailColors[Trail.GREEN_TRUNK]			= 0xff00A651;//0xff00ff00;
		mTrailColors[Trail.BLUE_TRUNK]			= 0xff0095DA;//0xff0000ff;
		mTrailColors[Trail.BLACK_TRUNK]			= 0xff000000;
		mTrailColors[Trail.DBLBLACK_TRUNK]		= 0xff000000;
		mTrailColors[Trail.RED_TRUNK]			= 0xffff0000;
		mTrailColors[Trail.SKI_LIFT]			= 0xff960000;
		mTrailColors[Trail.CHWY_RESID_TRAIL]	= 0xff7D7D7D;
		mTrailColors[Trail.WALKWAY_TRAIL]		= 0xff707070;
		
		mTrailWidth = new float[Trail.NUM_TRAIL_TYPES];
		mTrailWidth[Trail.GREEN_TRAIL]			= TRAIL_WIDTH;
		mTrailWidth[Trail.BLUE_TRAIL]			= TRAIL_WIDTH;
		mTrailWidth[Trail.BLACK_TRAIL]			= TRAIL_WIDTH;
		mTrailWidth[Trail.DBLBLACK_TRAIL]		= TRAIL_WIDTH;
		mTrailWidth[Trail.RED_TRAIL]			= TRAIL_WIDTH;
		mTrailWidth[Trail.GREEN_TRUNK]			= TRUNK_WIDTH;
		mTrailWidth[Trail.BLUE_TRUNK]			= TRUNK_WIDTH;
		mTrailWidth[Trail.BLACK_TRUNK]			= TRUNK_WIDTH;
		mTrailWidth[Trail.DBLBLACK_TRUNK]		= TRUNK_WIDTH;
		mTrailWidth[Trail.RED_TRUNK]			= TRUNK_WIDTH;
		mTrailWidth[Trail.SKI_LIFT]				= LIFT_WIDTH;
		mTrailWidth[Trail.CHWY_RESID_TRAIL]		= CHWAY_WIDTH;
		mTrailWidth[Trail.WALKWAY_TRAIL]		= WALKWAY_WIDTH;
		
		mTrailPaints = new Paint[ Trail.NUM_TRAIL_TYPES ];
		for( int idx = 0; idx < Trail.NUM_TRAIL_TYPES; ++idx )
		{
			Paint paint = new Paint( );
			paint.setAlpha(PATH_ALPHA);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStrokeJoin(Paint.Join.ROUND);
			paint.setAntiAlias(true);
			paint.setColor(mTrailColors[idx]);
			paint.setStrokeWidth(mTrailWidth[idx]);
			mTrailPaints[idx] = paint;			
		}		
		
		mTwoWayLiftPaint = new Paint();
		mTwoWayLiftPaint.setAlpha(PATH_ALPHA);
		mTwoWayLiftPaint.setStyle(Paint.Style.STROKE);
		mTwoWayLiftPaint.setStrokeCap(Paint.Cap.ROUND);
		mTwoWayLiftPaint.setStrokeJoin(Paint.Join.ROUND);
		mTwoWayLiftPaint.setAntiAlias(true);
		mTwoWayLiftPaint.setColor(mTrailColors[Trail.SKI_LIFT]);
		mTwoWayLiftPaint.setStrokeWidth(mTrailWidth[Trail.SKI_LIFT]);
		
		mInitialized = true;
	}	
	
	public void Draw(Canvas canvas, TrailDrawing trailDrawings) {
		if(!mInitialized)
			InitPaint();
		
		canvas.drawPath(trailDrawings.GetPath(), mTrailPaints[trailDrawings.mTrail.Type]);
	}
	
	public TrailDrawing CreateTransformedTrailDrawing(Trail trail, ResortInfo resortInfo) {
		ArrayList<PointD> pathPoints = new ArrayList<PointD>(trail.TrailPoints.size());
		
		int idx = 0;
		for( PointF pointF : trail.TrailPoints)
		{
			pathPoints.add(idx++, new PointD(
					mLocationTransformer.TransformLongitude(pointF.x),
					mLocationTransformer.TransformLatitude(pointF.y)));
		}
		
		TrailDrawing trailDrawing = new TrailDrawing(pathPoints, trail);
		
		return trailDrawing;
	}	
}
