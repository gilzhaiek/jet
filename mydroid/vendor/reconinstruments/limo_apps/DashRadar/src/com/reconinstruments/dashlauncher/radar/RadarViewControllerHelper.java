package com.reconinstruments.dashlauncher.radar;

import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.reconinstruments.dashlauncher.radar.RadarViewFrontControl.EControlEvents;
import com.reconinstruments.dashlauncher.radar.maps.drawings.MapDrawings;
import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;
import com.reconinstruments.dashlauncher.radar.maps.objects.MapImagesInfo;

public class RadarViewControllerHelper extends BaseControllerHelper  {
	private static final String TAG = "RadarViewControllerHelper";
		
	private final static float[] ZOOM_LEVELS						= {1200.0f, 800.0f, 500.0f, 250.0f};  // Meters
	//private final static float[] ZOOM_LEVELS						= {1000.0f, 1000.0f, 1000.0f, 1000.0f};  // Meters
	//private final static float[] ZOOM_LEVELS						= {1000.0f, 1000.0f, 1000.0f, 1000.0f};  // Meters
	//private final static float[] ZOOM_LEVELS						= {500.0f, 500.0f, 500.0f, 500.0f};  // Meters
	//private final static float[] MAP_PITCH_LEVELS					= {-40.0f,   -50.0f, -60.0f, -65.0f};
	private final static float[] MAP_PITCH_LEVELS					= {-63.0f,   -63.0f, -63.0f, -63.0f};
	private final static float BASE_PITCH_OFFSET					= -30.0f;
	//private final static float[] MAP_PITCH_LEVELS					= {0.0f,   -35.0f, -65.0f, -90.0f};
	private static float mMaxZoomLevel								= ZOOM_LEVELS[0];
	private static float[] mZoomLevels								= new float[ZOOM_LEVELS.length];
	private static float[] mYOffsets								= new float[ZOOM_LEVELS.length];
	
	private final static int  	CAMERA_RADAR_MODE_ZOOM_INDEX		= 1;	
	private final static float	CAMERA_RADAR_MODE_PITCH				= -63.0f;//MAP_PITCH_LEVELS[CAMERA_RADAR_MODE_ZOOM_INDEX];
	private final static float	CAMERA_RADAR_MODE_YOFFSET			= 40.0f; 
	private final static float	CAMERA_RADAR_MODE_CUTOFF_ANGLE		= 45.0f;

	private final static float	CAMERA_MAP_MODE_CUTOFF_ANGLE		= 45.0f;
	private final static int  	CAMERA_MAP_MODE_ZOOM_INDEX			= 2;
	private final static float 	CAMERA_YAW_INCR_BASE				= 5.0f;
	private static int			mCurrentMapZoomIndex;
	
	//protected static float 		mUserPitch							= 0.0f;
	protected static float 		mPitchOffset						= BASE_PITCH_OFFSET;
	
	private final static int	CAMERA_TRANSITION_TIME_MS			= 2000;
	private final static int	CAMERA_TRANSITION_DTIME_MS			= 200;
	
	protected static boolean 	mIsRadarMode						= true;
	private static boolean 		mRadarModeInitialized				= false;
	private static boolean 		mMapModeInitialized					= false;
	private static boolean		mShowCompassOnRadar					= true;
	private static boolean		mGotBuddies							= false;
	
	private static RadarViewRendererHelper mRadarViewRendererHelper	= null;
	private static CameraControlAnimation mCameraControlAnimation	= null;
	
	private RadarViewFrontControl mRadarViewFrontControl			= null;
	
	private static Location		mLocation 							= null;

	public RadarViewControllerHelper(Activity activity, LocationTransformer locationTransformer) {
		super(activity, new RadarViewRendererHelper((Context)activity, true, locationTransformer, ZOOM_LEVELS.length), locationTransformer);

		mRadarViewRendererHelper = (RadarViewRendererHelper)mRendererHelper;
		SetZoomIndex(CAMERA_MAP_MODE_ZOOM_INDEX);
		
		mRadarViewFrontControl = new RadarViewFrontControl(this, mRadarViewRendererHelper);
		
		for(int i = 0; i < mZoomLevels.length; i++) {
			mZoomLevels[i] = ZOOM_LEVELS[i];
		}

		for(int i = 0; i < mYOffsets.length; i++) {
			mYOffsets[i] = 0.0f;
		}
		
		//mRadarViewRendererHelper.mGLCircle.setRadius(locationTransformer.TransformMetersValue(ZOOM_LEVELS[CAMERA_RADAR_MODE_ZOOM_INDEX]));
		//Log.i(TAG, "mGLCircle.mRadius: " + mRadarViewRendererHelper.mGLCircle.getRadius());
		//mRadarViewRendererHelper.mGLCircle.setRadius(200);
		//Log.i(TAG, "mGLCircle.mRadius: " + mRadarViewRendererHelper.mGLCircle.getRadius());
		
		mCameraControlAnimation = new CameraControlAnimation(
				CAMERA_RADAR_MODE_PITCH,
				CAMERA_RADAR_MODE_YOFFSET,
				CAMERA_RADAR_MODE_CUTOFF_ANGLE, 
				mZoomLevels[CAMERA_RADAR_MODE_ZOOM_INDEX]);
	}
	
