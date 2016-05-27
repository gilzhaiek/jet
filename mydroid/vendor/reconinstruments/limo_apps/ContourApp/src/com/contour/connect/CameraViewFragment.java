package com.contour.connect;

import java.util.Arrays;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.contour.api.CameraComms;
import com.contour.api.CameraSettings;
import com.contour.api.CameraStatus;
import com.contour.connect.debug.CLog;
import com.contour.connect.views.RecordButton;
import com.contour.connect.views.RecordButton.OnRecordButtonClicked;
import com.contour.utils.api.actionbarcompat.ActionBarActivity;
import com.google.analytics.tracking.android.EasyTracker;

public class CameraViewFragment extends Fragment implements OnRecordButtonClicked {
    public final static String TAG             = "CameraViewFragment";
    private boolean mDebug = true;

    String  CAM_STATES[]; 
    ImageView           mLiveVideoView;
    LinearLayout        mStatusBar;
    TextView            mLowMemoryTitleBar;
    ImageView           mGpsHdopView;
    TextView            mGpsTitleView;

    ImageView           mSwitchPosView;
    TextView            mStorageView;
    ImageView           mBatteryLevelView; 
    ImageView           mStorageImageView; 
    TextView            mStatusSummaryTextView; 
    
    View                 mLivePreviewDisabledView; 
    TextView            mLivePreviewDisabledText; 

    String              mLivePreviewDisabled120FpsText;

    
    String              mRecordTimeElapsedString;
    String              mStatusBarPhotoLabel;
    String              mGpsOffText;

    TextView            mRecordingTimeElapsedView; 
    
    RecordButton        mRecordButton;
    
//    ImageView mRecordActiveAnimView;
//    ImageView mRecordButtonBackgroundView;
//    Animation mRecordingAnim;
//    Animation mRecordButtonBackgroundAnim;

    boolean             mRecording;
    int                 mRecordOffsetTime;
    
    private CameraStatus mLastCameraStatus;
    private CameraSettings mLastCameraSettings; 
    
    private boolean mLivePreviewDisabled = false;
    
    CameraViewUpdateListener mCameraViewUpdateListener;
    public static CameraViewFragment newInstance(boolean debug) {
        CameraViewFragment f = new CameraViewFragment();
        f.mDebug = debug;
        return f;
    }

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(mDebug) CLog.out(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(mDebug) CLog.out(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.camera_view, container, false);
        mLiveVideoView = (ImageView) v.findViewById(R.id.camera_live_video);
        mStatusBar = (LinearLayout) inflater.inflate(R.layout.status_bar,container,false);
        mLowMemoryTitleBar = (TextView) inflater.inflate(R.layout.text_title_bar, container,false);
        mLowMemoryTitleBar.setText(R.string.errormsgsdcardlow);

        mGpsHdopView = (ImageView) mStatusBar.findViewById(R.id.status_gps);
        mGpsTitleView  = (TextView) mStatusBar.findViewById(R.id.status_gps_text);
        mSwitchPosView = (ImageView) mStatusBar.findViewById(R.id.status_switchpos);
        mStorageView = (TextView) mStatusBar.findViewById(R.id.status_sdcard);
        mBatteryLevelView = (ImageView) mStatusBar.findViewById(R.id.battery_image);
        mStorageImageView = (ImageView) mStatusBar.findViewById(R.id.status_sdcard_msg);
        mStatusSummaryTextView  = (TextView) mStatusBar.findViewById(R.id.status_summary);
        mRecordButton = (RecordButton) v.findViewById(R.id.record_button);
        mRecordButton.setOnRecordButtonClicked(this);
        mRecordButton.setFocusableInTouchMode(true);
        mRecordButton.requestFocus();
        mRecordingTimeElapsedView = (TextView) v.findViewById(R.id.recording_time_label);
        mLivePreviewDisabledView = v.findViewById(R.id.live_preview_disabled_layout);
        mLivePreviewDisabledText = (TextView) v.findViewById(R.id.live_preview_disabled_text);
        
        mRecordButton.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            	Log.i("CameraViewFragment","ONFOCUS! Hadfocus: "+ hasFocus);
            }
        });
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(mDebug) CLog.out(TAG, "onActivityCreated", savedInstanceState);
        CAM_STATES = getActivity().getResources().getStringArray(R.array.camera_states);
        


        

