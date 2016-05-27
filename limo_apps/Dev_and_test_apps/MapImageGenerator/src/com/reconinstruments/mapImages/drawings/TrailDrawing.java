package com.reconinstruments.mapImages.drawings;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.util.Log;

import com.reconinstruments.mapImages.objects.Trail;
import com.reconinstruments.mapImages.prim.PointD;

public class TrailDrawing extends PolylineDrawing {
	private static final String TAG = "TrailDrawing";
	public Trail	mTrail;
	private Paint	mPaint = null;
	private Paint	mTextPaint = null;
	public MapDrawings.State mState = MapDrawings.State.NORMAL;

	public TrailDrawing(ArrayList<PointD> pathPoints, Trail trail) {
		super(pathPoints, true);
		mTrail = trail;
//		Log.i(TAG,"Trail: "+mTrail.Name + " : " +mPathAngle);
	}
	
	public void Release(){
		super.Release();
		mTrail = null;
	}
	
	public void InitPaint(int type){
		int 	paintColor ; 
		float 	paintWidth ;
		int     paintAlpha;

		mTextPaint = new Paint( );
		mTextPaint.setTextSize(NAME_TEXT_SIZE);
		mTextPaint.setTextAlign(Align.CENTER);
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTypeface(Typeface.SANS_SERIF);
		mTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
		mTextPaint.setColor(Color.WHITE);		
//		mTextPaint.setShadowLayer(2, 0, 0, Color.BLACK);
		
		mTextOutlinePaint = new Paint( );
		mTextOutlinePaint.setTextSize(NAME_TEXT_SIZE);
		mTextOutlinePaint.setTextAlign(Align.CENTER);
		mTextOutlinePaint.setAntiAlias(true);
		mTextOutlinePaint.setTypeface(Typeface.SANS_SERIF);
		mTextOutlinePaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
		mTextOutlinePaint.setColor(Color.BLACK);		
		mTextOutlinePaint.setStyle(Paint.Style.STROKE);
		mTextOutlinePaint.setStrokeWidth(3);


		
		// TODO change this to resource
		switch(type) {
		case Trail.GREEN_TRAIL:
			paintColor = 0xff00A651;//0xff00ff00;
			paintWidth = TRAIL_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.BLUE_TRAIL:
			paintColor = 0xff0095DA;//0xff0000ff
			paintWidth = TRAIL_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.BLACK_TRAIL:
			paintColor = 0xff000000;
			paintWidth = TRAIL_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.DBLBLACK_TRAIL:
			paintColor = 0xff000000;
			paintWidth = TRAIL_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.RED_TRAIL:
			paintColor = 0xffff0000;
			paintWidth = TRAIL_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.GREEN_TRUNK:
			paintColor = 0xff00A651;//0xff00ff00;
			paintWidth = TRUNK_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.BLUE_TRUNK:
			paintColor = 0xff0095DA;//0xff0000ff
			paintWidth = TRUNK_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.BLACK_TRUNK:
			paintColor = 0xff000000;
			paintWidth = TRUNK_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.DBLBLACK_TRUNK:
			paintColor = 0xff000000;
			paintWidth = TRUNK_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.RED_TRUNK:
			paintColor = 0xffff0000;
			paintWidth = TRUNK_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.SKI_LIFT:
			paintColor = 0xff960000;
			paintWidth = LIFT_WIDTH;
			paintAlpha = 4;
//			paintAlpha = PATH_ALPHA/2;
			break;
		case Trail.CHWY_RESID_TRAIL:			
			paintColor = 0xff7D7D7D;
			paintWidth = CHWAY_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.WALKWAY_TRAIL:	
			paintColor = 0xff707070;
			paintWidth = WALKWAY_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		case Trail.TRAIL_INVALID:
		default:
			paintColor = 0xff000000;
			paintWidth = TRAIL_WIDTH;
			paintAlpha = PATH_ALPHA;
			break;
		}

		mPaint = new Paint( );
		mPaint.setAlpha(paintAlpha);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setAntiAlias(true);
		mPaint.setColor(paintColor);
		mPaint.setStrokeWidth(paintWidth);
	}	
	
	public void Draw(Canvas canvas, double heading, RenderSchemeManager rsm, Matrix transformMatrix, double viewScale) {
//		if(mPaint == null) 
//			InitPaint(mTrail.Type);
		
		super.Draw(canvas, heading, rsm.GetTrailPaint(mTrail.Type, mState.ordinal(), viewScale)[0], transformMatrix);
//		if(mTrail.Name.equalsIgnoreCase("Goat's Gully")) {
//			Log.e(TAG,"Trail: "+mTrail.Name + " - "+ heading + ": " +mPathAngle + ", " + mCurrentPathIndex + " | " + rebasedAngle);
//		}
	}
	
	public void DrawNames(Canvas canvas, RenderSchemeManager rsm, Matrix transformMatrix, double viewScale) {
		super.DrawNames(canvas, rsm.GetTrailPaint(mTrail.Type, mState.ordinal(), viewScale)[1], rsm.GetTrailPaint(mTrail.Type,0,viewScale)[2], transformMatrix, mTrail.Name, mTrail.Type);
	}

}
