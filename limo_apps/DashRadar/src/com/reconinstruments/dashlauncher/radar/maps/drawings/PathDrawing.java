package com.reconinstruments.dashlauncher.radar.maps.drawings;

import java.util.ArrayList;

import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import com.reconinstruments.dashlauncher.radar.prim.PointD;

public class PathDrawing {
	private final static String TAG = "PathDrawing";
	
	public ArrayList<PointD>	mPathPoints;
	protected Path	mPath;
	protected RectF mBoundingBox;

	public PathDrawing(ArrayList<PointD> pathPoints) {
		mPathPoints = pathPoints;
	}
	
	public void Release(){
		if(mPath != null) {
			mPath.reset();
			mPath = null;
		}
		mBoundingBox = null;
		mPathPoints.clear();
	}
	
	public RectF GetBoundingBox(){
		if(mBoundingBox == null) {
			mBoundingBox = new RectF();
			GetPath().computeBounds(mBoundingBox, true);
		}
		
		return mBoundingBox;
	}
	
	public Path GetPath(){
		if(mPath != null)
			return mPath;
		
		if(mPathPoints != null) {
			mPath = new Path();
			
			mPath.incReserve(mPathPoints.size());
			
			int idx = 0;
			for( PointD pointD : mPathPoints)
			{
				if( idx++ == 0 )
				{			
					mPath.moveTo((float)pointD.x, (float)pointD.y);
				}
				else
				{
					mPath.lineTo((float)pointD.x, (float)pointD.y);
				}
			}
		}
		
		return mPath;
	}	
	
}
