package com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.cameraposition;

import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;


public class DynamicCameraPositionInterface {
	public interface IDynamicCameraPosition {
		public void SetCameraPosition(CameraViewport camera);		
		public void SetCameraPosition(CameraViewport camera, boolean forceUpdate);		
		public void SetCameraHeading(float heading);
		public void SetCameraPitch(float pitch);
	}
}
