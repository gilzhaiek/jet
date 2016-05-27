package com.reconinstruments.dashlauncher.radar;

import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

public class CompassViewControllerHelper extends BaseControllerHelper {
	private static final String TAG = "CompassViewControllerHelper";
	
		public CompassViewControllerHelper(Activity activity, LocationTransformer locationTransformer) {
		super(activity, new CompassViewRendererHelper((Context)activity), locationTransformer);
	}
	
	@Override
	public void OnLocationHeadingChanged(Location location, float yaw, float pitch, boolean gpsYaw) {
		super.OnLocationHeadingChanged(location, yaw, pitch, gpsYaw);
		
		((CompassViewRendererHelper)mRendererHelper).OnLocationHeadingChanged(location, yaw, pitch, gpsYaw);
		
		mRendererHandler.RedrawScene();
	}	
}
