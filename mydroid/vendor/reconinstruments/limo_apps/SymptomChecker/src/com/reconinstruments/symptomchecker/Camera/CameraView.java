package com.reconinstruments.symptomchecker.Camera;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView implements SurfaceHolder.Callback{

	private SurfaceHolder surfaceHolder = null;
	private Camera camera = null;
	private Context mContext;

	@SuppressWarnings("deprecation")
	public CameraView(Context context, SurfaceView cameraSurfaceView) {
		mContext = context;
		surfaceHolder = cameraSurfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			camera = Camera.open();
		} catch (Exception e){
			camera = null;
			return;
		}
		// Show the Camera display
		try {
			camera.setPreviewDisplay(holder);
		} catch (Exception e) {
			this.releaseCamera();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) {
		// Start the preview for surfaceChanged
		if (camera != null) {
			camera.startPreview();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Do not hold the camera during surfaceDestroyed - view should be gone
		this.releaseCamera();
	}

	/**
	 * Release the camera from use
	 */
	public void releaseCamera() {
		if (camera != null) {
			camera.release();
			camera = null;
		}
	}

	public boolean TakePicture(String filePath) {
		if(camera != null) {
			camera.takePicture(null, null, new PhotoHandler(mContext, filePath));
			camera.startPreview();
			return true;
		}
		return false;
		
	}
}
