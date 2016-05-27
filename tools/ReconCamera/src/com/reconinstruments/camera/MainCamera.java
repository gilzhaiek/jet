package com.reconinstruments.camera;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.reconinstruments.camera.callback.PictureTakenCallback;
import com.reconinstruments.camera.ui.ReconCameraSurfaceView;
import com.reconinstruments.camera.util.CameraUtil;
import com.reconinstruments.camera.util.VideoUtil;
import com.reconinstruments.gallery.Gallery;

public class MainCamera extends Activity implements View.OnClickListener {
	private static final String TAG = "MainCamera";
	private static final boolean DEBUG = ReconCamera.DEBUG;

	private Camera mCamera;
	private ReconCameraSurfaceView mReconCameraSurfaceView;
	private MediaRecorder mMediaRecorder;

	private Button mRecordButton;
	private Button mCaptureButton;
	private Button mGalleryButton;
	private boolean recording = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.d(TAG, "onCreate()");

		setContentView(R.layout.activity_main);

		//Get Camera for preview
		mCamera = CameraUtil.getCameraInstance(this);

		mReconCameraSurfaceView = new ReconCameraSurfaceView(this, mCamera);
		FrameLayout myCameraPreview = (FrameLayout)findViewById(R.id.videoview);
		myCameraPreview.addView(mReconCameraSurfaceView);

		mRecordButton = (Button)findViewById(R.id.main_record);
		mRecordButton.setOnClickListener(this);
		mCaptureButton = (Button)findViewById(R.id.main_capture);
		mCaptureButton.setOnClickListener(this);
		mGalleryButton = (Button)findViewById(R.id.main_gallery);
		mGalleryButton.setOnClickListener(this); 
	}



	@Override
	protected void onResume() {
		super.onResume();
		if (DEBUG) Log.d(TAG, "onResume()");
		// Get Camera Again if mCamera is Null
		if (mCamera == null) {
			if (DEBUG) Log.d(TAG, "mCamera is null");
			mCamera = CameraUtil.getCameraInstance(this);
			mReconCameraSurfaceView.setCamera(mCamera);
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (DEBUG) Log.d(TAG, "onPause()");
		mMediaRecorder = VideoUtil.releaseMediaRecorder(mCamera, mMediaRecorder);		// if you are using MediaRecorder, release it first
		mCamera = CameraUtil.releaseCamera(mCamera);					// release the camera immediately on pause event
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (DEBUG) Log.d(TAG, "onDestroy()");
	}



	@Override
	protected void onPostResume() {
		super.onPostResume();
		if (DEBUG) Log.d(TAG, "onPostResume()");
	}



	@Override
	protected void onRestart() {
		super.onRestart();
		if (DEBUG) Log.d(TAG, "onRestart()");
	}



	@Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.d(TAG, "onStart()");
	}



	@Override
	protected void onStop() {
		super.onStop();
		if (DEBUG) Log.d(TAG, "onStop()");
	}



	@Override
	public void onClick(View v) {
		if (DEBUG) Log.d(TAG, "onClick()");
		switch (v.getId()) {

		case R.id.main_capture:
			if (DEBUG) Log.d(TAG, "R.id.main_capture clicked");
			mCamera.takePicture(null, null, null, new PictureTakenCallback(this));
			break;

		case R.id.main_record:
			if (DEBUG) Log.d(TAG, "R.id.main_record clicked");
			if (recording) {
				if (DEBUG) Log.d(TAG, "Stop Recording");
				// stop recording and release camera
				mMediaRecorder.stop();  // stop the recording
				mMediaRecorder = VideoUtil.releaseMediaRecorder(mCamera, mMediaRecorder); // release the MediaRecorder object
				recording = false;
				mRecordButton.setText(getString(R.string.record));
				mCamera = CameraUtil.releaseCamera(mCamera);
				mCamera = CameraUtil.getCameraInstance(this);
				mCamera.stopPreview();
				CameraUtil.resetCameraViewToDefault(this, mCamera, mReconCameraSurfaceView);
				mCamera.startPreview();

			} else {
				if (DEBUG) Log.d(TAG, "Start recording");

				//Release Camera before MediaRecorder start
				mCamera = CameraUtil.releaseCamera(mCamera);
				mCamera = CameraUtil.getCameraInstance(this);
				mMediaRecorder = VideoUtil.prepareMediaRecorder(this, mCamera, mMediaRecorder, mReconCameraSurfaceView);
				if (mMediaRecorder == null){
					Toast.makeText(MainCamera.this,"Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
					finish();
				}
				
				mMediaRecorder.start();
				recording = true;
				mRecordButton.setText(getString(R.string.record_stop));
			}
			break;
//		case R.id.main_zoom_in:
//			if (DEBUG) Log.d(TAG, "R.id.main_zoom_in clicked");
//			CameraUtil.zoomIn(mCamera);
//			break;
//		case R.id.main_zoom_out:
//			if (DEBUG) Log.d(TAG, "R.id.main_zoom_out clicked");
//			CameraUtil.zoomOut(mCamera);
//			break;
		case R.id.main_gallery:
			if (DEBUG) Log.d(TAG, "R.id.main_gallery clicked");
			startActivity(new Intent(this, Gallery.class));
			break;
		}

	}

}