//        mRecordActiveAnimView =  (ImageView) getActivity().findViewById(R.id.record_button_anim_view);
//        mRecordActiveAnimView.setVisibility(View.INVISIBLE);
        


        mLivePreviewDisabled120FpsText = String.format(getActivity().getString(R.string.statusmsglivepreviewsupport),120) + getActivity().getString(R.string.fps);
        
//        mRecordingAnim = AnimationUtils.loadAnimation(this.getActivity(), R.anim.recording_active);
        mRecordTimeElapsedString = getActivity().getResources().getString(R.string.record_time_elapsed);
        mStatusBarPhotoLabel = getActivity().getResources().getString(R.string.status_photo_label);
//        mRecordButtonBackgroundAnim = AnimationUtils.loadAnimation(this.getActivity(), R.anim.record_button_bg_anim);
//        mRecordButtonBackgroundView = (ImageView) getActivity().findViewById(R.id.record_button_bg_view);
        mGpsOffText = getActivity().getResources().getString(R.string.gps) + ' ' + getActivity().getResources().getString(R.string.off).toUpperCase();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(mDebug) CLog.out(TAG, "onStart", mRecordButton.isEnabled(),mRecordButton.isSelected(),Arrays.toString(mRecordButton.getDrawableState()));

        mLastCameraStatus = mCameraViewUpdateListener.onCameraViewStarted(this);
        if(mLastCameraStatus!=null) {
            setCameraStatus(mLastCameraStatus);
            if(mLastCameraStatus.cameraState == CameraStatus.CAMERA_STATE_RECORDING) {
                onRecordingStarted(mLastCameraStatus.elapsedRecordSeconds);
            }
        } else {
//            ContourToast.showText(getActivity(), "Last Camera Status is NULL", Toast.LENGTH_LONG);
        }
    }

    
    @Override
    public void onStop () {
        super.onStop();
    }
    
    @Override
    public void onResume () {
        super.onResume();
        getActivity().registerReceiver(this.mCameraReceiver, initCameraReceiver());        
        mCameraViewUpdateListener.onStartVideoStream();
        if(mDebug) CLog.out(TAG, "onResume",mRecordButton.getDrawable(), mRecordButton.getDrawable().getCurrent());
    }

    @Override
    public void onPause () {
        super.onPause();
        mLivePreviewDisabled = false;
        sHandler.removeCallbacks(this.mLowMemoryRunner);
        getActivity().unregisterReceiver(mCameraReceiver);
    }
    
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCameraViewUpdateListener = (CameraViewUpdateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()+ " must implement CameraViewUpdateListener");
        }
    }
    
    @Override
    public void onDestroy () {
        super.onDestroy();
        mCameraViewUpdateListener.onCameraViewStopped();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mDebug) CLog.out(TAG, "onSaveInstanceState",outState);
        super.onSaveInstanceState(outState);
    }

    public void setCameraStatus(CameraStatus cameraStatus) {
        setGpsHdop(cameraStatus.gpsHdop);
        setSwitchPos(cameraStatus.switchPos);
        setBatteryLevel(cameraStatus.batteryPercentage);
        setStorageViews(cameraStatus.storageKbRemaining,cameraStatus.storageCapacity);   
        //TODO Update camera status only when no isEquals Here
        mLastCameraStatus = cameraStatus;
        
        MainActivity mainActivity = (MainActivity)getActivity();
        CameraSettings cameraSettings = mainActivity.getLastCameraSettings();
        if(cameraSettings == null) cameraSettings = mLastCameraSettings;
        if (cameraSettings != null) {
            cameraSettings.switchPosition = cameraStatus.switchPos;
            if (cameraSettings.getGps() == true) {
                mGpsHdopView.setVisibility(View.VISIBLE);
                mGpsTitleView.setText(R.string.gps_title);
            } else {
                mGpsTitleView.setText(mGpsOffText);
                mGpsHdopView.setVisibility(View.GONE);  
            }
               
            int videoRes = cameraSettings.getVideoRes();
            
            if (videoRes == CameraSettings.VideoRes.CTRVideoRes848x480fps120) {
                if(mLivePreviewDisabled == false) {
                    mLivePreviewDisabled120FpsText = String.format(mainActivity.getString(R.string.statusmsglivepreviewsupport),cameraSettings.getVideoFormat() == 1 ? 120 : 100) + mainActivity.getString(R.string.fps);
                    mLivePreviewDisabledText.setText(mLivePreviewDisabled120FpsText);
                    mLivePreviewDisabled = true;     
                }
            } else {
                mLivePreviewDisabled = false;
            }
            
            if(mLivePreviewDisabled) {
                mLivePreviewDisabledView.setVisibility(View.VISIBLE);
            } else {
                mLivePreviewDisabledView.setVisibility(View.INVISIBLE);
            }

            setStatusSummary(cameraStatus.switchPos, cameraSettings.getVideoRes(), cameraSettings.getVideoFormat(), cameraSettings.getPhotoMode());
            if(cameraStatus.storageKbRemaining < 500)
                this.showLowMemoryLabel();                
        } else {
            Log.w(CLog.TAG,TAG +" :: Settings is NULL!");
        }
    }

    private void setStatusSummary(byte switchPos, int videoRes, int videoFormat, int photoMode) {
        int fpsH = 120;
        int fpsM = 60;
        int fpsL = 30;
        if(videoFormat == 0) {
            fpsH = 100;
            fpsM = 50;
            fpsL = 25;
        }
        String summaryStr;
        switch(videoRes) {                                                                
        case CameraSettings.VideoRes.CTRVideoRes1920x1080fps30: summaryStr = String.format( "1080p/%dFPS/125�", fpsL); break;
        case CameraSettings.VideoRes.CTRVideoRes1280x960fps30: summaryStr = String.format(  "960p/%dFPS/170�", fpsL); break;
        case CameraSettings.VideoRes.CTRVideoRes1280x720fps30: summaryStr = String.format(  "720p/%dFPS/170�", fpsL); break;
        case CameraSettings.VideoRes.CTRVideoRes1280x720fps60: summaryStr = String.format(  "720p/%dFPS/170�", fpsM); break;
        case CameraSettings.VideoRes.CTRVideoResContinuousPhoto: summaryStr = String.format(mStatusBarPhotoLabel, photoMode); break;
        case CameraSettings.VideoRes.CTRVideoRes848x480fps30: summaryStr = String.format(   "480p/%dFPS/170�", fpsL); break;
        case CameraSettings.VideoRes.CTRVideoRes848x480fps60: summaryStr = String.format(   "480p/%dFPS/170�", fpsM); break;
        case CameraSettings.VideoRes.CTRVideoRes848x480fps120: summaryStr = String.format(  "480p/%dFPS/170�", fpsH); break;
        default: 
            summaryStr = getActivity().getString(R.string.statusmsgloading);
        }   
        mStatusSummaryTextView.setText(summaryStr);
        mStatusBar.requestLayout();
    }


    private void setStorageViews(int storageKbRemaining, int storageCapacity) {
        int storageVal = Math.round( 100 *((float)storageKbRemaining / (float)storageCapacity) );
        mStorageImageView.setImageLevel(storageVal);
        
        String sdCardRemaining = String.format("%.1f", (storageKbRemaining / 1024.0));
        String sdCardCapacity = String.format("%.1f", (storageCapacity / 1024.0));
        mStorageView.setText(sdCardRemaining + '/' + sdCardCapacity + " GB");
    }
    private void setSwitchPos(int switchPos) {
        mSwitchPosView.setImageLevel(switchPos);
    }

    private void setGpsHdop(int hdop) {
        if(mDebug) CLog.out(TAG,"setGpsHdop",hdop);
        if (hdop < 1) {
            mGpsHdopView.setImageResource(R.drawable.gpsstatus0);
        } else if (hdop <= 1) {
            mGpsHdopView.setImageResource(R.drawable.gpsstatus5);
        } else if (hdop <= 2) {
            mGpsHdopView.setImageResource(R.drawable.gpsstatus4);
        } else if (hdop <= 5) {
            mGpsHdopView.setImageResource(R.drawable.gpsstatus3);
        } else if (hdop <= 10) {
            mGpsHdopView.setImageResource(R.drawable.gpsstatus2);
        } else if (hdop <= 20) {
            mGpsHdopView.setImageResource(R.drawable.gpsstatus1);
        } else {
            mGpsHdopView.setImageResource(R.drawable.gpsstatus0);
        }
    }
    
    private void setBatteryLevel(int batteryLevel) {
        mBatteryLevelView.setImageLevel(batteryLevel);
    }
    
    private void setRecordingElapsedLabel(int elapsedSec) {
        int elapsedHours = (elapsedSec / (60*60));
        int elapsedMins = Math.abs(elapsedSec / 60)  % 60;
        String text = String.format(mRecordTimeElapsedString, elapsedHours, elapsedMins,elapsedSec%60);
        mRecordingTimeElapsedView.setText(text);
    }
    
    public void setLiveVideoEnabled(boolean enabled) {
        if(enabled) {
            Drawable d = this.mLiveVideoView.getDrawable(); 
            if(d != null){
                d.setAlpha(255);
            }
        } else {
            Drawable d = this.mLiveVideoView.getDrawable(); 
            if(d != null){
                d.setAlpha(0);
            }
        }
    }

    private void showLowMemoryLabel() {
        ((ActionBarActivity) getActivity()).getActionBarHelper().setTitleView(mLowMemoryTitleBar);
        sHandler.removeCallbacks(this.mLowMemoryRunner);
        sHandler.postDelayed(mLowMemoryRunner, 1000);
    }
    
    @Override
    public void onRecordButtonClicked(boolean wantsRecord) {
        if(mDebug) CLog.out(TAG, "onRecordButtonClicked",wantsRecord);

        if(wantsRecord) {
            mCameraViewUpdateListener.onStartRecordRequested();
        } else {
            mCameraViewUpdateListener.onStopRecordRequested();
        }      
    }
    
    private void updateViewForRecordStarted() {
        if(mDebug) CLog.out(TAG, "updateViewForRecordStarted");
        mRecordButton.setRecordingOn();
//        mRecordingAnim.reset();
        // mRecordButtonBackgroundAnim.reset();
//        mRecordActiveAnimView.clearAnimation();
        // mRecordButtonBackgroundView.clearAnimation();
//        mRecordActiveAnimView.startAnimation(mRecordingAnim);
        mRecordingTimeElapsedView.setVisibility(View.VISIBLE);
        mRecordButton.getBackground().setAlpha(0);
//        if (this.mLivePreviewDisabledForRecording) {
//            mLivePreviewDisabledView.setVisibility(View.VISIBLE);
//        } else if (mLivePreviewDisabled == false) {
//            mLivePreviewDisabledView.setVisibility(View.INVISIBLE);
//        }
        	
        // mRecordButtonBackgroundView.startAnimation(mRecordButtonBackgroundAnim);
    }

    private void updateViewForRecordStopped() {
        if(mDebug) CLog.out(TAG, "updateViewForRecordStopped");
        mRecordButton.setRecordingOff();
//        mRecordActiveAnimView.clearAnimation();
        mRecordingTimeElapsedView.setVisibility(View.INVISIBLE);
        mRecordButton.getBackground().setAlpha(255 );
//        if (mLivePreviewDisabled == false) {
//            mLivePreviewDisabledView.setVisibility(View.INVISIBLE);
//
//        }
    }
    
    private IntentFilter initCameraReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(CameraComms.ACTION_RECORDING_STARTED);  
        filter.addAction(CameraComms.ACTION_RECORDING_STOPPED);
        filter.addAction(CameraComms.ACTION_STATUS);
        filter.addAction(CameraComms.ACTION_SETTINGS);
        filter.addAction(CameraComms.ACTION_VIDEOFRAME);
        filter.addAction(CameraComms.ACTION_CAMERA_CONFIGURE_DISABLED);
        filter.addAction(CameraComms.ACTION_CAMERA_CONFIGURE_ENABLED);
        return filter;
    }
    
    public void setLiveVideoImage(Bitmap bitmap) {
        if(mLiveVideoView != null)
            mLiveVideoView.setImageBitmap(bitmap);
    }
    
    private void onRecordingStarted(int recordTimerTime) {
        mRecording = true;
        updateViewForRecordStarted();
            mRecordingTimer.setOffset(recordTimerTime);
        mRecordingTimer.start();
        
        String cameraModel = ((MainActivity) getActivity()).getCameraModel();
        if(cameraModel != null)
            EasyTracker.getTracker().trackEvent(Constants.GA_CATEGORY_RECORD, Constants.GA_ACTION_RECORDSTARTED, cameraModel, -1L);
    }
    
    private final BroadcastReceiver mCameraReceiver  = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(mDebug) CLog.out(TAG,"Received Action",action);
            if(action.equals(CameraComms.ACTION_RECORDING_STARTED)) {
                if(!mRecording) {
                    int recordTimerTime = 0;
                    if(intent.hasExtra(CameraComms.EXTRA_INTEGER))
                        recordTimerTime = intent.getIntExtra(CameraComms.EXTRA_INTEGER, 0);
                    onRecordingStarted(recordTimerTime);
                }
            }
            if(action.equals(CameraComms.ACTION_RECORDING_STOPPED)) {
                mRecording = false;
                updateViewForRecordStopped();
                mRecordingTimer.cancel();
            } else if (action.equals(CameraComms.ACTION_STATUS)) {
                Parcelable p = intent.getParcelableExtra(CameraComms.EXTRA_PARCELABLE);
                if(p != null) {
                    mLastCameraStatus = (CameraStatus) p;
                    if(mDebug) CLog.out(TAG, "broadcast receiver acquired status", mLastCameraStatus);
                    setCameraStatus(mLastCameraStatus);
                    if(mLastCameraStatus.cameraState == CameraStatus.CAMERA_STATE_RECORDING) {
                        if(!mRecording)
                            onRecordingStarted(mLastCameraStatus.elapsedRecordSeconds);
                    }
                } else {
                    Log.w(CLog.TAG, TAG + ":: broadcast receiver acquired NULL status");
                }
            }
            if (action.equals(CameraComms.ACTION_VIDEOFRAME)) {
                byte[] frameData = intent.getByteArrayExtra(CameraComms.EXTRA_BYTEARRAY);
                if(mDebug) CLog.out(TAG,"VIDEO FRAME",frameData.length);
                Bitmap bmp = BitmapFactory.decodeByteArray(frameData, 0, frameData.length);
                setLiveVideoImage(bmp);
            } if (action.equals(CameraComms.ACTION_CAMERA_CONFIGURE_ENABLED)) {
                if(mDebug) CLog.out(TAG,"ACTION_CAMERA_CONFIGURE_ENABLED",mRecordButton.isWaitingForCamera());
                mRecordButton.setRecordEnabled();
                ((ActionBarActivity)getActivity()).getActionBarHelper().setTitleView(mStatusBar);
            } else if (action.equals(CameraComms.ACTION_CAMERA_CONFIGURE_DISABLED)) {
                if(mDebug) CLog.out(TAG,"ACTION_CAMERA_CONFIGURE_DISABLED",mRecordButton.isWaitingForCamera());
                mRecordButton.setRecordDisabled();
            } else if(action.equals(CameraComms.ACTION_SETTINGS)) {
                if(mDebug) CLog.out(TAG,"ACTION_SETTINGS",mRecordButton.isWaitingForCamera());

                CameraSettings settings = intent.getParcelableExtra(CameraComms.EXTRA_PARCELABLE);
                try {
                    mLastCameraSettings = (CameraSettings) settings.clone();
                } catch (CloneNotSupportedException e) {
                    mLastCameraSettings = settings;
                    e.printStackTrace();
                }
                if(mLastCameraStatus != null) {
                    setCameraStatus(mLastCameraStatus);
                }
            }
        }
    };
    
    RecordingTimer mRecordingTimer = new RecordingTimer(Long.MAX_VALUE, 1000);
    
    class RecordingTimer extends CountDownTimer {

        public RecordingTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public int offset = 0;
        
        public void onTick(long millisUntilFinished) {
            setRecordingElapsedLabel( (int) ((Long.MAX_VALUE - millisUntilFinished) / 1000) + offset);
        }

        public void onFinish() {

        }
        
        public void setOffset(int offset) {
            this.offset = offset;
        }
     };
    
     private static  Handler sHandler = new Handler();
     private final Runnable mLowMemoryRunner = new Runnable() {
         public void run() {
             MainActivity m = (MainActivity)CameraViewFragment.this.getActivity();
             if(m != null && m.hasWindowFocus()) {
                 m.getActionBarHelper().setTitleView(mStatusBar);
             }
         }
     };
     
     
    public interface CameraViewUpdateListener {
        public void onStartVideoStream();
        public void onStartRecordRequested();
        public void onStopRecordRequested();
        public CameraStatus onCameraViewStarted(CameraViewFragment fragment);
        public void onCameraViewStopped();

    }

  
  
}
