package com.reconinstruments.symptomchecker.Camera;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class PhotoHandler implements PictureCallback{
	private final static String TAG = "PhotoHandler";
	private final Context context;
	private String mFilePath;

	public PhotoHandler(Context context, String filePath) {
		this.context = context;
		this.mFilePath = filePath;
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		File pictureFile;
		if(mFilePath != null && !mFilePath.isEmpty()){
			pictureFile = new File(mFilePath);
			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
				Toast.makeText(context, "New Image saved:" + mFilePath, Toast.LENGTH_LONG).show();
			} catch (Exception error) {
				Log.e(TAG, "File" + mFilePath + "not saved: " + error.getMessage());
				Toast.makeText(context, "Image could not be saved.", Toast.LENGTH_LONG).show();
			}
		}
		else {
			File pictureFileDir = getDir();
			if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
				Log.d(TAG, "Can't create directory to save image.");
				Toast.makeText(context, "Can't create directory to save image.", Toast.LENGTH_LONG).show();
				return;
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
			String date = dateFormat.format(new Date());
			String photoFile = "Picture_" + date + ".jpg";

			String filename = pictureFileDir.getPath() + File.separator + photoFile;
			pictureFile = new File(filename);
			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
				Toast.makeText(context, "New Image saved:" + photoFile, Toast.LENGTH_LONG).show();
			} catch (Exception error) {
				Log.e(TAG, "File" + filename + "not saved: " + error.getMessage());
				Toast.makeText(context, "Image could not be saved.", Toast.LENGTH_LONG).show();
			}
		}
	}

	private File getDir() {
		File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		return sdDir;
	}
}
