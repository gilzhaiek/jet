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

package com.reconinstruments.camera.app;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ShareActionProvider;

import com.reconinstruments.camera.CameraHolder;
import com.reconinstruments.camera.CameraModule;
import com.reconinstruments.camera.CameraSettings;
import com.reconinstruments.camera.ImageTaskManager;
import com.reconinstruments.camera.OnPreviewChangedListener;
import com.reconinstruments.camera.R;
import com.reconinstruments.camera.data.CameraDataAdapter;
import com.reconinstruments.camera.data.CameraPreviewData;
import com.reconinstruments.camera.data.FixedFirstDataAdapter;
import com.reconinstruments.camera.data.LocalData;
import com.reconinstruments.camera.data.LocalDataAdapter;
import com.reconinstruments.camera.data.LocalMediaObserver;
import com.reconinstruments.camera.manager.CameraManager;
import com.reconinstruments.camera.manager.CameraManager.CameraOpenErrorCallback;
import com.reconinstruments.camera.photo.PhotoModule;
import com.reconinstruments.camera.ui.CameraControls;
import com.reconinstruments.camera.ui.CameraSideMenu;
import com.reconinstruments.camera.ui.FilmStripView;
import com.reconinstruments.camera.ui.ModuleSwitcher;
import com.reconinstruments.camera.ui.OnScreenHint;
import com.reconinstruments.camera.util.CameraUtil;
import com.reconinstruments.camera.util.StorageUtil;
import com.reconinstruments.camera.util.UsageStatistics;
import com.reconinstruments.camera.video.VideoModule;
import com.reconinstruments.utils.SettingsUtil;

/**
 * This is the main UI for the Camera
 */
