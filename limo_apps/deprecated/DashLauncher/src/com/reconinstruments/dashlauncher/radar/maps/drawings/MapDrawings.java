package com.reconinstruments.dashlauncher.radar.maps.drawings;

import java.util.ArrayList;

import com.reconinstruments.dashlauncher.radar.maps.objects.ResortInfo;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

public class MapDrawings {
	private final static String TAG = "MapDrawings";
	
	public RectF mTransformedBoundingBox = null;
	
	public ResortInfo mResortInfo = null;
	
	public ArrayList<POIDrawing>	POIDrawings = null;
	public ArrayList<TrailDrawing>	TrailDrawings = null;
	public ArrayList<AreaDrawing>	AreaDrawings = null;
	
	public MapDrawings(){		
		POIDrawings = new ArrayList<POIDrawing>();
		TrailDrawings = new ArrayList<TrailDrawing>();
		AreaDrawings = new ArrayList<AreaDrawing>();
		mTransformedBoundingBox = new RectF();
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
			AreaDrawings.get(i).Release();
		}		

		POIDrawings.clear();
		TrailDrawings.clear();
		AreaDrawings.clear();
	}
	
	public void AddPOIDrawing(POIDrawing POIDrawing) {
		POIDrawings.add(POIDrawing);
	}
	
	public void AddTrailDrawing(TrailDrawing TrailDrawing) {
		TrailDrawings.add(TrailDrawing);
	}
	
	public void AddAreaDrawing(AreaDrawing AreaDrawing) {
		AreaDrawings.add(AreaDrawing);
	}		
	
	public static boolean IsMyInstance(Object object) {
		return object instanceof MapDrawings; 
	}
}
