package com.reconinstruments.mapImages.drawings;

import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

import com.reconinstruments.mapImages.helpers.LocationTransformer;
import com.reconinstruments.mapImages.mapview.DrawingSet;
import com.reconinstruments.mapImages.objects.POI;
import com.reconinstruments.mapImages.objects.ResortInfo;
import com.reconinstruments.mapImages.objects.Trail;

public class MapDrawings {
	private final static String TAG = "MapDrawings";
	
	public enum State {
		NORMAL,
		HAS_FOCUS,
		DISABLED,
		DISABLED_FOCUS
	}
	
	public RectF mTransformedBoundingBox = null;
	
	public ResortInfo mResortInfo = null;
	
	public ArrayList<POIDrawing>	POIDrawings = null;
	public ArrayList<TrailDrawing>	TrailDrawings = null;
	public ArrayList<AreaDrawing>	AreaDrawings = null;
	public LocationTransformer		mLocationTransformer= null;
	
	public MapDrawings(){		
		POIDrawings = new ArrayList<POIDrawing>();
		TrailDrawings = new ArrayList<TrailDrawing>();
		AreaDrawings = new ArrayList<AreaDrawing>();
		mTransformedBoundingBox = new RectF();
		mLocationTransformer = new LocationTransformer();

	}
	
	
	public void SetResortInfo(ResortInfo resortInfo) {
		mResortInfo = resortInfo;
	}

	public void SetTransformedBoundingBox(RectF boundingBox) {
		mTransformedBoundingBox.top = boundingBox.top;
		mTransformedBoundingBox.right = boundingBox.right;
		mTransformedBoundingBox.bottom = boundingBox.bottom;
		mTransformedBoundingBox.left = boundingBox.left;
	}	
	
	public void SetTransformedBoundingBox(float top, float right, float bottom, float left) {
		Log.v(TAG, "top="+top+" right="+right+" bottom="+bottom+" left="+left);
		if(bottom > top) {
			mTransformedBoundingBox.top = top;
			mTransformedBoundingBox.bottom = bottom;
		}
		else {
			mTransformedBoundingBox.top = bottom;
			mTransformedBoundingBox.bottom = top;			
		}
		if(right > left ) {
			mTransformedBoundingBox.left = left;
			mTransformedBoundingBox.right = right;
		}
		else {
			mTransformedBoundingBox.left = right;
			mTransformedBoundingBox.right = left;			
		}
	}
	
	public void Release(){
		for(int i = 0; i < POIDrawings.size(); i++ ) {
			//POIDrawings.get(i).Release();
		}

		for(int i = 0; i < TrailDrawings.size(); i++ ) {
			//TrailDrawings.get(i).Release();
		}
		
		for(int i = 0; i < AreaDrawings.size(); i++ ) {
			//AreaDrawings.get(i).Release();
		}		

		POIDrawings.clear();
		TrailDrawings.clear();
		AreaDrawings.clear();
	}
	
	public void AddPOIDrawing(POIDrawing poiDrawing) {
		POIDrawings.add(poiDrawing);
	}
	
	public void AddTrailDrawing(TrailDrawing trailDrawing) {
		TrailDrawings.add(trailDrawing);
	}
	
	public void AddAreaDrawing(AreaDrawing areaDrawing) {
		AreaDrawings.add(areaDrawing);
	}		
	
	public static boolean IsMyInstance(Object object) {
		return object instanceof MapDrawings; 
	} 

	public RectF getLocationTransformerBoundingBox() {
		return mResortInfo.BoundingBox;
	}
	
	public void clearClosestItem() {
		for(POIDrawing poiDrawing : POIDrawings) {
			poiDrawing.mState = MapDrawings.State.NORMAL;
		}
	}
	
