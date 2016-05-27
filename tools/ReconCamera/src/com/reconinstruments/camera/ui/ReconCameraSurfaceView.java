package com.reconinstruments.camera.ui;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.reconinstruments.camera.ReconCamera;
import com.reconinstruments.camera.util.CameraUtil;

public class ReconCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, AutoFocusCallback{
	private static final String TAG = "ReconCameraSurfaceView";
	private static final boolean DEBUG = ReconCamera.DEBUG;

	private SurfaceHolder mHolder;
	private Camera mCamera;
	private Context mContext;

	public ReconCameraSurfaceView(Context context, Camera camera) {
		super(context);
		if (DEBUG) Log.d(TAG, "ReconCameraSurfaceView()");
		mContext = context;
		mCamera = camera;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (DEBUG) Log.d(TAG, "surfaceChanged()");
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (holder.getSurface() == null){
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e){
			// ignore: tried to stop a non-existent preview
		}

		// make any resize, rotate or reformatting changes here
		Parameters param = mCamera.getParameters();


		//		List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
		//		Size optimalSize = CameraUtil.getOptimalPreviewSize(previewSizes, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
		//		parameters.setPreviewSize(optimalSize.width, optimalSize.height);
		//		parameters.setFlashMode(Parameters.FLASH_MODE_AUTO); // Not Supported for JET
		//		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); // Not Supported for JET

		List<Camera.Size> sizes = param.getSupportedPictureSizes();
		// TODO find the size index from Pref, if not found, set to default
		param.setPictureSize(sizes.get(1).width, sizes.get(1).height);
		
//		Camera.Size s = param.getSupportedPreviewSizes().get(10);
//		param.setPreviewSize( s.width, s.height );
		
		List<Camera.Size> previewSizes = param.getSupportedPreviewSizes();
		// TODO set to optimal size for preview to decrease overhead to cpu
		Size optimalSize = CameraUtil.getOptimalPreviewSize(previewSizes, width, height); 
//				getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
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

		mCamera.setParameters(param);

//		Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
//		if(display.getRotation() == Surface.ROTATION_0) {                        
//			mCamera.setDisplayOrientation(90);
//		} else if(display.getRotation() == Surface.ROTATION_270) {
//			mCamera.setDisplayOrientation(180);
//		}

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.autoFocus(this);
		} catch (Exception e){
		}

		mCamera.startPreview();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (DEBUG) Log.d(TAG, "surfaceCreated()");

		// The Surface has been created, now tell the camera where to draw the preview.
		try {
			mCamera.setPreviewDisplay(holder);
			if (DEBUG) Log.d(TAG, "setPreviewDisplay()");
			mCamera.startPreview();
			if (DEBUG) Log.d(TAG, "startPreview()");
		} catch (IOException e) {
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (DEBUG) Log.d(TAG, "surfaceDestroyed()");
	}

	public void setCamera(Camera camera) {
		if (DEBUG) Log.d(TAG, "setCamera()");
		mCamera = camera;
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if (DEBUG) Log.d(TAG, "onAutoFocus");
		if (DEBUG) Log.d(TAG, String.format("AutoFocus CallBack was a %s",success));
	}
}
