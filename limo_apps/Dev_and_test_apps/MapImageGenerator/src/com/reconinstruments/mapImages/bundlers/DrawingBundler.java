package com.reconinstruments.mapImages.bundlers;

import android.os.Bundle;

import com.reconinstruments.mapImages.drawings.AreaDrawing;
import com.reconinstruments.mapImages.drawings.POIDrawing;
import com.reconinstruments.mapImages.drawings.TrailDrawing;
import com.reconinstruments.mapImages.objects.Area;
import com.reconinstruments.mapImages.objects.POI;
import com.reconinstruments.mapImages.objects.Trail;

public class DrawingBundler extends BundlerBase {
	// POI
	public static POIDrawing GeneratePOIDrawing(Bundle bundle){
		return new POIDrawing(
				GetPointD("DrawingLocation", bundle),
				new POI(bundle.getInt("Type"),
						bundle.getString("Name"),
						GetPointD("Location", bundle)));
	}

	public static Bundle GeneratePOIBundle(POIDrawing poiDrawing){
		Bundle bundle = new Bundle();
		
		AddPointD("DrawingLocation", bundle, poiDrawing.mLocation);
		bundle.putInt("Type", poiDrawing.mPoi.Type);
		bundle.putString("Name", poiDrawing.mPoi.Name);
		AddPointD("Location", bundle, poiDrawing.mPoi.Location);
		bundle.putInt("Type", poiDrawing.mPoi.Type);
		
		return bundle;
	}
	
	// Trail
	public static TrailDrawing GenerateTrailDrawing(Bundle bundle){
		return new TrailDrawing(GetArrayPointD(bundle.getBundle("PathPoints")),
			new Trail(bundle.getInt("Type"),
					bundle.getString("Name"), 
					null,
					bundle.getInt("SpeedLimit"),
					bundle.getBoolean("IsOneWay")));
	}
		
	public static Bundle GenerateTrailBundle(TrailDrawing trailDrawing){
		Bundle bundle = new Bundle();
		
		bundle.putInt("Type", trailDrawing.mTrail.Type);
		bundle.putString("Name", trailDrawing.mTrail.Name);
		bundle.putInt("SpeedLimit", trailDrawing.mTrail.SpeedLimit);
		bundle.putBoolean("IsOneWay", trailDrawing.mTrail.IsOneWay);
		bundle.putBundle("PathPoints", GenerateArrayPointDBundle(trailDrawing.mPolylinePoints));
		
		return bundle;
	}
	
	// Area
	public static AreaDrawing GenerateAreaDrawing(Bundle bundle){
		return new AreaDrawing(GetArrayPointD(bundle.getBundle("PathPoints")),
			new Area(bundle.getInt("Type"),
					bundle.getString("Name"),
					null ));
	}
		
	public static Bundle GenerateAreaBundle(AreaDrawing areaDrawing){
		Bundle bundle = new Bundle();
		
		bundle.putInt("Type", areaDrawing.mArea.Type);
		bundle.putString("Name", areaDrawing.mArea.Name);
		bundle.putBundle("PathPoints", GenerateArrayPointDBundle(areaDrawing.mPolylinePoints));
		
		return bundle;
	}
}
