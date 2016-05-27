package com.reconinstruments.camera.util;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

import com.reconinstruments.camera.ReconCamera;

/**
 * This is helper utility class for Photo related functionality
 * 
 * @author patrick@reconinstruments.com
 *
 */
public class PhotoUtil {
	private static final String TAG = "PhotoUtil";
	private static final boolean DEBUG = ReconCamera.DEBUG;
	
	public static File getDir() {
		if (DEBUG) Log.d(TAG, "getDir()");
		File sdDir = new File(Environment.getExternalStorageDirectory(), ReconCamera.Photo.PHOTO_FILE_PATH);
		if (!sdDir.exists() && !sdDir.mkdirs()) {
			Log.e(TAG, "Can't create directory to save photo.");
			return null;
		}
		return sdDir;
	}
	
	public static String getFileName() { // TODO if there is same file taken in same second -> add _1 _2 postfix
		if (DEBUG) Log.d(TAG, "getFileName()");
		SimpleDateFormat dateFormat = new SimpleDateFormat(ReconCamera.Photo.PHOTO_FILE_DATE, ReconCamera.Photo.PHOTO_FILE_LOCALE);
		String date = dateFormat.format(new Date());
		return File.separator + ReconCamera.Photo.PHOTO_FILE_PREFIX + date + ReconCamera.Photo.PHOTO_FILE_EXT_DEF;
	}
	
	public static File getPhotoFile() {
		if (DEBUG) Log.d(TAG, "getPhotoFile()");
		File photoFile = null;
		try {
			photoFile = new File(PhotoUtil.getDir() + PhotoUtil.getFileName());
		} catch (NullPointerException e){
			Log.e(TAG, "Return Photo File Failed");
		}
		return photoFile;
	}
	
	public static File[] getPhotoFiles() {
		if (DEBUG) Log.d(TAG, "getPhotoUris()");
		
		File photoDir = PhotoUtil.getDir();
		if (photoDir == null) return null;
		
		File[] photoFiles = photoDir.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.toLowerCase(ReconCamera.Photo.PHOTO_FILE_LOCALE).endsWith(ReconCamera.Photo.PHOTO_FILE_EXT_DEF);
		    }
		});
		
		return photoFiles;
	}
}
