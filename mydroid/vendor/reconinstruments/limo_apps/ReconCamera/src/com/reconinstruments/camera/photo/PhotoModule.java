/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reconinstruments.camera.photo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.CameraProfile;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;

import com.reconinstruments.camera.CameraErrorCallback;
import com.reconinstruments.camera.CameraHolder;
import com.reconinstruments.camera.CameraModule;
import com.reconinstruments.camera.CameraSettings;
import com.reconinstruments.camera.Exif;
import com.reconinstruments.camera.OnPreviewChangedListener;
import com.reconinstruments.camera.R;
import com.reconinstruments.camera.app.CameraActivity;
import com.reconinstruments.camera.app.MediaSaveService;
import com.reconinstruments.camera.data.LocationManager;
import com.reconinstruments.camera.exif.ExifInterface;
import com.reconinstruments.camera.manager.CameraManager.CameraPictureCallback;
import com.reconinstruments.camera.manager.CameraManager.CameraProxy;
import com.reconinstruments.camera.manager.CameraManager.CameraShutterCallback;
import com.reconinstruments.camera.photo.PhotoModule.NamedImages.NamedEntity;
import com.reconinstruments.camera.preference.ComboPreferences;
import com.reconinstruments.camera.preference.ListPreference;
import com.reconinstruments.camera.preference.PreferenceGroup;
import com.reconinstruments.camera.preference.RecordLocationPreference;
import com.reconinstruments.camera.ui.CountDownView.OnCountDownFinishedListener;
import com.reconinstruments.camera.util.ApiHelper;
import com.reconinstruments.camera.util.CameraUtil;
import com.reconinstruments.camera.util.StorageUtil;
import com.reconinstruments.camera.util.UsageStatistics;


