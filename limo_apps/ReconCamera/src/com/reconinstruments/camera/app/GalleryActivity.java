package com.reconinstruments.camera.app;

import java.io.File;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;
import com.reconinstruments.camera.R;
import com.reconinstruments.camera.data.CameraDataAdapter;
import com.reconinstruments.camera.data.GalleryDataAdapter;
import com.reconinstruments.camera.data.LocalData;
import com.reconinstruments.camera.data.LocalDataAdapter;
import com.reconinstruments.camera.data.LocalMediaObserver;
import com.reconinstruments.camera.ui.DeleteDialog;
import com.reconinstruments.camera.ui.FilmStripView;
import com.reconinstruments.camera.util.CameraUtil;
import com.reconinstruments.camera.util.UsageStatistics;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;

/**
 * This is the main UI for the Camera
 */
public class GalleryActivity extends FragmentActivity
	implements ShareActionProvider.OnShareTargetSelectedListener, DeleteDialog.DialogKeyListener {
	private static final String TAG = GalleryActivity.class.getSimpleName();

    public static final int MENU_CANCEL = 0;
    public static final int MENU_DELETE = 1;

	public static final int WHAT_SHARE_PRESSED			= 100;
	public static final int WHAT_DELETE_PRESSED			= 101;
	
	public static final int DELETE_VIDEO_ACTION			= 411;
	
    /** This data adapter is used by FilmStripView. */
    private LocalDataAdapter mDataAdapter;
    
    private FilmStripView mFilmStripView;
    private TextView mCountView;
   
    private DeleteDialog mDeleteDialog;
    
    private int mResultCodeForTesting;
    private Intent mResultDataForTesting;
    
	private boolean mCalledFromCameraActivity = false;
	
	private ImageView mGoToCameraIcon;
    private ImageView mCornerGradientFade;
	
	private Animation mFadeCountView;
	
	private ForwardInterpolator mForwardInterpolator;
	private ReverseInterpolator mReverseInterpolator;

    private LocalMediaObserver mLocalImagesObserver;
    private LocalMediaObserver mLocalVideosObserver;
    
    private TextView mNoMediaMessage;

    private RelativeLayout mLoadingItem;

    private MediaSaveService mMediaSaveService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder b) {
            mMediaSaveService = ((MediaSaveService.LocalBinder) b).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (mMediaSaveService != null) {
                mMediaSaveService.setListener(null);
                mMediaSaveService = null;
            }
        }
    };
    
    @SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case WHAT_DELETE_PRESSED:
				Log.i(TAG, "Delete pressed, removing item: " + msg.arg1);
				Toast.makeText(GalleryActivity.this, "Deleted", Toast.LENGTH_LONG).show();

                // if there will be at least 1 item left after deleting this item
                // show counter and other ui
                if(mFilmStripView.getTotalNumber() > 1){
                    setFilmStripModeVisibility(true, true);
                    mCountView.setVisibility(View.VISIBLE);
                    mFadeCountView.setInterpolator(mReverseInterpolator);
                    mCountView.startAnimation(mFadeCountView);
                }
				removeData(msg.arg1);
				mFilmStripView.goToFilmStrip();
                hideDeleteItemDialog();
				break;
			case WHAT_SHARE_PRESSED:
				// TODO SHARE
				break;
			}
		}

	};
	
    public void toggleEmptyGallery(boolean showEmptyGallery){
        mFilmStripView.resetScale();
        setFilmStripModeVisibility(true, !showEmptyGallery);
        refreshCounter();
        mNoMediaMessage.setVisibility((showEmptyGallery) ? View.VISIBLE : View.GONE);
        mNoMediaMessage.setTextSize(20);
        mNoMediaMessage.setText(R.string.empty_gallery_text);
        hideLoadingItem();
    }

    private void refreshCounter() {
        mCountView.setText((mFilmStripView.getCurrentId()+1) + " / " + mFilmStripView.getTotalNumber());
    }

    private String fileNameFromDataID(int dataID) {
        final LocalData localData = mDataAdapter.getLocalData(dataID);

        File localFile = new File(localData.getPath());
        return localFile.getName();
    }

    private FilmStripView.Listener mFilmStripListener =
            new FilmStripView.Listener() {

    			float filmStripScale = 1.0f;
            	float fullScreenScale = 1.0f;
            	int scaleDuration = 0;
            	
            	Animation scaleToFullScreen = null, scaleCameraIcon = null;
    			
                @Override
                public void onDataFullScreenChange(int dataID, boolean full) {
                }

                /**
                 * This callback is also called when the data is loaded
                 */
                @Override
                public void onReload() {
                	filmStripScale = mFilmStripView.getFilmStripScale();
                	fullScreenScale = mFilmStripView.getFullScreenScale();
                	scaleDuration = mFilmStripView.getScaleDuration()/2;
                	
                	if(scaleToFullScreen == null){
                		Point size = new Point();
                		getWindowManager().getDefaultDisplay().getSize(size);
                		int windowWidth = size.x;
                		int windowHeight = size.y;
                		scaleToFullScreen = new ScaleAnimation(filmStripScale, fullScreenScale, filmStripScale, fullScreenScale, windowWidth/2f, windowHeight/2f);
                	}

                	scaleToFullScreen.setInterpolator(mReverseInterpolator);
                	scaleToFullScreen.setDuration(scaleDuration);
                	scaleToFullScreen.setFillAfter(true);
                	refreshCounter();
                }

                @Override
                public void onCurrentDataCentered(int dataID) {
                	
                	refreshCounter();
                }

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
    
    public MediaSaveService getMediaSaveService() {
        return mMediaSaveService;
    }

    private void removeData(int dataID) {
    	Log.d(TAG,"In removeData()");
        mDataAdapter.removeData(GalleryActivity.this, dataID);
        refreshData();
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
    
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        
        Log.d(TAG, ">>>>> Launching GALLERY Activity");

        mCalledFromCameraActivity = getIntent().getBooleanExtra("CALLED_FROM_CAMERA_ACTIVITY", false);

        setContentView(R.layout.gallery_filmstrip);

        // Put a CameraPreviewData at the first position.
        mDataAdapter = new GalleryDataAdapter(
        		new CameraDataAdapter(new ColorDrawable(
                        getResources().getColor(R.color.photo_placeholder))));
        
        mFilmStripView = (FilmStripView) findViewById(R.id.gallery_filmstrip_view);
        mFilmStripView.setViewGap(
                getResources().getDimensionPixelSize(R.dimen.camera_film_strip_gap));
        // Set up the camera preview first so the preview shows up ASAP.
        mFilmStripView.setListener(mFilmStripListener);
        
        mCountView = (TextView) findViewById(R.id.gallery_filmstrip_count);
        
        mNoMediaMessage = (TextView) findViewById(R.id.gallery_no_media_message);
        mDataAdapter.setNoMediaTextView(mNoMediaMessage);
        
        mGoToCameraIcon = (ImageView) findViewById(R.id.go_to_camera_icon);
        
        mFilmStripView.setDataAdapter(mDataAdapter);
        mDataAdapter.requestLoad(getContentResolver());
        
        Log.d(TAG, "total number of images in mDataAdapter: " + mDataAdapter.getTotalNumber());
        
        mCornerGradientFade = (ImageView) findViewById(R.id.corner_gradient);

        mLoadingItem = (RelativeLayout) findViewById(R.id.loading_screen);
        
        setupAnimations();
        
        if(mLocalImagesObserver == null) mLocalImagesObserver = new LocalMediaObserver();
        if(mLocalVideosObserver == null) mLocalVideosObserver = new LocalMediaObserver();

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
        super.onPause();

        mLocalImagesObserver.setActivityPaused(true);
        mLocalVideosObserver.setActivityPaused(true);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	if(requestCode == CameraActivity.REQ_CODE_DONT_SWITCH_TO_PREVIEW){
    		if(resultCode == DELETE_VIDEO_ACTION){
    			int currentId = mFilmStripView.getCurrentId();
    			mHandler.sendMessage(Message.obtain(mHandler, WHAT_DELETE_PRESSED, currentId, -1));
                mCountView.setVisibility(View.INVISIBLE);
                refreshCounter();
    		}
    	}
    	else {
    		super.onActivityResult(requestCode, resultCode, data);
    	}
    }

    @Override
    public void onResume() {
        UsageStatistics.onEvent(UsageStatistics.COMPONENT_CAMERA,
                UsageStatistics.ACTION_FOREGROUNDED, this.getClass().getSimpleName());

        super.onResume();

        if (mLocalVideosObserver.isMediaDataChangedDuringPause()
                || mLocalImagesObserver.isMediaDataChangedDuringPause()) {
            mDataAdapter.requestLoad(getContentResolver());
        }
        
        if(mDataAdapter.getTotalNumber() == 0){
        	Log.d(TAG, "***** nothing in mDataAdapter!! *****");
            toggleEmptyGallery(true);
        }
        else {
        	toggleEmptyGallery(false);
        }
        mLocalImagesObserver.setActivityPaused(false);
        mLocalVideosObserver.setActivityPaused(false);
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
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int currentId = mFilmStripView.getCurrentId();
        LocalData localData = mDataAdapter.getLocalData(currentId);
    	
    	switch (keyCode) {
    	case KeyEvent.KEYCODE_DPAD_RIGHT:
            mFilmStripView.getController().goToNextItem();
			return true;
    	case KeyEvent.KEYCODE_DPAD_LEFT:
            mFilmStripView.getController().goToPreviousItem();
			return true;
    	case KeyEvent.KEYCODE_DPAD_UP:
    	case KeyEvent.KEYCODE_DPAD_DOWN:
    		if (!mFilmStripView.inFullScreen()) {
    			if(!mCalledFromCameraActivity){
	    			Intent intent = new Intent(this, CameraActivity.class);
	    			Log.d(TAG, "Starting CameraActivity.........");
	    			startActivity(intent);
    			}
    			Log.d(TAG, "Finishing Gallery Activity....");
    			finish();
    			return true;
    		}
    		return false;
    	case KeyEvent.KEYCODE_DPAD_CENTER:
    	case KeyEvent.KEYCODE_ENTER:
            if(localData.isPhoto()) {
                if (mFilmStripView.inFilmStrip()) {
                    //currently in filmstripView, want to
                    //focus on one image. Go to Fullscreen mode.
                    mFilmStripView.goToFullScreen();
                    setFilmStripModeVisibility(false, true);
                    mFadeCountView.setInterpolator(mForwardInterpolator);
                    mCountView.startAnimation(mFadeCountView);
                } else {
                    showDeleteItemDialog();
                }
            }
            else {
                //this gallery item is a video
                CameraUtil.playVideo(this, localData.getContentUri(), "testing");
            }

    		return true;
    	case KeyEvent.KEYCODE_BACK:
    		if((mFilmStripView.getCurrentId() == -1) || (localData == null)){
    			finish();
        		return true;
        	}

    		if (localData.isPhoto()) {
    			if(mFilmStripView.inFullScreen()){
                    mFilmStripView.goToFilmStrip();
                    setFilmStripModeVisibility(true, true);
                    mFadeCountView.setInterpolator(mReverseInterpolator);
                    mCountView.startAnimation(mFadeCountView);
                    return true;
    			}
    			else {
	    			Log.d(TAG, "Finishing Gallery Activity....");
    				finish();
    			}
    		} 
    		else {
				//this gallery item is a video
    			if(mFilmStripView.inFullScreen()){
    				mFilmStripView.goToFilmStrip();
                    mFadeCountView.setInterpolator(mReverseInterpolator);
                    mCountView.startAnimation(mFadeCountView);
    				setFilmStripModeVisibility(true, true);
    			}
    			else {
	    			Log.d(TAG, "Finishing Gallery Activity....");
    				finish();
    			}
    		}
    		
    		return false;
    	}
    	
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean handleKeyEvent(DialogInterface dialog, int index, int keyCode, KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode){
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    int currentId = mFilmStripView.getCurrentId();
                    if (mFilmStripView.getCurrentId() == -1) {
                        return true;
                    }
                    switch (index) {
                        case MENU_CANCEL:
                            break;
                        case MENU_DELETE:
                            mHandler.sendMessage(Message.obtain(mHandler, WHAT_DELETE_PRESSED, currentId, -1));
                            break;
                    }
                    dialog.dismiss();
                    break;
                default:
                    return false;
            }
            return true;
        }
        else return false;
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
   
	//updates the image counter in the gallery view
	private void refreshData() {
        int currentId = mFilmStripView.getCurrentId();
        mFilmStripListener.onCurrentDataCentered(currentId);
    }

	/**
	 * Helper method that turns on/off visibility for views defined 
	 * in the Gallery. Used mostly to get rid of icons/effects when 
	 * focusing on a single item in the Gallery.
	 * @param cameraIconVisible whether the "go_to_camera" icon and its gradient should be visible
     * @param counterVisible whether the item counter TextView should be visible
	 */
	private void setFilmStripModeVisibility(boolean cameraIconVisible, boolean counterVisible){
		int cameraIconVisiblity = (cameraIconVisible) ? View.VISIBLE : View.INVISIBLE;
        int itemCounterVisibility = (counterVisible) ? View.VISIBLE : View.INVISIBLE;
		mGoToCameraIcon.setVisibility(cameraIconVisiblity);
        mCornerGradientFade.setVisibility(cameraIconVisiblity);
        mCountView.setVisibility(itemCounterVisibility);
	}
	
	private void setupAnimations(){
        mFadeCountView = new AlphaAnimation(1.0f, 0.0f);
        mFadeCountView.setDuration(300);
        mFadeCountView.setInterpolator(mForwardInterpolator);
        mFadeCountView.setFillAfter(true);

        mForwardInterpolator = new ForwardInterpolator();
        mReverseInterpolator = new ReverseInterpolator();
	}
	
    private void showDeleteItemDialog(){
        ArrayList<Fragment> fragList = new ArrayList<Fragment>();
        fragList.add(new ReconJetDialogFragment(com.reconinstruments.commonwidgets.R.layout.carousel_text_only, "Cancel", 0, 0));
        fragList.add(new ReconJetDialogFragment(com.reconinstruments.commonwidgets.R.layout.carousel_text_only, "Delete", 0, 1));

        mDeleteDialog = new DeleteDialog(this, "DELETE PHOTO?", fragList, R.layout.delete_dialog_layout);
        mDeleteDialog.setDialogKeyListener(this);
        mDeleteDialog.show(getSupportFragmentManager().beginTransaction(), "DELETE_PHOTO");
    }

    private void hideDeleteItemDialog(){
        if(mDeleteDialog != null){
            mDeleteDialog.dismiss();
        }
    }

    public void hideLoadingItem(){
        mLoadingItem.setVisibility(View.GONE);
    }

	/**
	 * Interpolator used for which 'direction' to execute an
	 * animation in. ReverseInterpolator allows for 'reverse' animations.
	 * */	

	public static class ReverseInterpolator implements Interpolator {
		@Override
		public float getInterpolation(float paramFloat){
			return Math.abs(paramFloat - 1f);
		}
	}
	/**
	 * Interpolator used for which 'direction' to execute an
	 * animation in. ForwardInterpolator allows for 'forward' animations.
	 * */	
	public static class ForwardInterpolator implements Interpolator {
		@Override
		public float getInterpolation(float paramFloat){
			return Math.abs(paramFloat);
		}
	}
}
