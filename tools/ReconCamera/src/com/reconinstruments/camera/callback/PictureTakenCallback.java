package com.reconinstruments.camera.callback;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.camera.ReconCamera;
import com.reconinstruments.camera.util.PhotoUtil;

public class PictureTakenCallback implements PictureCallback {
	private static final String TAG = "PictureTakenCallback";
	private static final boolean DEBUG = ReconCamera.DEBUG;

	private final Context context;

	public PictureTakenCallback(Context context) {
		this.context = context;
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		File mPictureSaveDir = PhotoUtil.getDir();

		if (!mPictureSaveDir.exists() && !mPictureSaveDir.mkdirs()) {
			Log.d(TAG, "Can't create directory to save image.");
			Toast.makeText(context, "Can't create directory to save image.",
					Toast.LENGTH_LONG).show();
			return;
		}

		File pictureFile = PhotoUtil.getPhotoFile();

		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			fos.write(data);
			fos.close();
			Toast.makeText(context, "New Image saved:" + pictureFile,
					Toast.LENGTH_LONG).show();
		} catch (Exception error) {
			Log.d(TAG, "File" + pictureFile!=null ? pictureFile.getPath():"file" + "not saved: " + error.getMessage());
			Toast.makeText(context, "Image could not be saved.",
					Toast.LENGTH_LONG).show();
		} finally {
			camera.startPreview();
		}
		
	}

}
