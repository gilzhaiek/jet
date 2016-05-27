package com.reconinstruments.camera.util;

import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.camera.ReconCamera;
import com.reconinstruments.camera.ui.ReconCameraSurfaceView;

public class CameraUtil {
	private static final String TAG = "CameraUtil";
	private static final boolean DEBUG = ReconCamera.DEBUG;

	public static void resetCameraViewToDefault(Context context, Camera camera, ReconCameraSurfaceView surfaceView) {
		if (DEBUG) Log.d(TAG, "resetCameraViewToDefault()");

		if (camera == null) {
			Log.d(TAG, "Camera instance is null");
			return;
		}

		// stop preview before making changes
		try {
			camera.stopPreview();
		} catch (Exception e){
			// ignore: tried to stop a non-existent preview
		}

		// make any resize, rotate or reformatting changes here
		Parameters param = camera.getParameters();

		List<Camera.Size> sizes = param.getSupportedPictureSizes();
		// TODO find the size index from Pref, if not found, set to default
		param.setPictureSize(sizes.get(1).width, sizes.get(1).height);

		// Set the Optiomal Preview Size according to the screenSize
		List<Camera.Size> previewSizes = param.getSupportedPreviewSizes(); 
		Size optimalSize = CameraUtil.getOptimalPreviewSize(
				previewSizes,
				context.getResources().getDisplayMetrics().widthPixels,
				context.getResources().getDisplayMetrics().heightPixels
				);
		
		if (DEBUG) Log.d(TAG, "optimalSize width: " + optimalSize.width + " height: " + optimalSize.height);
		param.setPreviewSize( optimalSize.width, optimalSize.height );

		if (param.isAutoExposureLockSupported()) {
			param.setAutoExposureLock(true);
		} else {
			if (DEBUG) Log.d(TAG, "AutoExposureLock Not Supported");
		}
		if (param.isAutoWhiteBalanceLockSupported()) {
			param.setAutoWhiteBalanceLock(true);
		} else {
			if (DEBUG) Log.d(TAG, "AutoWhiteBalaceLock Not Supported");
		}
		param.setPictureFormat(ImageFormat.JPEG);

		camera.setParameters(param);

		// start preview with new settings
		try {
			camera.setPreviewDisplay(surfaceView.getHolder());
		} catch (Exception e){
			if (DEBUG) Log.e(TAG, "setPreviewDisplay() failed");
		}
		try {
			camera.autoFocus(surfaceView);
		} catch (Exception e){
			if (DEBUG) Log.e(TAG, "autoFocus() failed");
		}

	}

	public static Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) w/h;
		if (DEBUG) Log.d(TAG, "aspect Ratio : " + targetRatio);

		if (sizes==null) return null;

		Size optimalSize = null;

		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Find size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}


	public static void zoomIn(Camera camera) {
		Camera.Parameters params = camera.getParameters();
		if (params.isZoomSupported()) {
			int currentZoom = params.getZoom();
			if (currentZoom < params.getMaxZoom()) {
				currentZoom++;
			}
			camera.getParameters().setZoom(currentZoom);
		}
	}

	public static void zoomOut(Camera camera) {
		Camera.Parameters params = camera.getParameters();
		if (params.isZoomSupported()) {
			int currentZoom = params.getZoom();
			if (currentZoom > 0) {
				currentZoom--;
			}			
			camera.getParameters().setZoom(currentZoom);
		}
	}



	/**
	 * Get Camera Instance ( Primary Camera )
	 * @return
	 */
	public static Camera getCameraInstance(Context context){
		if (DEBUG) Log.d(TAG, "getCameraInstance()");
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		}
		catch (Exception e){
			if (DEBUG) Log.d(TAG, "Camera is not available or doesn't exist");
			Toast.makeText(context, "Fail to get Camera", Toast.LENGTH_LONG).show();

		}
		return c; // returns null if camera is unavailable
	}

	public static Camera releaseCamera(Camera camera){
		if (DEBUG) Log.d(TAG, "releaseCamera");
		if (camera != null){
			if (DEBUG) Log.d(TAG, "release and set null to camera");
			camera.release();        // release the camera for other applications
			camera = null;
		}
		return camera;
	}

}