	@Override
	public void SetRendererHandler(RendererHandler rendererHandler){
		super.SetRendererHandler(rendererHandler);
		mRadarViewFrontControl.SetRendererHandler(rendererHandler);
	}
	
	public void LocationTransformerUpdated(LocationTransformer locationTransformer, boolean gpsLocation){
		//Log.v(TAG,"LocationTransformerUpdated");
		float arrawSize = locationTransformer.TransformMetersValue(RadarViewRendererHelper.USER_ARROW_SIZE_METERS);
		mMaxZoomLevel = locationTransformer.TransformMetersValue(ZOOM_LEVELS[0])+arrawSize*2;
		
		for(int i = 0; i < ZOOM_LEVELS.length; i++) {
			float transZoomLevel = locationTransformer.TransformMetersValue(ZOOM_LEVELS[i]);
			mYOffsets[i] = Util.getZoomYOffset(transZoomLevel+arrawSize*2);
			mZoomLevels[i] = transZoomLevel+arrawSize*2;
		}
		
		// FIXME : These 2 calls fit the draw area circle inside of the disk mask in the radar screen
		//		   they might need to be tweaked a bit in order to fit perfectly.
		if(locationTransformer.mBoundingBoxSet) {
			mRadarViewRendererHelper.SetGLCircleParams(mYOffsets[CAMERA_RADAR_MODE_ZOOM_INDEX]*2.3f, mZoomLevels[CAMERA_RADAR_MODE_ZOOM_INDEX]*1.17f);
		}
		//Log.i(TAG, "mGLCircle.mRadius: " + mRadarViewRendererHelper.mGLCircle.getRadius());
		
		if(!gpsLocation) {
			mRadarViewRendererHelper.SetMaxDistanceToDraw((mMaxZoomLevel+mYOffsets[0])*1.20f);
			Util.SetZoomLevels(mZoomLevels[mZoomLevels.length-1],mMaxZoomLevel);
			mRadarViewRendererHelper.SetNominalZoomLevel((mZoomLevels[mZoomLevels.length-1]+mZoomLevels[mZoomLevels.length-2])/2.0f);
		}
	}
	
	@Override
	public void UpdateBuddies(Bundle bundle, Location userLocation)
	{
		super.UpdateBuddies(bundle, userLocation);
		mRadarViewRendererHelper.UpdateBuddies(bundle, userLocation, mMaxZoomLevel, mActivity);
		
		if(!mGotBuddies) {
			mShowCompassOnRadar = false;
			mGotBuddies = true;
			EnterRadarMode();
		}
	}
	 
	public void SetMapDrawings(MapImagesInfo mapImagesInfo, MapDrawings mapDrawings) {
		mRadarViewRendererHelper.SetMapDrawings(mapImagesInfo, mapDrawings, mActivity);

		mShowCompassOnRadar = false;
		EnterRadarMode();
	}
	
	protected static void SetZoomIndex(int zoomIndex) {
		mCurrentMapZoomIndex = zoomIndex;
		mRadarViewRendererHelper.SetCurrentZoom(zoomIndex, mZoomLevels[mCurrentMapZoomIndex]);
	}
	
	protected static void SetMapMode(){
		SetFullScreenMode(true);
		
		SetZoomIndex(CAMERA_MAP_MODE_ZOOM_INDEX);
		
		mPitchOffset = BASE_PITCH_OFFSET;
		mCameraControlAnimation.SetNextCamera(
				GetCurrentPitch(),
				mYOffsets[mCurrentMapZoomIndex],
				0.0f,
				CAMERA_MAP_MODE_CUTOFF_ANGLE,
				mZoomLevels[mCurrentMapZoomIndex]);
		
		mRadarViewRendererHelper.SetRadarMode(false);		
		mRadarViewRendererHelper.SetYawLock(false);
		
		if(!mMapModeInitialized) {
			mMapModeInitialized = true;
		}
		
		mIsRadarMode = false;
	}
	
