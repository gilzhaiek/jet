package com.reconinstruments.dashlauncher.radar.maps.bundlers;

import com.reconinstruments.dashlauncher.radar.maps.drawings.POIDrawing;
import com.reconinstruments.dashlauncher.radar.maps.drawings.TrailDrawing;
import com.reconinstruments.dashlauncher.radar.maps.objects.POI;
import com.reconinstruments.dashlauncher.radar.maps.objects.Trail;

import android.os.Bundle;

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
		bundle.putBundle("PathPoints", GenerateArrayPointDBundle(trailDrawing.mPathPoints));
		
		return bundle;
	}
}
