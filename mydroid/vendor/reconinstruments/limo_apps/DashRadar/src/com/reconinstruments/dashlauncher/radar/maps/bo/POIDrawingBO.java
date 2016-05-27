package com.reconinstruments.dashlauncher.radar.maps.bo;

import com.reconinstruments.dashlauncher.radar.maps.drawings.POIDrawing;
import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;
import com.reconinstruments.dashlauncher.radar.maps.objects.POI;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortInfo;
import com.reconinstruments.dashlauncher.radar.prim.PointD;

public class POIDrawingBO {
	private LocationTransformer mLocationTransformer = null;
	
	public POIDrawingBO(LocationTransformer locationTransformer) {
		mLocationTransformer = locationTransformer;
	}
		
	public POIDrawing CreateTransformedPOIDrawing(POI poi, ResortInfo resortInfo) {
		PointD location = new PointD(	mLocationTransformer.TransformLongitude(poi.Location.x),
										mLocationTransformer.TransformLatitude(poi.Location.y)); 
		POIDrawing poiDrawing = new POIDrawing(location, poi);
		
		return poiDrawing;
	}	
}