public class CameraActivity extends Activity
	implements ModuleSwitcher.ModuleSwitchListener, ShareActionProvider.OnShareTargetSelectedListener,
               OnPreviewChangedListener {
	private static final String TAG = CameraActivity.class.getSimpleName();
	private static int VIDEO_RECORD_DURATION = 15;
	
	public static final int HIDE_INDICATOR = 1;
	public static final int SHOW_INDICATOR = 2;
    public static final int SHOW_IDLE_SCREEN = 3;
    public static final long IDLE_COUNT = 1000; // 1 second intervals
    public static final long IDLE_MAX_DURATION = 120 * IDLE_COUNT; // 2 minute timeout
	
    /**
     * Request code from an activity we started that indicated that we do not
     * want to reset the view to the preview in onResume.
     */
    public static final int REQ_CODE_DONT_SWITCH_TO_PREVIEW = 142;

    private boolean mVideoFirstRun = true;
    private boolean mPhotoFirstRun = true;
    private boolean mPreviewTimedOut = false;

    /** Whether onResume should reset the view to the preview. */
    private boolean mResetToPreviewOnResume = true;

    /** This data adapter is used by FilmStripView. */
    private LocalDataAdapter mDataAdapter;
    /** This data adapter represents the real local camera data. */
    private LocalDataAdapter mWrappedDataAdapter;

    private int mCurrentModuleIndex;
    private CameraModule mCurrentModule;
    private View mCameraModuleRootView;
    
    private FilmStripView mFilmStripView;
    
    private CameraSideMenu mSideMenu;

    private FrameLayout mCamVideoSelectWrapper;
    private ImageView mGoToGalleryIcon;
    private ImageView mCornerGradient;
    private View mSelectedOutline;
    
    private int mResultCodeForTesting;
    private Intent mResultDataForTesting;
    
    private OnScreenHint mStorageHint;
    private long mStorageSpaceBytes = StorageUtil.LOW_STORAGE_THRESHOLD_BYTES;

    @SuppressLint("HandlerLeak")
	public Handler mMainHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HIDE_INDICATOR:
				mSideMenu.setVisibility(View.INVISIBLE);
				mSelectedOutline.setVisibility(View.INVISIBLE);
				break;
			case SHOW_INDICATOR:
				mSideMenu.setVisibility(View.VISIBLE);
				mSelectedOutline.setVisibility(View.VISIBLE);
				break;
            case SHOW_IDLE_SCREEN:
                mCurrentModule.togglePreviewTimeoutScreen(true);
                break;
			default:
				break;
			}
		}
    	
    };

    public Handler mIdleHandler = new Handler();
    protected volatile long mUserIdleTime = 0;
    protected Runnable idleCounter = new Runnable() {
        @Override
        public void run() {
            mUserIdleTime += IDLE_COUNT;
            if(mUserIdleTime >= IDLE_MAX_DURATION){
                mMainHandler.sendEmptyMessage(SHOW_IDLE_SCREEN);
                resetIdleCounter(false);
            }
            mIdleHandler.postDelayed(this, IDLE_COUNT);
        }
    };

    private CameraPreviewData mCameraPreviewData;

    private LocalMediaObserver mLocalImagesObserver;
    private LocalMediaObserver mLocalVideosObserver;

    private MediaSaveService mMediaSaveService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder b) {
            mMediaSaveService = ((MediaSaveService.LocalBinder) b).getService();
            mCurrentModule.onMediaSaveServiceConnected(mMediaSaveService);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (mMediaSaveService != null) {
                mMediaSaveService.setListener(null);
                mMediaSaveService = null;
            }
        }
    };

    private CameraOpenErrorCallback mCameraOpenErrorCallback =
            new CameraOpenErrorCallback() {
                @Override
                public void onCameraDisabled(int cameraId) {
                    UsageStatistics.onEvent(UsageStatistics.COMPONENT_CAMERA,
                            UsageStatistics.ACTION_OPEN_FAIL, "security");

                    CameraUtil.showErrorAndFinish(CameraActivity.this,
                            R.string.camera_disabled);
                }

                @Override
                public void onDeviceOpenFailure(int cameraId) {
                    UsageStatistics.onEvent(UsageStatistics.COMPONENT_CAMERA,
                            UsageStatistics.ACTION_OPEN_FAIL, "open");

                    CameraUtil.showErrorAndFinish(CameraActivity.this,
                            R.string.cannot_connect_camera);
                }

                @Override
                public void onReconnectionFailure(CameraManager mgr) {
                    UsageStatistics.onEvent(UsageStatistics.COMPONENT_CAMERA,
                            UsageStatistics.ACTION_OPEN_FAIL, "reconnect");

                    CameraUtil.showErrorAndFinish(CameraActivity.this,
                            R.string.cannot_connect_camera);
                }
            };

    private String fileNameFromDataID(int dataID) {
        final LocalData localData = mDataAdapter.getLocalData(dataID);

        File localFile = new File(localData.getPath());
        return localFile.getName();
    }

    private FilmStripView.Listener mFilmStripListener =
            new FilmStripView.Listener() {

                @Override
                public void onDataFullScreenChange(int dataID, boolean full) {
                    boolean isCameraID = isCameraPreview(dataID);
                    if (!isCameraID) {
                        if (!full) {
                            // Always show action bar in filmstrip mode
                        }
                    }
                }

                /**
                 * Check if the local data corresponding to dataID is the camera
                 * preview.
                 *
                 * @param dataID the ID of the local data
                 * @return true if the local data is not null and it is the
                 *         camera preview.
                 */
                private boolean isCameraPreview(int dataID) {
                    LocalData localData = mDataAdapter.getLocalData(dataID);
                    if (localData == null) {
                        Log.w(TAG, "Current data ID not found.");
                        return false;
                    }
                    return localData.getLocalDataType() == LocalData.LOCAL_CAMERA_PREVIEW;
                }

                @Override
                public void onReload() {
                    setPreviewControlsVisibility(true);
                }

                @Override
                public void onCurrentDataCentered(int dataID) {}

                @Override
                public void onCurrentDataOffCentered(int dataID) {}

                @Override
                public void onDataFocusChanged(final int dataID, final boolean focused) {}
            };

    public void gotoGallery() {
        UsageStatistics.onEvent(UsageStatistics.COMPONENT_CAMERA, UsageStatistics.ACTION_FILMSTRIP,
                "thumbnailTap");

        mFilmStripView.getController().goToNextItem();
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
        int currentDataId = mFilmStripView.getCurrentId();
        if (currentDataId < 0) {
            return false;
        }
        UsageStatistics.onEvent(UsageStatistics.COMPONENT_CAMERA, UsageStatistics.ACTION_SHARE,
                intent.getComponent().getPackageName(), 0,
                UsageStatistics.hashFileName(fileNameFromDataID(currentDataId)));
        return true;
    }

    private ImageTaskManager.TaskListener mPlaceholderListener =
            new ImageTaskManager.TaskListener() {

                @Override
                public void onTaskQueued(String filePath, final Uri imageUri) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyNewMedia(imageUri);
                            int dataID = mDataAdapter.findDataByContentUri(imageUri);
                            if (dataID != -1) {
                                LocalData d = mDataAdapter.getLocalData(dataID);
                                mDataAdapter.updateData(dataID, d);
                            }
                        }
                    });
                }

                @Override
                public void onTaskDone(String filePath, final Uri imageUri) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mDataAdapter.refresh(getContentResolver(), imageUri);
                        }
                    });
                }

                @Override
                public void onTaskProgress(String filePath, Uri imageUri, int progress) {
                    // Do nothing
                }
    };

    public MediaSaveService getMediaSaveService() {
        return mMediaSaveService;
    }

    public void notifyNewMedia(Uri uri) {
        ContentResolver cr = getContentResolver();
        String mimeType = cr.getType(uri);
        if (mimeType.startsWith("video/")) {
            sendBroadcast(new Intent(CameraUtil.ACTION_NEW_VIDEO, uri));
            mDataAdapter.addNewVideo(cr, uri);
        } else if (mimeType.startsWith("image/")) {
            CameraUtil.broadcastNewPicture(this, uri);
            mDataAdapter.addNewPhoto(cr, uri);
        } else if (mimeType.startsWith("application/stitching-preview")) {
            mDataAdapter.addNewPhoto(cr, uri);
        } else {
            android.util.Log.w(TAG, "Unknown new media with MIME type:"
                    + mimeType + ", uri:" + uri);
        }
    }

    private void removeData(int dataID) {
        mDataAdapter.removeData(CameraActivity.this, dataID);
        performDeletion();
    }

    private void bindMediaSaveService() {
        Intent intent = new Intent(this, MediaSaveService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindMediaSaveService() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }

    private boolean isCaptureIntent() {
        if (MediaStore.ACTION_VIDEO_CAPTURE.equals(getIntent().getAction())
                || MediaStore.ACTION_IMAGE_CAPTURE.equals(getIntent().getAction())
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Log.d(TAG, ">>>>> Launching CAMERA Activity");
        setContentView(R.layout.camera_filmstrip);
        mCamVideoSelectWrapper = (FrameLayout) findViewById(R.id.cam_video_select_wrapper);
        mSelectedOutline = findViewById(R.id.selected_outline);
        mGoToGalleryIcon = (ImageView) findViewById(R.id.go_to_gallery_icon);
        mCornerGradient = (ImageView) findViewById(R.id.corner_gradient);

	VIDEO_RECORD_DURATION = SettingsUtil
	    .getCachableSystemIntOrSet(this, SettingsUtil.VIDEO_RECORD_DURATION,
				       SettingsUtil.DEFAULT_VIDEO_RECORD_DURATION);
        
        // Check if this is in the secure camera mode.
        Intent intent = getIntent();
        String action = intent.getAction();
        if(!intent.hasExtra(MediaStore.EXTRA_DURATION_LIMIT)){
        	intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, VIDEO_RECORD_DURATION);
        }

        LayoutInflater inflater = getLayoutInflater();
        
        View rootLayout = inflater.inflate(R.layout.camera, null, false);
        mCameraModuleRootView = rootLayout.findViewById(R.id.camera_app_root);
        mCameraPreviewData = new CameraPreviewData(rootLayout,
                FilmStripView.ImageData.SIZE_FULL,
                FilmStripView.ImageData.SIZE_FULL);
        // Put a CameraPreviewData at the first position.
        mWrappedDataAdapter = new FixedFirstDataAdapter(
                new CameraDataAdapter(new ColorDrawable(
                        getResources().getColor(R.color.photo_placeholder))),
                mCameraPreviewData);
        mFilmStripView = (FilmStripView) findViewById(R.id.filmstrip_view);
        mFilmStripView.setViewGap(
                getResources().getDimensionPixelSize(R.dimen.camera_film_strip_gap));
        // Set up the camera preview first so the preview shows up ASAP.
        mFilmStripView.setListener(mFilmStripListener);
        
        mSideMenu = (CameraSideMenu) findViewById(R.id.filmstrip_sidemenu);

        int moduleIndex = -1;
        if (MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(getIntent().getAction())
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(getIntent().getAction())) {
            moduleIndex = ModuleSwitcher.VIDEO_MODULE_INDEX;
        } else if (MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA.equals(getIntent().getAction())
                || MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(getIntent()
                        .getAction())) {
            moduleIndex = ModuleSwitcher.PHOTO_MODULE_INDEX;
        } else if (MediaStore.ACTION_IMAGE_CAPTURE.equals(getIntent().getAction())
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction())) {
            moduleIndex = ModuleSwitcher.PHOTO_MODULE_INDEX;
        } else {
            // If the activity has not been started using an explicit intent,
            // read the module index from the last time the user changed modes
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            moduleIndex = prefs.getInt(CameraSettings.KEY_STARTUP_MODULE_INDEX, -1);
            if (moduleIndex < ModuleSwitcher.PHOTO_MODULE_INDEX
            		|| moduleIndex > ModuleSwitcher.VIDEO_MODULE_INDEX) { // FALLBACK
                moduleIndex = ModuleSwitcher.PHOTO_MODULE_INDEX;
            }
        }
        
        setModuleFromIndex(moduleIndex);
        
        mDataAdapter = mWrappedDataAdapter;
        mFilmStripView.setDataAdapter(mDataAdapter);
        if (!isCaptureIntent()) {
        	mDataAdapter.requestLoad(getContentResolver());
        }
        

        mLocalImagesObserver = new LocalMediaObserver();
        mLocalVideosObserver = new LocalMediaObserver();

        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
                mLocalImagesObserver);
        getContentResolver().registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true,
                mLocalVideosObserver);
    }

    @Override
    public void onPause() {
        // Delete photos that are pending deletion
        mCurrentModule.onPauseBeforeSuper();
        super.onPause();
        mCurrentModule.onPauseAfterSuper();

        mLocalImagesObserver.setActivityPaused(true);
        mLocalVideosObserver.setActivityPaused(true);
    }

    @Override
    public void onPreviewPaused(){
        // preview timed out, hide overlaying UI elements
        mPreviewTimedOut = true;
        hideGalleryIcon();
        mCamVideoSelectWrapper.setVisibility(View.GONE);
        resetIdleCounter(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CODE_DONT_SWITCH_TO_PREVIEW) {
            mResetToPreviewOnResume = false;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onResume() {
    	UsageStatistics.onEvent(UsageStatistics.COMPONENT_CAMERA,
                UsageStatistics.ACTION_FOREGROUNDED, this.getClass().getSimpleName());

        mCurrentModule.onResumeBeforeSuper();
        super.onResume();
        mCurrentModule.onResumeAfterSuper();
        resetIdleCounter(true);

        setSwipingEnabled(true);

        if (mResetToPreviewOnResume) {
            // Go to the preview on resume.
            mFilmStripView.getController().goToFirstItem();
        }
        // Default is showing the preview, unless disabled by explicitly
        // starting an activity we want to return from to the filmstrip rather
        // than the preview.
        mResetToPreviewOnResume = true;

        if (mLocalVideosObserver.isMediaDataChangedDuringPause()
                || mLocalImagesObserver.isMediaDataChangedDuringPause()) {
            mDataAdapter.requestLoad(getContentResolver());
        }
        mLocalImagesObserver.setActivityPaused(false);
        mLocalVideosObserver.setActivityPaused(false);
    }

    @Override
    public void onPreviewResumed(){
        // user input happened; resume camera preview, and restore overlaying UI
        mPreviewTimedOut = false;
        showGalleryIcon();
        mCamVideoSelectWrapper.setVisibility(View.VISIBLE);
        resetIdleCounter(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        bindMediaSaveService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindMediaSaveService();
    }

    @Override
    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mLocalImagesObserver);
        getContentResolver().unregisterContentObserver(mLocalVideosObserver);

        super.onDestroy();
    }

    @Override
    public void onUserInteraction(){
        if(!mPreviewTimedOut) resetIdleCounter(true);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mCurrentModule.onConfigurationChanged(config);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	/**
    	 * @NOTE For JET, OFM Select is same as "ENTER",
    	 * whereas BLE Remote center button is DPAD_CENTER
    	 */

        // if camera preview has timed out, block all input except SELECT and BACK
    	switch (keyCode) {
    	case KeyEvent.KEYCODE_DPAD_RIGHT:
            if(mPreviewTimedOut) return true;
			else if (mCurrentModule instanceof VideoModule && ((VideoModule) mCurrentModule).isRecording()) {
				//currently recording, do nothing
			}
			else {
				if (mCurrentModuleIndex < ModuleSwitcher.VIDEO_MODULE_INDEX) {
					onModuleSelected(mCurrentModuleIndex + 1);
				}
			}
    		return true;
    	case KeyEvent.KEYCODE_DPAD_LEFT:
            if(mPreviewTimedOut) return true;
			else if (mCurrentModule instanceof VideoModule && ((VideoModule) mCurrentModule).isRecording()) {
				//currently recording, do nothing
			}
			else {
				if (mCurrentModuleIndex > ModuleSwitcher.PHOTO_MODULE_INDEX) {
					onModuleSelected(mCurrentModuleIndex - 1);
				}
			}
    		return true;
    	case KeyEvent.KEYCODE_MEDIA_RECORD:
    	case KeyEvent.KEYCODE_DPAD_CENTER:
    	case KeyEvent.KEYCODE_ENTER:
    		switch (mCurrentModuleIndex) {
			case ModuleSwitcher.VIDEO_MODULE_INDEX:
			case ModuleSwitcher.PHOTO_MODULE_INDEX:
				mCurrentModule.onKeyDown(KeyEvent.KEYCODE_ENTER, event);
				return true;
			}
    		return true;
    	case KeyEvent.KEYCODE_DPAD_UP:
    	case KeyEvent.KEYCODE_DPAD_DOWN:
            if(mPreviewTimedOut) return true;
            else if (mCurrentModule instanceof VideoModule && ((VideoModule) mCurrentModule).isRecording()) {
				//currently recording, do nothing
			}
			else {
				Intent intent = new Intent(this, GalleryActivity.class);
				intent.putExtra("CALLED_FROM_CAMERA_ACTIVITY", true);
				startActivity(intent);
			}
			
			//don't call finish() if you want to return to CameraActivity 
			//after backing out of GalleryActivity
//			finish();
    		return true;
    	}
    	
        return super.onKeyDown(keyCode, event);
    }
    
    

    @Override
	public void onBackPressed() {
    	if (isRecording()) {
    		// DO NOTHING DURING RECORDING 
    		// TODO NOTIFY USER SOMETHING
    	} else {
    		finish();
    	}
	}

	protected void updateStorageSpace() {
        mStorageSpaceBytes = StorageUtil.getAvailableSpace();
    }

    public long getStorageSpaceBytes() {
        return mStorageSpaceBytes;
    }

    public void updateStorageSpaceAndHint() {
        updateStorageSpace();
        updateStorageHint(mStorageSpaceBytes);
    }

    protected void updateStorageHint(long storageSpace) {
        String message = null;
        if (storageSpace == StorageUtil.UNAVAILABLE) {
            message = getString(R.string.no_storage);
        } else if (storageSpace == StorageUtil.PREPARING) {
            message = getString(R.string.preparing_sd);
        } else if (storageSpace == StorageUtil.UNKNOWN_SIZE) {
            message = getString(R.string.access_sd_fail);
        } else if (storageSpace <= StorageUtil.LOW_STORAGE_THRESHOLD_BYTES) {
            message = getString(R.string.spaceIsLow_content);
        }

        if (message != null) {
            if (mStorageHint == null) {
                mStorageHint = OnScreenHint.makeText(this, message);
            } else {
                mStorageHint.setText(message);
            }
            mStorageHint.show();
        } else if (mStorageHint != null) {
            mStorageHint.cancel();
            mStorageHint = null;
        }
    }

    public void setResultEx(int resultCode) {
        mResultCodeForTesting = resultCode;
        setResult(resultCode);
    }

    public void setResultEx(int resultCode, Intent data) {
        mResultCodeForTesting = resultCode;
        mResultDataForTesting = data;
        setResult(resultCode, data);
    }

    public int getResultCode() {
        return mResultCodeForTesting;
    }

    public Intent getResultData() {
        return mResultDataForTesting;
    }

    @Override
    public void onModuleSelected(int moduleIndex) {
    	Log.d(TAG, "mCurrentModuleIndex : " + mCurrentModuleIndex + " onModuleSelected : " + moduleIndex);
        if (mCurrentModuleIndex == moduleIndex) {
            return;
        }

        CameraHolder.instance().keep();
        closeModule(mCurrentModule);
        setModuleFromIndex(moduleIndex);

        openModule(mCurrentModule);
        if (mMediaSaveService != null) {
            mCurrentModule.onMediaSaveServiceConnected(mMediaSaveService);
        }

        // Store the module index so we can use it the next time the Camera
        // starts up.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(CameraSettings.KEY_STARTUP_MODULE_INDEX, moduleIndex).apply();
    }

    /**
     * Sets the mCurrentModuleIndex, creates a new module instance for the given
     * index an sets it as mCurrentModule.
     */
    private void setModuleFromIndex(int moduleIndex) {
        mCurrentModuleIndex = moduleIndex;
        
        mSideMenu.setSelectedMenu(mCurrentModuleIndex);
        
        switch (moduleIndex) {
            case ModuleSwitcher.VIDEO_MODULE_INDEX:
                mCurrentModule = new VideoModule();
                mCurrentModule.init(this, mCameraModuleRootView, mVideoFirstRun);

                mVideoFirstRun = false;
                break;

            case ModuleSwitcher.PHOTO_MODULE_INDEX:
                mCurrentModule = new PhotoModule();
                mCurrentModule.init(this, mCameraModuleRootView, mPhotoFirstRun);
                mPhotoFirstRun = false;
                break;

            default:
                // In any other case, Fall back to photo mode.
                mCurrentModule = new PhotoModule();
                mCurrentModuleIndex = ModuleSwitcher.PHOTO_MODULE_INDEX;
                mCurrentModule.init(this, mCameraModuleRootView, mPhotoFirstRun);
                mPhotoFirstRun = false;
                break;
        }
        mCurrentModule.setOnPreviewChangedListener(this);
        
    }

    private void openModule(CameraModule module) {
        module.onResumeBeforeSuper();
        module.onResumeAfterSuper();
    }

    private void closeModule(CameraModule module) {
        module.onPauseBeforeSuper();
        module.onPauseAfterSuper();
        ((ViewGroup) mCameraModuleRootView).removeAllViews();
    }

    private void performDeletion() {
        mDataAdapter.executeDeletion(CameraActivity.this);

        int currentId = mFilmStripView.getCurrentId();
        mFilmStripListener.onCurrentDataCentered(currentId);
    }

    /**
     * Enable/disable swipe-to-filmstrip. Will always disable swipe if in
     * capture intent.
     *
     * @param enable {@code true} to enable swipe.
     */
    public void setSwipingEnabled(boolean enable) {
        if (isCaptureIntent()) {
            mCameraPreviewData.lockPreview(true);
        } else {
            mCameraPreviewData.lockPreview(!enable);
        }
    }


    /**
     * Check whether camera controls are visible.
     *
     * @return whether controls are visible.
     */
    private boolean arePreviewControlsVisible() {
        return mCurrentModule.arePreviewControlsVisible();
    }

    /**
     * Show or hide the {@link CameraControls} using the current module's
     * implementation of {@link #onPreviewFocusChanged}.
     *
     * @param showControls whether to show camera controls.
     */
    private void setPreviewControlsVisibility(boolean showControls) {
        mCurrentModule.onPreviewFocusChanged(showControls);
    }

    // Accessor methods for getting latency times used in performance testing
    public long getAutoFocusTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mAutoFocusTime : -1;
    }

    public long getShutterLag() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mShutterLag : -1;
    }

    public long getShutterToPictureDisplayedTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mShutterToPictureDisplayedTime : -1;
    }

    public long getPictureDisplayedToJpegCallbackTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mPictureDisplayedToJpegCallbackTime : -1;
    }

    public long getJpegCallbackFinishTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mJpegCallbackFinishTime : -1;
    }

    public long getCaptureStartTime() {
        return (mCurrentModule instanceof PhotoModule) ?
                ((PhotoModule) mCurrentModule).mCaptureStartTime : -1;
    }

    public boolean isRecording() {
        return (mCurrentModule instanceof VideoModule) ?
                ((VideoModule) mCurrentModule).isRecording() : false;
    }

    public CameraOpenErrorCallback getCameraOpenErrorCallback() {
        return mCameraOpenErrorCallback;
    }

    // For debugging purposes only.
    public CameraModule getCurrentModule() {
        return mCurrentModule;
    }
    
    public void hideGalleryIcon(){
    	mGoToGalleryIcon.setVisibility(View.INVISIBLE);
        mCornerGradient.setVisibility(View.INVISIBLE);
    }
    
    public void showGalleryIcon(){
    	mGoToGalleryIcon.setVisibility(View.VISIBLE);
        mCornerGradient.setVisibility(View.VISIBLE);
    }

    private void resetIdleCounter(boolean restart){
        mUserIdleTime = 0;
        mIdleHandler.removeCallbacks(idleCounter);
        if(restart) mIdleHandler.postDelayed(idleCounter, IDLE_COUNT);
    }

    public boolean isPreviewTimedOut(){
        return mPreviewTimedOut;
    }
}
