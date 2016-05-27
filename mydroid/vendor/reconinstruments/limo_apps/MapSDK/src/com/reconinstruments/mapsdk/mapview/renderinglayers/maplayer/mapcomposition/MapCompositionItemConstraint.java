package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition;

import android.util.Log;

public class MapCompositionItemConstraint {
	private static final String TAG = "MapCompositionItemConstraint";
	float scaleMax = 9999999999.f;
	float scaleMin = 0.0f;
	
	public void SetScaleMax(float max) {
		scaleMax = max;
	}
	public void SetScaleMin(float min) {
		scaleMin = min;
	}
	
	public boolean IncludeInDrawing(float scale) {
//		Log.e(TAG, "IncludeInDrawing:" + scaleMin + "-" + scale + "-" + scaleMax);
		return (scale > scaleMin && scale <= scaleMax);
	}
}