public class PhotoModule
        implements CameraModule,
        PhotoController,
        MediaSaveService.Listener,
        OnCountDownFinishedListener {

    private static final String TAG = "CAM_PhotoModule";
    private static final boolean DEBUG = false;
    // We number the request code from 1000 to avoid collision with Gallery.
    private static final int REQUEST_CROP = 1000;

    private static final int SETUP_PREVIEW = 1;
    private static final int FIRST_TIME_INIT = 2;
    private static final int CLEAR_SCREEN_DELAY = 3;
    private static final int SET_CAMERA_PARAMETERS_WHEN_IDLE = 4;
    private static final int SWITCH_CAMERA = 6;
    private static final int SWITCH_CAMERA_START_ANIMATION = 7;
    private static final int CAMERA_OPEN_DONE = 8;
    private static final int OPEN_CAMERA_FAIL = 9;
    private static final int CAMERA_DISABLED = 10;
    private static final int SWITCH_TO_GCAM_MODULE = 11;

    // The subset of parameters we need to update in setCameraParameters().
    private static final int UPDATE_PARAM_INITIALIZE = 1;
    private static final int UPDATE_PARAM_ZOOM = 2;
    private static final int UPDATE_PARAM_PREFERENCE = 4;
    private static final int UPDATE_PARAM_ALL = -1;

    // This is the delay before we execute onResume tasks when coming
    // from the lock screen, to allow time for onPause to execute.
    private static final int ON_RESUME_TASKS_DELAY_MSEC = 20;

    private static final String DEBUG_IMAGE_PREFIX = "DEBUG_";

    private int mFrames_Thousands = 10000; //Frames per 1000 seconds
    // copied from Camera hierarchy
    private CameraActivity mActivity;
    private CameraProxy mCameraDevice;
    private int mCameraId;
    private Parameters mParameters;
    private boolean mPaused;
    
    private int mPreviewWidth = 0, mPreviewHeight = 0;

    private PhotoUI mUI;

    private boolean mShouldShowInstructions = true;

    // The activity is going to switch to the specified camera id. This is
    // needed because texture copy is done in GL thread. -1 means camera is not
    // switching.
    protected int mPendingSwitchCameraId = -1;
    private boolean mOpenCameraFail;
    private boolean mCameraDisabled;

    /**
     * this is used to control the availability of camera for time-consuming 
     * operations ( eg. ImageStabilization, HDR )
     */
    private boolean mCanTakePhoto = true;

    // When setCameraParametersWhenIdle() is called, we accumulate the subsets
    // needed to be updated in mUpdateSet.
    private int mUpdateSet;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;

    private Parameters mInitialParams;
    private boolean mAeLockSupported;
    
    private boolean mContinuousFocusSupported;

    private ComboPreferences mPreferences;

    private static final String sTempCropFilename = "crop-temp";

    private ContentProviderClient mMediaProviderClient;

    // mCropValue and mSaveUri are used only if isImageCaptureIntent() is true.
    private String mCropValue;
    private Uri mSaveUri;

    private Uri mDebugUri;

    // We use a queue to generated names of the images to be used later
    // when the image is ready to be saved.
    private NamedImages mNamedImages;

    private Runnable mDoSnapRunnable = new Runnable() {
        @Override
        public void run() {
            takePhoto();
        }
    };
    
    /**
     * An unpublished intent flag requesting to return as soon as capturing
     * is completed.
     *
     * TODO: consider publishing by moving into MediaStore.
     */
    private static final String EXTRA_QUICK_CAPTURE =
            "android.intent.extra.quickCapture";

    // Indicates whether we are using front camera
    private boolean mMirror;
    private boolean mFirstTimeInitialized;
    private boolean mIsImageCaptureIntent;

    private int mCameraState = PREVIEW_STOPPED;
    private boolean mSnapshotOnIdle = false;

    private ContentResolver mContentResolver;

    private LocationManager mLocationManager;

    private final PostViewPictureCallback mPostViewPictureCallback =
            new PostViewPictureCallback();
    private final RawPictureCallback mRawPictureCallback =
            new RawPictureCallback();
    
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    private long mShutterCallbackTime;
    private long mPostViewPictureCallbackTime;
    private long mRawPictureCallbackTime;
    private long mJpegPictureCallbackTime;
    private byte[] mJpegImageData;

    // These latency time are for the CameraLatency test.
    public long mAutoFocusTime;
    public long mShutterLag;
    public long mShutterToPictureDisplayedTime;
    public long mPictureDisplayedToJpegCallbackTime;
    public long mJpegCallbackFinishTime;
    public long mCaptureStartTime;
    
    private String mSceneMode;

    private final Handler mHandler = new MainHandler();

    private PreferenceGroup mPreferenceGroup;

    private boolean mQuickCapture;

    // True if all the parameters needed to start preview is ready.
    private boolean mCameraPreviewParamsReady = false;

    private MediaSaveService.OnMediaSavedListener mOnMediaSavedListener =
            new MediaSaveService.OnMediaSavedListener() {
                @Override
                public void onMediaSaved(Uri uri) {
                    if (uri != null) {
                        mActivity.notifyNewMedia(uri);
                    }
                }
            };

    /**
     * This Handler is used to post message back onto the main thread of the
     * application
     */
    private class MainHandler extends Handler {
        public MainHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SETUP_PREVIEW: {
                    setupPreview();
                    break;
                }

                case CLEAR_SCREEN_DELAY: {
                    mActivity.getWindow().clearFlags(
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                }

                case FIRST_TIME_INIT: {
                    initializeFirstTime();
                    break;
                }

                case SET_CAMERA_PARAMETERS_WHEN_IDLE: {
                    setCameraParametersWhenIdle(0);
                    break;
                }

                case SWITCH_CAMERA: {
                    switchCamera();
                    break;
                }

                case SWITCH_CAMERA_START_ANIMATION: {
                    // TODO: Need to revisit
                    // ((CameraScreenNail) mActivity.mCameraScreenNail).animateSwitchCamera();
                    break;
                }

                case CAMERA_OPEN_DONE: {
                    onCameraOpened();
                    break;
                }

                case OPEN_CAMERA_FAIL: {
                    mOpenCameraFail = true;
                    CameraUtil.showErrorAndFinish(mActivity,
                            R.string.cannot_connect_camera);
                    break;
                }

                case CAMERA_DISABLED: {
                    mCameraDisabled = true;
                    CameraUtil.showErrorAndFinish(mActivity,
                            R.string.camera_disabled);
                    break;
                }

            }
        }
    }


    @Override
    public void init(CameraActivity activity, View parent, boolean showInstructions) {
        mActivity = activity;
        mUI = new PhotoUI(activity, this, parent, showInstructions);
        mShouldShowInstructions = showInstructions;
        mPreferences = new ComboPreferences(mActivity);
        CameraSettings.upgradeGlobalPreferences(mPreferences.getGlobal());
        mCameraId = getPreferredCameraId(mPreferences);

        mContentResolver = mActivity.getContentResolver();

        // Surface texture is from camera screen nail and startPreview needs it.
        // This must be done before startPreview.
        mIsImageCaptureIntent = isImageCaptureIntent();

        mPreferences.setLocalId(mActivity, mCameraId);
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
        // we need to reset exposure for the preview
        resetExposureCompensation();

        initializeControlByIntent();
        mQuickCapture = mActivity.getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        mLocationManager = new LocationManager(mActivity, null);
    }

    private void initializeControlByIntent() {
        mUI.initializeControlByIntent();
        if (mIsImageCaptureIntent) {
            setupCaptureParams();
        }
    }

    private void onPreviewStarted() {
        setCameraState(IDLE);
        locationFirstRun();
        if(mShouldShowInstructions) {
            mUI.fadeInstructionText(1000, 2500);
            mShouldShowInstructions = false;
        }
    }

    // Prompt the user to pick to record location for the very first run of
    // camera only
    private void locationFirstRun() {
    	Log.d(TAG, "This is first Camera Run");
        if (RecordLocationPreference.isSet(mPreferences)) {
            return;
        }
        this.enableRecordingLocation(true);
    }

    @Override
    public void enableRecordingLocation(boolean enable) {
        setLocationPreference(enable ? RecordLocationPreference.VALUE_ON
                : RecordLocationPreference.VALUE_OFF);
    }

    @Override
    public void onPreviewUIReady() {
        if(mActivity.isPreviewTimedOut()){
            onResumeBeforeSuper();
            onResumeAfterSuper();
        }
        startPreview();
    }

    @Override
    public void onPreviewUIDestroyed() {
        onPauseBeforeSuper();
        onPauseAfterSuper(); // this method will call stopPreview()
    }

    private void setLocationPreference(String value) {
        mPreferences.edit()
                .putString(CameraSettings.KEY_RECORD_LOCATION, value)
                .apply();
        // TODO: Fix this to use the actual onSharedPreferencesChanged listener
        // instead of invoking manually
        onSharedPreferenceChanged();
    }

    private void onCameraOpened() {
        // These depend on camera parameters.
        openCameraCommon();
    }

    private void switchCamera() {
        if (mPaused) return;

        Log.v(TAG, "Start to switch camera. id=" + mPendingSwitchCameraId);
        mCameraId = mPendingSwitchCameraId;
        mPendingSwitchCameraId = -1;
        setCameraId(mCameraId);

        // from onPause
        closeCamera();
        mUI.collapseCameraControls();

        // Restart the camera and initialize the UI. From onCreate.
        mPreferences.setLocalId(mActivity, mCameraId);
        CameraSettings.upgradeLocalPreferences(mPreferences.getLocal());
        mCameraDevice = CameraUtil.openCamera(
                mActivity, mCameraId, mHandler,
                mActivity.getCameraOpenErrorCallback());

        if (mCameraDevice == null) {
            Log.e(TAG, "Failed to open camera:" + mCameraId + ", aborting.");
            return;
        }
        mParameters = mCameraDevice.getParameters();
        initializeCapabilities();
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mCameraId];
        mMirror = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
        setupPreview();

        // reset zoom value index
        openCameraCommon();

        // Start switch camera animation. Post a message because
        // onFrameAvailable from the old camera may already exist.
        mHandler.sendEmptyMessage(SWITCH_CAMERA_START_ANIMATION);
    }

    protected void setCameraId(int cameraId) {
        ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_CAMERA_ID);
        pref.setValue("" + cameraId);
    }

    // either open a new camera or switch cameras
    private void openCameraCommon() {
        loadCameraPreferences();
    }

    private void resetExposureCompensation() {
        String value = mPreferences.getString(CameraSettings.KEY_EXPOSURE,
                CameraSettings.EXPOSURE_DEFAULT_VALUE);
        if (!CameraSettings.EXPOSURE_DEFAULT_VALUE.equals(value)) {
            Editor editor = mPreferences.edit();
            editor.putString(CameraSettings.KEY_EXPOSURE, "0");
            editor.apply();
        }
    }

    private void keepMediaProviderInstance() {
        // We want to keep a reference to MediaProvider in camera's lifecycle.
        // TODO: Utilize mMediaProviderClient instance to replace
        // ContentResolver calls.
        if (mMediaProviderClient == null) {
            mMediaProviderClient = mContentResolver
                    .acquireContentProviderClient(MediaStore.AUTHORITY);
        }
    }

    // Snapshots can only be taken after this is called. It should be called
    // once only. We could have done these things in onCreate() but we want to
    // make preview screen appear as soon as possible.
    private void initializeFirstTime() {
        if (mFirstTimeInitialized || mPaused) {
            return;
        }

        // Initialize location service.
        boolean recordLocation = RecordLocationPreference.get(
                mPreferences, mContentResolver);
        mLocationManager.recordLocation(recordLocation);

        keepMediaProviderInstance();

        MediaSaveService s = mActivity.getMediaSaveService();
        // We set the listener only when both service and shutterbutton
        // are initialized.
        if (s != null) {
            s.setListener(this);
        }

        mNamedImages = new NamedImages();

        mFirstTimeInitialized = true;
        addIdleHandler();

        mActivity.updateStorageSpaceAndHint();
    }

    // If the activity is paused and resumed, this method will be called in
    // onResume.
    private void initializeSecondTime() {
        // Start location update if needed.
        boolean recordLocation = RecordLocationPreference.get(
                mPreferences, mContentResolver);
        mLocationManager.recordLocation(recordLocation);
        MediaSaveService s = mActivity.getMediaSaveService();
        if (s != null) {
            s.setListener(this);
        }
        mNamedImages = new NamedImages();
        mUI.initializeSecondTime(mParameters);
        keepMediaProviderInstance();
    }

    private void addIdleHandler() {
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                StorageUtil.ensureOSXCompatible();
                return false;
            }
        });
    }

    private final class ShutterCallback
            implements CameraShutterCallback {

        private boolean mNeedsAnimation;

        public ShutterCallback(boolean needsAnimation) {
            mNeedsAnimation = needsAnimation;
        }

        @Override
        public void onShutter(CameraProxy camera) {
            mShutterCallbackTime = System.currentTimeMillis();
            mShutterLag = mShutterCallbackTime - mCaptureStartTime;
            Log.v(TAG, "mShutterLag = " + mShutterLag + "ms");
            if (mNeedsAnimation) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        animateAfterShutter();
                    }
                });
            }
        }
    }

    private final class PostViewPictureCallback
            implements CameraPictureCallback {
        @Override
        public void onPictureTaken(byte [] data, CameraProxy camera) {
            mPostViewPictureCallbackTime = System.currentTimeMillis();
            Log.v(TAG, "mShutterToPostViewCallbackTime = "
                    + (mPostViewPictureCallbackTime - mShutterCallbackTime)
                    + "ms");
        }
    }

    private final class RawPictureCallback
            implements CameraPictureCallback {
        @Override
        public void onPictureTaken(byte [] rawData, CameraProxy camera) {
            mRawPictureCallbackTime = System.currentTimeMillis();
            Log.v(TAG, "mShutterToRawCallbackTime = "
                    + (mRawPictureCallbackTime - mShutterCallbackTime) + "ms");
        }
    }

    private final class JpegPictureCallback
            implements CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            mLocation = loc;
        }

        @Override
        public void onPictureTaken(final byte [] jpegData, CameraProxy camera) {
        	Log.d(TAG, "onPictureTaken() called!");
        	// Enable Photo Taking after picture is taken. For avoiding race condition.
        	// This will be compatible with later version ( API 17 ) which supports
        	// HDR feature, which is multi-second operations.
        	mCanTakePhoto=true;
        	
            if (mPaused) {
                return;
            }
            if (mIsImageCaptureIntent) {
                stopPreview();
            }
            if (mSceneMode == CameraUtil.SCENE_MODE_HDR) {
                mUI.setSwipingEnabled(true);
            }

            mJpegPictureCallbackTime = System.currentTimeMillis();
            // If postview callback has arrived, the captured image is displayed
            // in postview callback. If not, the captured image is displayed in
            // raw picture callback.
            if (mPostViewPictureCallbackTime != 0) {
                mShutterToPictureDisplayedTime =
                        mPostViewPictureCallbackTime - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime =
                        mJpegPictureCallbackTime - mPostViewPictureCallbackTime;
            } else {
                mShutterToPictureDisplayedTime =
                        mRawPictureCallbackTime - mShutterCallbackTime;
                mPictureDisplayedToJpegCallbackTime =
                        mJpegPictureCallbackTime - mRawPictureCallbackTime;
            }
            Log.v(TAG, "mPictureDisplayedToJpegCallbackTime = "
                    + mPictureDisplayedToJpegCallbackTime + "ms");

            if (!mIsImageCaptureIntent) {
                setupPreview();
            }

            ExifInterface exif = Exif.getExif(jpegData);
            int orientation = 0; // make it always 0 : Portrait
//            int orientation = Exif.getOrientation(exif);

            if (!mIsImageCaptureIntent) {
                // Calculate the width and the height of the jpeg.
                Size s = mParameters.getPictureSize();
                int width, height;
                width = s.width;
                height = s.height;
                
                NamedEntity name = mNamedImages.getNextNameEntity();
                String title = (name == null) ? null : name.title;
                long date = (name == null) ? -1 : name.date;

                // Handle debug mode outputs
                if (mDebugUri != null) {
                    // If using a debug uri, save jpeg there.
                    saveToDebugUri(jpegData);

                    // Adjust the title of the debug image shown in mediastore.
                    if (title != null) {
                        title = DEBUG_IMAGE_PREFIX + title;
                    }
                }

                if (title == null) {
                    Log.e(TAG, "Unbalanced name/data pair");
                } else {
                    if (date == -1) date = mCaptureStartTime;
                    mActivity.getMediaSaveService().addImage(
                            jpegData, title, date, mLocation, width, height,
                            orientation, exif, mOnMediaSavedListener, mContentResolver);
                }
                // Animate capture with real jpeg data instead of a preview frame.
                mUI.animateCapture(jpegData, orientation, mMirror);
            } else {
                mJpegImageData = jpegData;
                if (mQuickCapture) {
                    onCaptureDone();
                }
            }

            // Check this in advance of each shot so we don't add to shutter
            // latency. It's true that someone else could write to the SD card in
            // the mean time and fill it, but that could have happened between the
            // shutter press and saving the JPEG too.
            mActivity.updateStorageSpaceAndHint();

            long now = System.currentTimeMillis();
            mJpegCallbackFinishTime = now - mJpegPictureCallbackTime;
            Log.v(TAG, "mJpegCallbackFinishTime = "
                    + mJpegCallbackFinishTime + "ms");
            mJpegPictureCallbackTime = 0;
        }
    }

    /**
     * This class is just a thread-safe queue for name,date holder objects.
     */
    public static class NamedImages {
        private Vector<NamedEntity> mQueue;

        public NamedImages() {
            mQueue = new Vector<NamedEntity>();
        }

        public void nameNewImage(long date) {
            NamedEntity r = new NamedEntity();
            r.title = CameraUtil.createJpegName(date);
            r.date = date;
            mQueue.add(r);
        }

        public NamedEntity getNextNameEntity() {
            synchronized(mQueue) {
                if (!mQueue.isEmpty()) {
                    return mQueue.remove(0);
                }
            }
            return null;
        }

        public static class NamedEntity {
            public String title;
            public long date;
        }
    }

    private void setCameraState(int state) {
        mCameraState = state;
        switch (state) {
            case PhotoController.PREVIEW_STOPPED:
            case PhotoController.SNAPSHOT_IN_PROGRESS:
            case PhotoController.SWITCHING_CAMERA:
                //mUI.enableGestures(false); TODO replace with control enable/disable for stability
                break;
            case PhotoController.IDLE:
                //mUI.enableGestures(true); TODO replace with control enable/disable for stability
                break;
        }
    }

    private void animateAfterShutter() {
        // Only animate when in full screen capture mode
        // i.e. If monkey/a user swipes to the gallery during picture taking,
        // don't show animation
        if (!mIsImageCaptureIntent) {
            mUI.animateFlash();
        }
    }

    public boolean capture() {
        // If we are already in the middle of taking a snapshot or the image save request
        // is full then ignore.
        if (mCameraDevice == null || mCameraState == SNAPSHOT_IN_PROGRESS
                || mCameraState == SWITCHING_CAMERA
                || mActivity.getMediaSaveService() == null
                || mActivity.getMediaSaveService().isQueueFull()) {
            return false;
        }
        mCaptureStartTime = System.currentTimeMillis();
        
        mPostViewPictureCallbackTime = 0;
        mJpegImageData = null;

        final boolean animateBefore = (mSceneMode == CameraUtil.SCENE_MODE_HDR);

        if (animateBefore) {
            animateAfterShutter();
        }

        // Set rotation and gps data.
        mParameters.setRotation(0);
        Location loc = mLocationManager.getCurrentLocation();
        CameraUtil.setGpsParameters(mParameters, loc);
        mCameraDevice.setParameters(mParameters);

        // We don't want user to press the button again while taking a
        // multi-second operations
        mCanTakePhoto = false;
        
        mCameraDevice.takePicture(mHandler,
                new ShutterCallback(!animateBefore),
                mRawPictureCallback, mPostViewPictureCallback,
                new JpegPictureCallback(loc));

        mNamedImages.nameNewImage(mCaptureStartTime);

        setCameraState(SNAPSHOT_IN_PROGRESS);
        UsageStatistics.onEvent(UsageStatistics.COMPONENT_CAMERA,
                UsageStatistics.ACTION_CAPTURE_DONE, "Photo", 0,
                UsageStatistics.hashFileName(mNamedImages.mQueue.lastElement().title + ".jpg"));
        return true;
    }

    private int getPreferredCameraId(ComboPreferences preferences) {
        int intentCameraId = CameraUtil.getCameraFacingIntentExtras(mActivity);
        if (intentCameraId != -1) {
            // Testing purpose. Launch a specific camera through the intent
            // extras.
            return intentCameraId;
        } else {
            return CameraSettings.readPreferredCameraId(preferences);
        }
    }

    private void loadCameraPreferences() {
        CameraSettings settings = new CameraSettings(mActivity, mInitialParams,
                mCameraId, CameraHolder.instance().getCameraInfo());
        mPreferenceGroup = settings.getPreferenceGroup(R.xml.camera_preferences);
    }

    @Override
    public void onStop() {
        if (mMediaProviderClient != null) {
            mMediaProviderClient.release();
            mMediaProviderClient = null;
        }
    }

    @Override
    public void onCaptureCancelled() {
        mActivity.setResultEx(Activity.RESULT_CANCELED, new Intent());
        mActivity.finish();
    }

    @Override
    public void onCaptureRetake() {
        if (mPaused)
            return;
        setupPreview();
    }

    @Override
    public void onCaptureDone() {
        if (mPaused) {
            return;
        }

        byte[] data = mJpegImageData;
        Log.d(TAG, mJpegImageData == null ? "data is null" : "data is not null" );

        if (mCropValue == null) {
            // First handle the no crop case -- just return the value.  If the
            // caller specifies a "save uri" then write the data to its
            // stream. Otherwise, pass back a scaled down version of the bitmap
            // directly in the extras.
            if (mSaveUri != null) {
                OutputStream outputStream = null;
                try {
                    outputStream = mContentResolver.openOutputStream(mSaveUri);
                    outputStream.write(data);
                    outputStream.close();

                    mActivity.setResultEx(Activity.RESULT_OK);
                    mActivity.finish();
                } catch (IOException ex) {
                    // ignore exception
                } finally {
                    CameraUtil.closeSilently(outputStream);
                }
            } else {
                Bitmap bitmap = CameraUtil.makeBitmap(data, 50 * 1024);
                mActivity.setResultEx(Activity.RESULT_OK,
                        new Intent("inline-data").putExtra("data", bitmap));
                mActivity.finish();
            }
        } else {
            // Save the image to a temp file and invoke the cropper
            Uri tempUri = null;
            FileOutputStream tempStream = null;
            try {
                File path = mActivity.getFileStreamPath(sTempCropFilename);
                path.delete();
                tempStream = mActivity.openFileOutput(sTempCropFilename, 0);
                tempStream.write(data);
                tempStream.close();
                tempUri = Uri.fromFile(path);
            } catch (FileNotFoundException ex) {
                mActivity.setResultEx(Activity.RESULT_CANCELED);
                mActivity.finish();
                return;
            } catch (IOException ex) {
                mActivity.setResultEx(Activity.RESULT_CANCELED);
                mActivity.finish();
                return;
            } finally {
                CameraUtil.closeSilently(tempStream);
            }

            Bundle newExtras = new Bundle();
            if (mCropValue.equals("circle")) {
                newExtras.putString("circleCrop", "true");
            }
            if (mSaveUri != null) {
                newExtras.putParcelable(MediaStore.EXTRA_OUTPUT, mSaveUri);
            } else {
                newExtras.putBoolean(CameraUtil.KEY_RETURN_DATA, true);
            }

            // TODO: Share this constant.
            final String CROP_ACTION = "com.android.camera.action.CROP";
            Intent cropIntent = new Intent(CROP_ACTION);

            cropIntent.setData(tempUri);
            cropIntent.putExtras(newExtras);

            mActivity.startActivityForResult(cropIntent, REQUEST_CROP);
        }
    }

    public void takePhoto() {
    	
    	if (!mCanTakePhoto)
    		return;
    	
        if (mPaused || mUI.collapseCameraControls()
                || (mCameraState == SWITCHING_CAMERA)
                || (mCameraState == PREVIEW_STOPPED)) return;

        // Do not take the picture if there is not enough storage.
        if (mActivity.getStorageSpaceBytes() <= StorageUtil.LOW_STORAGE_THRESHOLD_BYTES) {
            Log.i(TAG, "Not enough space or storage not ready. remaining="
                    + mActivity.getStorageSpaceBytes());
            return;
        }
        Log.v(TAG, "onShutterButtonClick: mCameraState=" + mCameraState);

        if (mSceneMode == CameraUtil.SCENE_MODE_HDR) {
            mUI.setSwipingEnabled(false);
        }
        // If the user wants to do a snapshot while the previous one is still
        // in progress, remember the fact and do it after we finish the previous
        // one and re-start the preview. Snapshot in progress also includes the
        // state that autofocus is focusing and a picture will be taken when
        // focus callback arrives.
        if (mCameraState == SNAPSHOT_IN_PROGRESS && !mIsImageCaptureIntent) {
            mSnapshotOnIdle = true;
            return;
        }

        String timer = mPreferences.getString(
                CameraSettings.KEY_TIMER,
                mActivity.getString(R.string.pref_camera_timer_default));
        boolean playSound = mPreferences.getString(CameraSettings.KEY_TIMER_SOUND_EFFECTS,
                mActivity.getString(R.string.pref_camera_timer_sound_default))
                .equals(mActivity.getString(R.string.setting_on_value));

        int seconds = Integer.parseInt(timer);
        // When shutter button is pressed, check whether the previous countdown is
        // finished. If not, cancel the previous countdown and start a new one.
        if (mUI.isCountingDown()) {
            mUI.cancelCountDown();
        }
        if (seconds > 0) {
            mUI.startCountDown(seconds, playSound);
        } else {
            mSnapshotOnIdle = false;
            capture(); // Capture the photo TODO is this right to be here?
        }
    }

    @Override
    public void installIntentFilter() {
        // Do nothing.
    }

    @Override
    public boolean updateStorageHintOnResume() {
        return mFirstTimeInitialized;
    }

    @Override
    public void onResumeBeforeSuper() {
        mPaused = false;
    }
    