	protected static void SetRadarMode() {
		mCameraControlAnimation.SetNextCamera(
				CAMERA_RADAR_MODE_PITCH,
				CAMERA_RADAR_MODE_YOFFSET,
				0.0f,
				CAMERA_RADAR_MODE_CUTOFF_ANGLE,
				mZoomLevels[CAMERA_RADAR_MODE_ZOOM_INDEX]);
		
		mRadarViewRendererHelper.SetRadarMode(true);		
		mRadarViewRendererHelper.SetYawLock(false);
		
		if(!mRadarModeInitialized) {
			mRadarModeInitialized = true;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();

		if(!mShowCompassOnRadar) {
			if(!mRadarViewRendererHelper.MapsLoaded()) {
				if(!mRadarViewRendererHelper.HasBuddies()) {
					mGotBuddies = false;
					mShowCompassOnRadar = true;
				} else {
					mGotBuddies = true;
				}
			} else {
				mGotBuddies = mRadarViewRendererHelper.HasBuddies();
			}
		} 
		
		mRadarViewFrontControl.ResetToRadarModeControls(mRadarViewRendererHelper.MapsLoaded(), mShowCompassOnRadar);
		SetRadarMode();
		if(!mCameraControlAnimation.isAlive())
			mCameraControlAnimation.start();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	public void OnControlEvent(RadarViewFrontControl.EControlEvents controlEvent){
		switch (controlEvent) {
		case eEventRadarMapToFullMap:
		case eEventCompassToFullMap:
			SetMapMode();
			break;
		case eEventShowZoomControl:
		case eEventCompassToRadarMap:
		case eEventFullMapToCompass:
		case eEventFullMapToRadarMap:
		case eEventRadarMapLoaded:
		case eEventRadarMapToCompass:
		default:
			break;
		}
	}
	
	protected void EnterMapMode(){
		if(mRadarViewRendererHelper.MapsLoaded()) {
			mRadarViewFrontControl.AnimateFrontMask((!mShowCompassOnRadar) ? EControlEvents.eEventRadarMapToFullMap : EControlEvents.eEventCompassToFullMap);
		}
	}
	
	protected void EnterRadarMode(){
		mRadarViewFrontControl.HideZoomControlNow();
		if(mRadarViewRendererHelper.MapsLoaded()) {
			mRadarViewFrontControl.AnimateFrontMask((!mShowCompassOnRadar) ? EControlEvents.eEventFullMapToRadarMap : EControlEvents.eEventFullMapToCompass);
		} else {
			if(!mShowCompassOnRadar && !mRadarViewRendererHelper.HasBuddies()) mShowCompassOnRadar = true;
			mRadarViewFrontControl.AnimateFrontMask((!mShowCompassOnRadar) ? EControlEvents.eEventCompassToDotMap : EControlEvents.eEventDotMapToCompass);
		}
		mIsRadarMode = true;
		SetRadarMode();
	}
	
	protected void ToggleRadarCompass() {
		if(mIsRadarMode) {
			mShowCompassOnRadar = !mShowCompassOnRadar;
			if(mRadarViewRendererHelper.MapsLoaded()) {
				mRadarViewFrontControl.AnimateFrontMask((!mShowCompassOnRadar) ? EControlEvents.eEventCompassToRadarMap : EControlEvents.eEventRadarMapToCompass);
			} else if(mGotBuddies) {
				mRadarViewFrontControl.AnimateFrontMask((!mShowCompassOnRadar) ? EControlEvents.eEventCompassToDotMap : EControlEvents.eEventDotMapToCompass);
			}
		}
	}
	
	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(!mIsRadarMode) {
			switch(keyCode)
			{
			case KeyEvent.KEYCODE_DPAD_UP:
				mRadarViewFrontControl.ShowZoomControl();
				if(mCurrentMapZoomIndex < (mZoomLevels.length-1)) SetZoomIndex(mCurrentMapZoomIndex + 1);
				mCameraControlAnimation.SetZoomLevel(GetCurrentPitch(), mYOffsets[mCurrentMapZoomIndex], mZoomLevels[mCurrentMapZoomIndex]);
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				mRadarViewFrontControl.ShowZoomControl();
				if(mCurrentMapZoomIndex > 0) SetZoomIndex(mCurrentMapZoomIndex - 1);
				if(mCurrentMapZoomIndex == 0) {
					mCameraControlAnimation.SetZoomLevel(GetCurrentPitch(), mYOffsets[mCurrentMapZoomIndex],mZoomLevels[mCurrentMapZoomIndex]);
				} else {
					mCameraControlAnimation.SetZoomLevel(GetCurrentPitch(), mYOffsets[mCurrentMapZoomIndex],mZoomLevels[mCurrentMapZoomIndex]);
				}
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				mRadarViewRendererHelper.SetYawLock(true);
				mCameraControlAnimation.SetYawOffset(mCameraControlAnimation.GetYawOffset() - CAMERA_YAW_INCR_BASE, false);
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				mRadarViewRendererHelper.SetYawLock(true);
				mCameraControlAnimation.SetYawOffset(mCameraControlAnimation.GetYawOffset() + CAMERA_YAW_INCR_BASE, false);
				return true;
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				mRadarViewRendererHelper.SetYawLock(false);
				mCameraControlAnimation.SetYawOffset(0.0f, true);
				return true;
			default : return true;
			}
		} else {
			switch(keyCode)
			{
			case KeyEvent.KEYCODE_DPAD_UP:
				ToggleRadarCompass();
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				ToggleRadarCompass();
				return true;
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				EnterMapMode();
				return true;
			}
		}
			
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(!mIsRadarMode) {
			switch(keyCode)
			{
			case KeyEvent.KEYCODE_BACK: 
				EnterRadarMode();
				SetFullScreenMode(false);
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return true;
			}
		}

		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public boolean IsTopLevel(){return mIsRadarMode;}
	
	private static float GetCurrentPitch(){
		if(mIsRadarMode) {
			return CAMERA_RADAR_MODE_PITCH;
		}
		//else if(mUserPitch == 0.0f) {
			return MAP_PITCH_LEVELS[mCurrentMapZoomIndex]; 
		/*} else {
			float newPitch = Util.getCastPitch(mUserPitch, 0, -45, MAP_PITCH_LEVELS[mCurrentMapZoomIndex]-25, MAP_PITCH_LEVELS[mCurrentMapZoomIndex]+20);
			return Util.lowPassPitchFilter(newPitch, mCameraControlAnimation.GetPitch());
		}*/
	}
	
	@Override
	public void OnLocationHeadingChanged(Location location, float yaw, float pitch, boolean gpsHeading) {
		super.OnLocationHeadingChanged(location, yaw, pitch, gpsHeading);
		
		if(location != null) 
		{
			if(mLocation == null) {
				mRadarViewRendererHelper.SetUserLocation(location);
			}
			else if((mLocation.getLatitude() != location.getLatitude()) || mLocation.getLongitude() != location.getLongitude()) 
			{ 
				mRadarViewRendererHelper.SetUserLocation(location);
			}
			mLocation = location;
		}
		 
		mRadarViewRendererHelper.setHeadingDirection(Util.lowPassYawFilter(yaw, mRadarViewRendererHelper.mYaw));
		
		//if(!mIsRadarMode && !gpsHeading)
		if(!mIsRadarMode)
		{
			float newPitch = Util.getCastPitch(pitch, 20, -60.0f, 0.0f, -80.0f);
			mPitchOffset = Util.lowPassPitchFilter(newPitch, mPitchOffset);
			
			mCameraControlAnimation.SetPitch(GetCurrentPitch());
		} else {
			mPitchOffset = BASE_PITCH_OFFSET; 
			mCameraControlAnimation.SetPitch(GetCurrentPitch());
		}
	}
		
	class CameraControlAnimation extends Thread {
		private final ReentrantLock mLock = new ReentrantLock();
				
		public boolean	mIsPaused = true;
		protected boolean mStartTrans = false;
		public int		mCameraTransitionTimeMS = CAMERA_TRANSITION_TIME_MS;
		public int 		mCameraTransitionDTimeMS = CAMERA_TRANSITION_DTIME_MS;
		protected int	mCameraTransitionTimeCnt = 0;
		
		protected float	mPitch,				mTargetPitch;
		protected float	mYOffset,			mTargetYOffset;		
		protected float	mCutoffAngle, 		mTargetCutoffAngle;
		protected float	mZoomLevel, 		mTargetZoomLevel;
		public float	mYawOffset, 		mTargetYawOffset;
		
		public CameraControlAnimation(float pitch, float yOffset, float cutoffAngle, float zoomLevel) {
			SetCamera(pitch, yOffset, 0.0f, cutoffAngle, zoomLevel);
		}
		
		public void Pause() { mIsPaused = true; }
		public void Resume() { mIsPaused = false; }
		
		protected void SetCamera(float pitch, float yOffset, float yawOffset, float cutoffAngle, float zoomLevel) {
			mPitch			= pitch;
			mYOffset		= yOffset;
			mCutoffAngle	= cutoffAngle;
			mZoomLevel		= zoomLevel;
			mYawOffset		= yawOffset;
			
			if(mRadarViewRendererHelper != null) {
				mRadarViewRendererHelper.SetCamera(pitch, mPitchOffset, yOffset, mYawOffset, cutoffAngle, zoomLevel);
			} 
			if(mRendererHandler != null) {
				mRendererHandler.RedrawScene();
			}
		}
		
		public void SetPitch(float pitch) {
			if(mStartTrans)
				mTargetPitch = pitch;
			else {
				mPitch = pitch;
				SetCamera(pitch, mYOffset, mYawOffset, mCutoffAngle, mZoomLevel);
			}
		}
		
		public float GetPitch() {
			if(mStartTrans)
				return mTargetPitch;
			else {
				return mPitch;
			}
		}
		
		public boolean OverrideYaw() {
			return (GetYawOffset() > 0.0f);
		}
		
		public float GetYawOffset(){
			if(mStartTrans) {
				return GetTransVal(mYawOffset, mTargetYawOffset);
			}
			else {
				return mYawOffset;
			}
		}
		
		public void SetYawOffset(float yawOffset, boolean animate) {
			while(yawOffset > 180.0f) yawOffset -= 360.0f;
			while(yawOffset < -180.0f) yawOffset += 360.0f;
			
			if(!mStartTrans && (mYawOffset == yawOffset))
				return;
			
			if(!animate) {
				mYawOffset = yawOffset;
				mTargetYawOffset = yawOffset;
			}
			SetNextCamera(mTargetPitch, mTargetYOffset, yawOffset, mTargetCutoffAngle, mTargetZoomLevel);
		}
		
		public void SetZoomLevel(float pitch, float yOffset, float zoomLevel) {
			//Log.v(TAG,"SetZoomLevel(pitch="+pitch+", yOffset="+yOffset+", zoomLevel="+zoomLevel+")");
			SetNextCamera(pitch, yOffset, mTargetYawOffset, mTargetCutoffAngle, zoomLevel);
		}
		
		public void SetNextCamera(float pitch, float yOffset, float yawOffset, float cutoffAngle, float zoomLevel) {
			mLock.lock();
			
			if(mStartTrans && mCameraTransitionTimeCnt > 0) {
    			SetCamera(
    					GetTransVal(mPitch,				mTargetPitch),
    					GetTransVal(mYOffset,			mTargetYOffset),
    					GetTransVal(mYawOffset,			mTargetYawOffset),
    					GetTransVal(mCutoffAngle,		mTargetCutoffAngle),
    					GetTransVal(mZoomLevel,			mTargetZoomLevel));
			}
			
			mTargetPitch				= pitch;
			mTargetYOffset				= yOffset;
			mTargetYawOffset			= yawOffset;
			mTargetCutoffAngle			= cutoffAngle;
			mTargetZoomLevel			= zoomLevel;
			mCameraTransitionTimeCnt	= 0;
			mStartTrans 				= true;
			
			mLock.unlock();
		}
		
		protected float GetTransVal(float startVal, float endVal) {
			return (float) (( (endVal-startVal)/2.0f ) * ( (Math.sin( (double)(((double)mCameraTransitionTimeCnt/(double)mCameraTransitionTimeMS)*Math.PI-(Math.PI/2))))  + 1 ) + startVal);
		}
		
	    @Override
	    public void run(){
	    	mIsPaused = false;
	    	while(true) {
	    		mLock.lock();
	    		if(!mIsPaused) {
	    			if(mStartTrans) {
		    			if(mCameraTransitionTimeCnt >= mCameraTransitionTimeMS) {
		    				SetCamera(mTargetPitch, mTargetYOffset, mTargetYawOffset, mTargetCutoffAngle, mTargetZoomLevel);
		    				mStartTrans = false;
		    			}
		    			else {
			    			mRadarViewRendererHelper.SetCamera(
			    					GetTransVal(mPitch,				mTargetPitch),
			    					mPitchOffset,
			    					GetTransVal(mYOffset,			mTargetYOffset),
			    					GetTransVal(mYawOffset,			mTargetYawOffset),
			    					GetTransVal(mCutoffAngle,		mTargetCutoffAngle),
			    					GetTransVal(mZoomLevel,			mTargetZoomLevel));
			    			mRendererHandler.RedrawScene();
		    			}
		    			mCameraTransitionTimeCnt += mCameraTransitionDTimeMS;
	    			}
	    		}
	    		
	    		mLock.unlock();
	    		
	    		try {
					Thread.sleep(mCameraTransitionDTimeMS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	    	}
	    }
	}
}