	public void DrawBackground(Canvas canvas, RectF geoRegionBoundary, double mapHeading, RenderSchemeManager rsm, Matrix drawingTransform, double viewScale, Resources res, boolean showAllNames, DrawingSet curDrawingSet) {  // draw all objects within bounds

		Log.e(TAG,"$$$$$$$$$$$$ MapDrawings.DrawBackground()");
		// draw various layers...

//		Log.e(TAG, "dBB: "+(int)geoRegionBoundary.left+","+(int)geoRegionBoundary.right+","+(int)geoRegionBoundary.top+","+(int)geoRegionBoundary.bottom);

		for(AreaDrawing areaDrawing : AreaDrawings) {
			if(curDrawingSet.mCancelLoad) return;
			RectF areaBB = areaDrawing.GetBoundingBox();
//			Log.e(TAG, "areaBB "+ areaDrawing.mArea.Name +": "+(int)areaBB.left+","+(int)areaBB.right+","+(int)areaBB.top+","+(int)areaBB.bottom);
			if(RectF.intersects(areaDrawing.GetBoundingBox(),geoRegionBoundary)) {
//				Log.i(TAG, "area: "+ areaDrawing.mArea.Name);
				areaDrawing.Draw(canvas, rsm, drawingTransform, viewScale);
			}
		}		

		for(TrailDrawing trailDrawing : TrailDrawings) {
			if(curDrawingSet.mCancelLoad) return;
			if(trailDrawing.mTrail.Type != Trail.SKI_LIFT) {
				if(RectF.intersects(trailDrawing.GetBoundingBox(),geoRegionBoundary)) {
//					Log.i(TAG,"trail "+trailDrawing.mTrail.Name);
					trailDrawing.Draw(canvas, mapHeading, rsm, drawingTransform, viewScale);
				}
			}
		}

		for(TrailDrawing trailDrawing : TrailDrawings) {
			if(curDrawingSet.mCancelLoad) return;
			if(trailDrawing.mTrail.Type == Trail.SKI_LIFT)	{	// draw lifts last
				RectF tBB = trailDrawing.GetBoundingBox();
				if(RectF.intersects(tBB, geoRegionBoundary)) {
//					Log.i(TAG,"lift "+trailDrawing.mTrail.Name);
					trailDrawing.Draw(canvas, mapHeading, rsm, drawingTransform, viewScale);
				}
			}
		}

		if(showAllNames) {
			for(TrailDrawing trailDrawing : TrailDrawings) {
				if(curDrawingSet.mCancelLoad) return;
				if(trailDrawing.mTrail.Type != Trail.SKI_LIFT) {
					if(RectF.intersects(trailDrawing.GetBoundingBox(),geoRegionBoundary)) {
						//					Log.e(TAG,"trail "+trailDrawing.mTrail.Name);
						trailDrawing.DrawNames(canvas, rsm, drawingTransform, viewScale);
					}
				}
			}
			for(TrailDrawing trailDrawing : TrailDrawings) {
				if(curDrawingSet.mCancelLoad) return;
				if(trailDrawing.mTrail.Type == Trail.SKI_LIFT)	{	// draw lifts last
					RectF tBB = trailDrawing.GetBoundingBox();
					if(RectF.intersects(tBB, geoRegionBoundary)) {
						trailDrawing.DrawNames(canvas, rsm, drawingTransform, viewScale);
					}
				}
			}
		}
	}
	
	public void LoadPOIArray(ArrayList<POIDrawing> poiArray, RectF viewPortBoundary, DrawingSet curDrawingSet) {  // load all objects within bounds into provided arraylist
		poiArray.clear();	// get rid of old pois
		for(POIDrawing poiDrawing : POIDrawings) {
			if(curDrawingSet.mCancelLoad) return;
			if(poiDrawing.mPoi.Type != POI.POI_TYPE_BUDDY)	{	// don't load buddies here
				if(viewPortBoundary.contains((float)poiDrawing.mLocation.x, (float)poiDrawing.mLocation.y)) {
					//			Log.e(TAG,"POI "+(int)(poiDrawing.mLocation.x)+", "+ (int)(poiDrawing.mLocation.y) +" : "+poiDrawing.mPoi.Name);
					poiArray.add(poiDrawing);

				}
			}
		}
	}

}
