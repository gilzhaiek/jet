package com.reconinstruments.dashlauncher.radar.maps.drawings;

import com.reconinstruments.dashlauncher.radar.maps.objects.POI;
import com.reconinstruments.dashlauncher.radar.prim.PointD;

public class POIDrawing {
	public POI		mPoi;
	public PointD	mLocation;
	
	public POIDrawing(PointD location, POI poi) {
		mPoi = poi;
		mLocation = location;
	}
		
	public void Release(){
		mLocation = null;
		mPoi = null;
	}
}
