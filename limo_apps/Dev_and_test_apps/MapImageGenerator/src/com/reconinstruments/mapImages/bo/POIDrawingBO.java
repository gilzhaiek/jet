package com.reconinstruments.mapImages.bo;

import com.reconinstruments.mapImages.drawings.POIDrawing;
import com.reconinstruments.mapImages.helpers.LocationTransformer;
import com.reconinstruments.mapImages.objects.POI;
import com.reconinstruments.mapImages.objects.ResortInfo;
import com.reconinstruments.mapImages.prim.PointD;

public class POIDrawingBO {
	private LocationTransformer mLocationTransformer = null;
	
	public POIDrawingBO(LocationTransformer locationTransformer) {
		mLocationTransformer = locationTransformer;
	}
		
	public POIDrawing CreateTransformedPOIDrawing(POI poi, ResortInfo resortInfo) {
		double riBBBottom = mLocationTransformer.TransformLatitude(resortInfo.BoundingBox.bottom, resortInfo.BoundingBox.left);

		PointD location = new PointD(	mLocationTransformer.TransformLongitude(poi.Location.y, poi.Location.x),
										riBBBottom - mLocationTransformer.TransformLatitude(poi.Location.y, poi.Location.x)); 		// graphics drawing coordinate system, ie, y positive down
		POIDrawing poiDrawing = new POIDrawing(location, poi);
		
		return poiDrawing;
	}	
}
