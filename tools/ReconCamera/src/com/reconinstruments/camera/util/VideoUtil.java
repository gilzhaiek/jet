package com.reconinstruments.camera.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.reconinstruments.camera.R;
import com.reconinstruments.camera.ReconCamera;
import com.reconinstruments.camera.ui.ReconCameraSurfaceView;

/**
 * This is helper utility class for Video capture functionality
 * 
 * @author patrick@reconinstruments.com
 *
 */
public class VideoUtil {
	private static final String TAG = "VideoUtil";
	private static final boolean DEBUG = ReconCamera.DEBUG;
	
	public static File getDir() {
		if (DEBUG) Log.d(TAG, "getDir()");
		File sdDir = new File(Environment.getExternalStorageDirectory(), ReconCamera.Video.VIDEO_FILE_PATH);
		if (!sdDir.exists() && !sdDir.mkdirs()) {
			Log.e(TAG, "Can't create directory to save video.");
			return null;
		}
		return sdDir;
	}
	
	public static String getFileName() {
		if (DEBUG) Log.d(TAG, "getFileName()");
		SimpleDateFormat dateFormat = new SimpleDateFormat(ReconCamera.Video.VIDEO_FILE_DATE, ReconCamera.Video.VIDEO_FILE_LOCALE);
		String date = dateFormat.format(new Date());
		return File.separator + ReconCamera.Video.VIDEO_FILE_PREFIX + date + ReconCamera.Video.VIDEO_FILE_EXT_DEF;
	}
	
	public static File getVideoFile() {
		if (DEBUG) Log.d(TAG, "getVideoFile()");
		File videoFile = null;
		try {
			videoFile = new File(VideoUtil.getDir() + VideoUtil.getFileName());
		} catch (NullPointerException e){
			Log.e(TAG, "Return Video File Failed");
		}
		return videoFile;
	}
	
	public static MediaRecorder prepareMediaRecorder(Context context, Camera camera, MediaRecorder mediaRecorder, ReconCameraSurfaceView reconCameraSurfaceView){
		if (DEBUG) Log.d(TAG, "prepareMediaRecorder()");
        mediaRecorder = new MediaRecorder();

        camera.unlock();
        mediaRecorder.setCamera(camera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
        //mediaRecorder.setVideoSize(1280, 720);
        // FIXME HardCoded

        File mVideoFile = VideoUtil.getVideoFile();
        if (mVideoFile != null) {
        	mediaRecorder.setOutputFile(mVideoFile.getPath());
        } else {
        	Log.d(TAG, context.getString(R.string.error_retrieve_file));
        	Toast.makeText(context, R.string.error_retrieve_file, Toast.LENGTH_LONG).show();
        	VideoUtil.releaseMediaRecorder(camera, mediaRecorder);
        	return null;
        }
        
        mediaRecorder.setMaxDuration(ReconCamera.Video.MAX_CAPTURE_DURATION);
        mediaRecorder.setMaxFileSize(ReconCamera.Video.MAX_CAPTURE_SIZE);

        mediaRecorder.setPreviewDisplay(reconCameraSurfaceView.getHolder().getSurface());

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
        	Log.e(TAG, "IllegalStateException");
            VideoUtil.releaseMediaRecorder(camera, mediaRecorder);
            return null;
        } catch (IOException e) {
        	Log.e(TAG, "IOException");
            VideoUtil.releaseMediaRecorder(camera, mediaRecorder);
            return null;
        }
        return mediaRecorder;
        
    }
	
	public static MediaRecorder releaseMediaRecorder(Camera camera, MediaRecorder mediaRecorder){
    	if (DEBUG) Log.d(TAG, "releaseMediaRecorder");
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
        return mediaRecorder;
    }

}