//    protected class CameraStartThread extends Thread {
//        @Override
//        public void run() {
//            startPreview();
//        }
//    }

    private boolean prepareCamera() {
        // We need to check whether the activity is paused before long
        // operations to ensure that onPause() can be done ASAP.
        Log.v(TAG, "Open camera device.");
        mCameraDevice = CameraUtil.openCamera(
                mActivity, mCameraId, mHandler,
                mActivity.getCameraOpenErrorCallback());
        if (mCameraDevice == null) {
            Log.e(TAG, "Failed to open camera:" + mCameraId);
            return false;
        }
        mParameters = mCameraDevice.getParameters();

        initializeCapabilities();

        setCameraParameters(UPDATE_PARAM_INITIALIZE | UPDATE_PARAM_ZOOM);
        mHandler.sendEmptyMessage(CAMERA_OPEN_DONE);
        mCameraPreviewParamsReady = true;
        return true;
    }

    @Override
    public void onResumeAfterSuper() {
        // Add delay on resume from lock screen only, in order to to speed up
        // the onResume --> onPause --> onResume cycle from lock screen.
        // Don't do always because letting go of thread can cause delay.
        String action = mActivity.getIntent().getAction();
        if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(action)
                || MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action)) {
            Log.v(TAG, "On resume, from lock screen.");
            // Note: onPauseAfterSuper() will delete this runnable, so we will
            // at most have 1 copy queued up.
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    onResumeTasks();
                }
            }, ON_RESUME_TASKS_DELAY_MSEC);
        } else {
            Log.v(TAG, "On resume.");
            onResumeTasks();
        }
    }

    private void onResumeTasks() {
        Log.v(TAG, "Executing onResumeTasks.");
        if (mOpenCameraFail || mCameraDisabled) return;

        mJpegPictureCallbackTime = 0;
        resetExposureCompensation();
        if (!prepareCamera()) {
            // Camera failure.
            return;
        }

        // If first time initialization is not finished, put it in the
        // message queue.
        if (!mFirstTimeInitialized) {
            mHandler.sendEmptyMessage(FIRST_TIME_INIT);
        } else {
            initializeSecondTime();
        }
        mUI.initDisplayChangeListener();
        keepScreenOnAwhile();

        mUI.onResume();

        UsageStatistics.onContentViewChanged(
                UsageStatistics.COMPONENT_CAMERA, "PhotoModule");

    }

    @Override
    public void onPauseBeforeSuper() {
        mPaused = true;
    }

    @Override
    public void onPauseAfterSuper() {
        Log.v(TAG, "On pause.");

        // Reset the focus first. Camera CTS does not guarantee that
        // cancelAutoFocus is allowed after preview stops.
        if (mCameraDevice != null && mCameraState != PREVIEW_STOPPED) {
            mCameraDevice.cancelAutoFocus();
        }
        // If the camera has not been opened asynchronously yet,
        // and startPreview hasn't been called, then this is a no-op.
        // (e.g. onResume -> onPause -> onResume).
        stopPreview();

        mNamedImages = null;

        if (mLocationManager != null) mLocationManager.recordLocation(false);

        // If we are in an image capture intent and has taken
        // a picture, we just clear it in onPause.
        mJpegImageData = null;

        // Remove the messages and runnables in the queue.
        mHandler.removeCallbacksAndMessages(null);

        closeCamera();

        resetScreenOn();
        mUI.onPause();

        mPendingSwitchCameraId = -1;
        MediaSaveService s = mActivity.getMediaSaveService();
        if (s != null) {
            s.setListener(null);
        }
        mUI.removeDisplayChangeListener();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v(TAG, "onConfigurationChanged");
    }

    @Override
    public void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CROP: {
                Intent intent = new Intent();
                if (data != null) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        intent.putExtras(extras);
                    }
                }
                mActivity.setResultEx(resultCode, intent);
                mActivity.finish();

                File path = mActivity.getFileStreamPath(sTempCropFilename);
                path.delete();

                break;
            }
        }
    }

    private boolean canTakePicture() {
        return isCameraIdle() && (mActivity.getStorageSpaceBytes() > StorageUtil.LOW_STORAGE_THRESHOLD_BYTES);
    }

    @Override
    public boolean onBackPressed() {
        return mUI.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (mFirstTimeInitialized && event.getRepeatCount() == 0) {
                    togglePreviewTimeoutScreen(false);
                	takePhoto();
                }
                return true;
        }
        return false;
    }

    private void closeCamera() {
        Log.v(TAG, "Close camera device.");
        if (mCameraDevice != null) {
            mCameraDevice.setZoomChangeListener(null);
            mCameraDevice.setErrorCallback(null);

//            if (!CameraActivity.isFirstStartAfterScreenOn()) {
                // Blocks until camera is actually released.
                CameraHolder.instance().strongRelease();
//            } else {
//                CameraHolder.instance().release();
//            }

            mCameraDevice = null;
            setCameraState(PREVIEW_STOPPED);

        }
    }

    /** Only called by UI thread. */
    private void setupPreview() {
        startPreview();
    }

    /** This can run on a background thread, post any view updates to MainHandler. */
    private void startPreview() {
        if (mPaused || mCameraDevice == null) {
            return;
        }

        // Use SurfaceView to render camera preview for extra power savings
        SurfaceHolder sh = mUI.getSurfaceHolder();
        if (sh == null) {
            Log.w(TAG, "startPreview: surfaceView is not ready.");
            return;
        }

        if (!mCameraPreviewParamsReady) {
            Log.w(TAG, "startPreview: parameters for preview is not ready.");
            return;
        }
        mCameraDevice.setErrorCallback(mErrorCallback);
        // ICS camera frameworks has a bug. Face detection state is not cleared 1589
        // after taking a picture. Stop the preview to work around it. The bug
        // was fixed in JB.
        if (mCameraState != PREVIEW_STOPPED) {
            stopPreview();
        }

        setCameraParameters(UPDATE_PARAM_PREFERENCE);
        // Let UI set its expected aspect ratio
        mCameraDevice.setPreviewDisplay(sh);

        Log.v(TAG, "startPreview");
        mCameraDevice.startPreview();
        onPreviewStarted();

        if (mSnapshotOnIdle) {
            mHandler.post(mDoSnapRunnable);
        }
    }

    @Override
    public void stopPreview() {
        if (mCameraDevice != null && mCameraState != PREVIEW_STOPPED) {
            Log.v(TAG, "stopPreview");
            mCameraDevice.stopPreview();
        }
        setCameraState(PREVIEW_STOPPED);
    }

    private void updateCameraParametersInitialize() {
        mParameters.set(CameraUtil.RECORDING_HINT, CameraUtil.FALSE);

        //We know our camera and display chain, so choose the best fit here
        mParameters.setPreviewFpsRange(mFrames_Thousands,mFrames_Thousands);
        //TODO: Use 640X480@10fps after we understand how to use GPU processing, check:SurfaceTexture.updateTexImage
        //TODO: Or 320X240@20fps, 10fps is too slow, but we need to check its performance and power consumption.
        mPreviewWidth = 1280;
        mPreviewHeight = 720;
        mParameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
        
        mParameters.setPictureSize(1280,960);
        // Disable video stabilization. Convenience methods not available in API
        // level <= 14
        String vstabSupported = mParameters.get("video-stabilization-supported");
        if ("true".equals(vstabSupported)) {
            mParameters.set("video-stabilization", "false");
        }
    }

    private void updateCameraParametersZoom() {
        // Set zoom.
        if (mParameters.isZoomSupported()) {
        }
    }
    
    private void setAutoExposureLockIfSupported() {
        if (mAeLockSupported) {
            mParameters.setAutoExposureLock(false);
        }
    }

    private boolean updateCameraParametersPreference() {
    	setAutoExposureLockIfSupported();
        /*
        // Set picture size.
        String pictureSize = mPreferences.getString(
                CameraSettings.KEY_PICTURE_SIZE, null);
        if (pictureSize == null) {
            CameraSettings.initialCameraPictureSize(mActivity, mParameters);
        } else {
            List<Size> supported = mParameters.getSupportedPictureSizes();
            CameraSettings.setCameraPictureSize(
                    pictureSize, supported, mParameters);
        }
        
        // Set a preview size that is closest to the viewfinder height and has
        // the right aspect ratio.
        Log.d(TAG, "Setting Photo preview size... ");
        Size optimalSize = CameraUtil.getOptimalPreviewSize(mActivity, sizes,
        		(double)mPreviewWidth / (double)mPreviewHeight);
        Log.e(TAG,"optimal Width: " + optimalSize.width + " ||| optimal Height: " + optimalSize.height);
        Size original = mParameters.getPreviewSize();
        if(optimalSize.width != 0 && optimalSize.height != 0) {
            mUI.updatePreviewAspectRatio((float) optimalSize.width
                    / (float) optimalSize.height);
            //mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
        }

        if (!original.equals(optimalSize)) {
        	mParameters.setPreviewSize(optimalSize.width, optimalSize.height);

            // Zoom related settings will be changed for different preview
            // sizes, so set and read the parameters to get latest values
            if (mHandler.getLooper() == Looper.myLooper()) {
            	Log.d(TAG, "Handler looper == MYLOOPER");
                // On UI thread only, not when camera starts up
                setupPreview();
            } else {
                mCameraDevice.setParameters(mParameters);
            }
            mParameters = mCameraDevice.getParameters();
        }
        Log.v(TAG, "Preview size is " + optimalSize.width + "x" + optimalSize.height);
        */

        mUI.updatePreviewAspectRatio((float) mPreviewWidth
                    / (float) mPreviewHeight);

        if(DEBUG) {
            List<Size> sizes = mParameters.getSupportedPreviewSizes();
            Log.v(TAG, "Supported Photo preview size");
            for(int i=0; i< sizes.size(); i++)
                Log.v(TAG, " <" + sizes.get(i).width + 
                        " : " + sizes.get(i).height + ">");
            Size cam_size = mParameters.getPreviewSize();
            Log.v(TAG, "change size:" + cam_size.width + "," + cam_size.height);

            List<int[]> fpslist = mParameters.getSupportedPreviewFpsRange();
            Log.v(TAG, "Preview supported FPS range ");
            for(int i=0; i< fpslist.size(); i++)
                Log.v(TAG, " <" + fpslist.get(i)[Parameters.PREVIEW_FPS_MIN_INDEX] + 
                            " : " + fpslist.get(i)[Parameters.PREVIEW_FPS_MAX_INDEX] + ">");
            int[] fpsrange= new int[2];
                    mParameters.getPreviewFpsRange(fpsrange);
            Log.v(TAG, "Current FPS range: <" + fpsrange[Parameters.PREVIEW_FPS_MIN_INDEX] + ":" + fpsrange[Parameters.PREVIEW_FPS_MAX_INDEX] + ">");
        }

        // Since changing scene mode may change supported values, set scene mode
        // first. HDR is a scene mode. To promote it in UI, it is stored in a
        // separate preference.
        String onValue = mActivity.getString(R.string.setting_on_value);
        String hdr = mPreferences.getString(CameraSettings.KEY_CAMERA_HDR,
                mActivity.getString(R.string.pref_camera_hdr_default));
        String hdrPlus = mPreferences.getString(CameraSettings.KEY_CAMERA_HDR_PLUS,
                mActivity.getString(R.string.pref_camera_hdr_plus_default));
        boolean hdrOn = onValue.equals(hdr);
        boolean hdrPlusOn = onValue.equals(hdrPlus);

        boolean doGcamModeSwitch = false;
        if (hdrPlusOn) {
            // Kick off mode switch to gcam.
            doGcamModeSwitch = true;
        } else {
            if (hdrOn) {
                mSceneMode = CameraUtil.SCENE_MODE_HDR;
            } else {
                mSceneMode = mPreferences.getString(
                        CameraSettings.KEY_SCENE_MODE,
                        mActivity.getString(R.string.pref_camera_scenemode_default));
            }
        }
        if (CameraUtil.isSupported(mSceneMode, mParameters.getSupportedSceneModes())) {
            if (!mParameters.getSceneMode().equals(mSceneMode)) {
                mParameters.setSceneMode(mSceneMode);

                // Setting scene mode will change the settings of flash mode,
                // white balance, and focus mode. Here we read back the
                // parameters, so we can know those settings.
                mCameraDevice.setParameters(mParameters);
                mParameters = mCameraDevice.getParameters();
            }
        } else {
            mSceneMode = mParameters.getSceneMode();
            if (mSceneMode == null) {
                mSceneMode = Parameters.SCENE_MODE_AUTO;
            }
        }

        // Set JPEG quality.
        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(mCameraId,
                CameraProfile.QUALITY_HIGH);
        mParameters.setJpegQuality(jpegQuality);

        // For the following settings, we need to check if the settings are
        // still supported by latest driver, if not, ignore the settings.

        // Set exposure compensation
        int value = CameraSettings.readExposure(mPreferences);
        int max = mParameters.getMaxExposureCompensation();
        int min = mParameters.getMinExposureCompensation();
        if (value >= min && value <= max) {
            mParameters.setExposureCompensation(value);
        } else {
            Log.w(TAG, "invalid exposure range: " + value);
        }
        
//        mExposureComp = value;

        if (Parameters.SCENE_MODE_AUTO.equals(mSceneMode)) {
            // Set flash mode.
            String flashMode = mPreferences.getString(
                    CameraSettings.KEY_FLASH_MODE,
                    mActivity.getString(R.string.pref_camera_flashmode_default));
            List<String> supportedFlash = mParameters.getSupportedFlashModes();
            if (CameraUtil.isSupported(flashMode, supportedFlash)) {
                mParameters.setFlashMode(flashMode);
            } else {
                flashMode = mParameters.getFlashMode();
                if (flashMode == null) {
                    flashMode = mActivity.getString(
                            R.string.pref_camera_flashmode_no_flash);
                }
            }

            // Set white balance parameter.
            String whiteBalance = mPreferences.getString(
                    CameraSettings.KEY_WHITE_BALANCE,
                    mActivity.getString(R.string.pref_camera_whitebalance_default));
            if (CameraUtil.isSupported(whiteBalance,
                    mParameters.getSupportedWhiteBalance())) {
                mParameters.setWhiteBalance(whiteBalance);
            } else {
                whiteBalance = mParameters.getWhiteBalance();
                if (whiteBalance == null) {
                    whiteBalance = Parameters.WHITE_BALANCE_AUTO;
                }
            }

        }

        if (mContinuousFocusSupported && ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK) {
            updateAutoFocusMoveCallback();
        }

        return doGcamModeSwitch;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void updateAutoFocusMoveCallback() {
    	mCameraDevice.setAutoFocusMoveCallback(null, null);
    }

    // We separate the parameters into several subsets, so we can update only
    // the subsets actually need updating. The PREFERENCE set needs extra
    // locking because the preference can be changed from GLThread as well.
    private void setCameraParameters(int updateSet) {
        boolean doModeSwitch = false;

        if ((updateSet & UPDATE_PARAM_INITIALIZE) != 0) {
            updateCameraParametersInitialize();
        }

        if ((updateSet & UPDATE_PARAM_ZOOM) != 0) {
            updateCameraParametersZoom();
        }

        if ((updateSet & UPDATE_PARAM_PREFERENCE) != 0) {
            doModeSwitch = updateCameraParametersPreference();
        }

        mCameraDevice.setParameters(mParameters);

        // Switch to gcam module if HDR+ was selected
        if (doModeSwitch && !mIsImageCaptureIntent) {
            mHandler.sendEmptyMessage(SWITCH_TO_GCAM_MODULE);
        }
    }

    // If the Camera is idle, update the parameters immediately, otherwise
    // accumulate them in mUpdateSet and update later.
    private void setCameraParametersWhenIdle(int additionalUpdateSet) {
        mUpdateSet |= additionalUpdateSet;
        if (mCameraDevice == null) {
            // We will update all the parameters when we open the device, so
            // we don't need to do anything now.
            mUpdateSet = 0;
            return;
        } else if (isCameraIdle()) {
            setCameraParameters(mUpdateSet);
            mUpdateSet = 0;
        } else {
            if (!mHandler.hasMessages(SET_CAMERA_PARAMETERS_WHEN_IDLE)) {
                mHandler.sendEmptyMessageDelayed(
                        SET_CAMERA_PARAMETERS_WHEN_IDLE, 1000);
            }
        }
    }

    @Override
    public boolean isCameraIdle() {
        return (mCameraState == IDLE) ||
                (mCameraState == PREVIEW_STOPPED)
                        && (mCameraState != SWITCHING_CAMERA);
    }

    @Override
    public boolean isImageCaptureIntent() {
        String action = mActivity.getIntent().getAction();
        return MediaStore.ACTION_IMAGE_CAPTURE.equals(action);
    }

    private void setupCaptureParams() {
        Bundle myExtras = mActivity.getIntent().getExtras();
        if (myExtras != null) {
            mSaveUri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
            mCropValue = myExtras.getString("crop");
        }
    }

    private void onSharedPreferenceChanged() {
        // ignore the events after "onPause()"
        if (mPaused) return;

        boolean recordLocation = RecordLocationPreference.get(
                mPreferences, mContentResolver);
        mLocationManager.recordLocation(recordLocation);

        setCameraParametersWhenIdle(UPDATE_PARAM_PREFERENCE);
    }

    // Preview texture has been copied. Now camera can be released and the
    // animation can be started.
    @Override
    public void onPreviewTextureCopied() {
        mHandler.sendEmptyMessage(SWITCH_CAMERA);
    }

    @Override
    public void onCaptureTextureCopied() {
    }

    private void resetScreenOn() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mHandler.removeMessages(CLEAR_SCREEN_DELAY);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.sendEmptyMessageDelayed(CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    private void initializeCapabilities() {
        mInitialParams = mCameraDevice.getParameters();
        mAeLockSupported = CameraUtil.isAutoExposureLockSupported(mInitialParams);
        mContinuousFocusSupported = mInitialParams.getSupportedFocusModes().contains(
                CameraUtil.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    @Override
    public void onCountDownFinished() {
        mSnapshotOnIdle = false;
    }

    @Override
    public int onZoomChanged(int index) {
        // Not useful to change zoom value when the activity is paused.
        if (mPaused) return index;
        if (mParameters == null || mCameraDevice == null) return index;
        // Set zoom parameters asynchronously
        mCameraDevice.setParameters(mParameters);
        Parameters p = mCameraDevice.getParameters();
        if (p != null) return p.getZoom();
        return index;
    }

    @Override
    public int getCameraState() {
        return mCameraState;
    }

    @Override
    public void onMediaSaveServiceConnected(MediaSaveService s) {
        // We set the listener only when both service and shutterbutton
        // are initialized.
        if (mFirstTimeInitialized) {
            s.setListener(this);
        }
    }

    @Override
    public void onPreviewFocusChanged(boolean previewFocused) {
        mUI.onPreviewFocusChanged(previewFocused);
    }

    @Override
    public boolean arePreviewControlsVisible() {
        return mUI.arePreviewControlsVisible();
    }

    // For debugging only.
    public void setDebugUri(Uri uri) {
        mDebugUri = uri;
    }

    // For debugging only.
    private void saveToDebugUri(byte[] data) {
        if (mDebugUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = mContentResolver.openOutputStream(mDebugUri);
                outputStream.write(data);
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception while writing debug jpeg file", e);
            } finally {
                CameraUtil.closeSilently(outputStream);
            }
        }
    }

	@Override
	public void onQueueStatus(boolean full) {
		mCanTakePhoto = !full;
	}

    @Override
    public void togglePreviewTimeoutScreen(boolean showTimeoutScreen){
        if(showTimeoutScreen) mUI.showPreviewTimeoutScreen();
        else mUI.hidePreviewTimeoutScreen();
    }

    @Override
    public void setOnPreviewChangedListener(OnPreviewChangedListener listener){
        mUI.setOnPreviewChangedListener(listener);
    }
}
