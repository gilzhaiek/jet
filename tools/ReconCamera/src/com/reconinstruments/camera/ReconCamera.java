package com.reconinstruments.camera;

import java.util.Locale;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.reconinstruments.camera.util.PhotoUtil;

public class ReconCamera extends Application {
	private static final String TAG =	"ReconCamera";
	public static final boolean DEBUG = true;

	/**
	 * This is collection of constants used for all Video Related functionalities 
	 * 
	 * @author patrick@reconinstruments.com
	 *
	 */
	public static final class Video {

		/*//////////////////////////////////////////////////////////////////////
			File Related Constants
		//////////////////////////////////////////////////////////////////////*/

		/**
		 * Path of the directory for video files
		 */
		public static final String VIDEO_FILE_PATH	= "/ReconCamera/Video";

		/**
		 * Prefix for the name of video file
		 */
		public static final String VIDEO_FILE_PREFIX	= "Video_";

		/**
		 * Date format of video file
		 */
		public static final String VIDEO_FILE_DATE	= "yyyy_MM_dd_HH_mm_ss";

		/**
		 * Locale for the date of video file
		 */
		public static final Locale VIDEO_FILE_LOCALE	= Locale.US;

		/**
		 * Default File extension for video files
		 */
		public static final String VIDEO_FILE_EXT_DEF	= ReconCamera.Video.VIDEO_FILE_EXT_MP4; 

		/**
		 * MP4 File extension for video files
		 */
		public static final String VIDEO_FILE_EXT_MP4	= ".mp4";

		/*//////////////////////////////////////////////////////////////////////
			Video Capture Related Constants
		//////////////////////////////////////////////////////////////////////*/

		/**
		 * Set negative or 0 for Unlimited Capture Duration in ms
		 */
		public static final int MAX_CAPTURE_DURATION	= 60000; // 60 Seconds

		/**
		 * Set negative or 0 for Unlimited Capture Size in bytes
		 */
		public static final long MAX_CAPTURE_SIZE		= 500000000; // Approx 500MB

	}

	/**
	 * This is collection of constants used for all Photo related functionalities 
	 * 
	 * @author patrick@reconinstruments.com
	 *
	 */
	public static final class Photo {
		/*//////////////////////////////////////////////////////////////////////
			File Related Constants
		//////////////////////////////////////////////////////////////////////*/

		/**
		 * Path of the directory for video files
		 */
		public static final String PHOTO_FILE_PATH	= "/ReconCamera/Photo";

		/**
		 * Prefix for the name of video file
		 */
		public static final String PHOTO_FILE_PREFIX	= "Photo_";

		/**
		 * Date format of video file
		 */
		public static final String PHOTO_FILE_DATE	= "yyyy_MM_dd_HH_mm_ss_SSS";

		/**
		 * Locale for the date of video file
		 */
		public static final Locale PHOTO_FILE_LOCALE	= Locale.US;

		/**
		 * Default File extension for video files
		 */
		public static final String PHOTO_FILE_EXT_DEF	= ReconCamera.Photo.PHOTO_FILE_EXT_JPG; 

		/**
		 * MP4 File extension for video files
		 */
		public static final String PHOTO_FILE_EXT_JPG	= ".jpg";

		/*//////////////////////////////////////////////////////////////////////
			Photo Capture Related Constants TODO
		//////////////////////////////////////////////////////////////////////*/

	}

	public static final class Gallery {
		public static final String EXTRA_PICTURE_POSITION = "PICTURE_POSITION";
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (DEBUG) Log.d(TAG, "onCreate()");

		initUniversalImageLoader(getApplicationContext());
	}

	/**
	 * This configuration tuning is custom. You can tune every option, you may tune some of them,
	 * or you can create default configuration by
	 * ImageLoaderConfiguration.createDefault(this) method.
	 *  
	 * @param context
	 */
	public static void initUniversalImageLoader(Context context) {
		if (DEBUG) Log.d(TAG, "Initiating Universal Image Loader");

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
		.threadPriority(Thread.NORM_PRIORITY - 2)
		.denyCacheImageMultipleSizesInMemory()
		.discCacheFileNameGenerator(new Md5FileNameGenerator())
		.tasksProcessingOrder(QueueProcessingType.LIFO)
		.writeDebugLogs() // Remove for release app
		.build();
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);

	}


}
