package com.reconinstruments.mapsdk.mapview.camera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoTile;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.MapView;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;


public class CameraViewport extends RectF {
	private static final String TAG = "CameraViewport";
	
	// the CameraViewport represents a RectF in screen coordinates centered around a geo-location (long, lat)
	// it is calculated from a region width (in meters), the aspect ratio and the GPS long, lat
	// it has a current and target location, rotation and scale and is regularly updated by it's parent MapView through UpdateViewport().  
	// Each call of the UpdateViewport() moves the viewport center closer to the target location and similarly moves the viewport rotation and scaling towards the target rotation and scaling

	// The camera will not have a boundary set until it has it's center point initialized
	
	private static final float DEFAULT_SCALE = 2.0f;
	private static final boolean BLOCK_ZOOM_ROLLOVER = false;
	private static final float MAX_ALTITUDE_SCALE = 20.f;
	private static final float CAMERA_ALTITUDE_TO_VIEWPORT_WIDTH_RATIO = 4.0f;	// an effective camera view angle of 28 deg  - this corresponds to a height scale of 1.0
																				// for base viewport width of 1284m (at 3m per pixel), give base height of 2,568m
	private final static String PROFILE_FOLDER = "ReconApps/MapData";
	private final static float TAPER_PITCH_FACTOR = 2f;

    private static final double INVALID_LATITUDE = -200;
    private static final double INVALID_LONGITUDE = -200;
	
	
	public volatile boolean mLoadNewTargetScale = false;
	
	public volatile float mLoadedAltitudeScale = 1f;
	
	private static boolean FORCE_TAPER_PITCH_WHILE_MOVING = true;
	
	// currently camera Scale represents a zoom magnification scale and not a viewport width/camera height scale modifier
	// at some point this can be converted to match mapSdk API which controls camera in terms of height scale modifier
	// for now the code just converts to the old zoom mag scale
	
	// conversion ratios
	float					mMetersPerPixel = World2DrawingTransformer.DISTANCE_PER_PIXEL;

	// GPS coords
	public double 			mCenterLongitude;			
	public double			mCenterLatitude;
	// in meters
	double 					mLastRequestedRegionWidthInMeters = 0.0;		
	double					mBaseRegionWidthInMeters;
	double					mBaseCameraAltitudeInMeters;
	double					mRegionWidthInMeters = 0.0;
	double					mRegionHeightInMeters = 0.0;
	double					mTargetRegionWidthInMeters = 0.0;
	double					mTargetRegionHeightInMeters = 0.0;
	double					mLowestResTargetRegionWidthInMeters = 0.0;
	double					mLowestResTargetRegionHeightInMeters = 0.0;
	double 					mPanStepSizeInMeters;			//
	float					mPanStepScale = 1.0f;
	float					mPixelWidthInDeg = 0.f;
	float					mPixelHeightInDeg = 0.f;

	public boolean			mCameraRotateWithUser = false;		// logic moved to mapview
	public boolean			mCameraPitchesWithUser = false;

	int 					mMaxZoomLevelIndex = 3;
	int 					mMinZoomLevelIndex = 0;
	// in screen pixels
	public int				mScreenWidthInPixels;
	public int				mScreenHeightInPixels;
	public int				mStatusBarHeight = 30;
	
	// unitless ratios
	double					mAspectRatio = 428.0/240.0;  // TODO calc this in future, not hardcoded
	
	double 					mMaxZoomScale = 0.25;
	double					mMinZoomScale = 99999.0;
	double					mMinAltitudeScale = 0;
	double					mMaxAltitudeScale = 0;
	double[] 				mPresetZoomScale;				// array of preset zoom scales
	/**
	 * This is the next altitude scale value for {@link #mCurrentAltitudeScale}
	 * to move toward. It represents an incremental value and is used to calculate
	 * a smooth transition from {@link #mCurrentAltitudeScale} to {@link #mTargetAltitudeScale}
	 */
	public volatile float 	mTargetAltitudeScale;
	public float 			mTargetRotationAngle = 0.0f;	// clockwise from north = 0;
	public double 			mTargetLongitude = 0.0;
	public double 			mTargetLatitude = 0.0;
	public double			mTargetVelocity = 0.0;
	public volatile float	mCurrentAltitudeScale;
	/**
	 * This is similar to {@link #mTargetAltitudeScale}, but represents the next 
	 * of the n zoom levels defined in XML. <br><br> 
	 * e.g. If there are 4 zoom levels defined in rendering_schemes.xml, and {@link #mCurrentAltitudeScale}
	 * is currently at zoom_level=1, and we want to transition to zoom_level=2, then 
	 * {@link #mNextAltitudeScale} will be set to the scale value represented by 
	 * zoom_level=2
	 */
	public volatile float 	mNextAltitudeScale;
	public float 			mCurrentRotationAngle = 0.0f;	// clockwise from north = 0;
	public double 			mCurrentLongitude = INVALID_LONGITUDE;		// in gps coordinates
	public double 			mCurrentLatitude = INVALID_LATITUDE;
	public double			mCurrentVelocity = 0.0;
	public volatile double	mCurrentPitch = 0.0;
	public volatile double 	mTargetPitch = 0.0;
	public volatile double 	mRealCurrentPitch = 0.0;
	public volatile double	mPitchOffset = 0.0f;
	protected ArrayList<PointXY> mPitchVelocityCurve;
	protected ArrayList<PointXY> mPitchAltitudeCurve;
	protected ArrayList<PointXY> mAltitudeVelocityCurve;
	protected volatile boolean 	mCameraRespondsToUserVelocity = false;
	boolean					mVelocityCurvesNotLoadedCorrectly =false;
	int 					mVelocityBaseZoomLevelIndex = 0;
	float					mIdleVelocityThreshold = 3.f;	// 3m/s;
	
	volatile float 			mAltitudeScaleChangeRate = 0.2f;		// default rate values... running values supplied on init from parent (which reads it from a RenderingScheme xml file)
	
	private float 			mAltitudeToPitchRatio = 1f;
	
	float 					mPitchChangeRate = 0.4f;
	float 					mChangeRotationRate = 3.0f;
	double 					mChangeOffsetRateDegrees = 0.0002;
	double 					mChangeVelocityRate = 1.0;
	private int				mCurrentTileIndex = 0;		// for debug logging
	
	public RectF			mUserTestBB = new RectF();	
	// 
	RectF					mCurrentBaseGPSBB = new RectF();	// boundary boxes in GPS coordinates for min zoom level - noting that RectF is graphics object so Y positive down
	RectF					mTargetBaseGPSBB = new RectF();		// 
	RectF					mCurrentGPSBB = new RectF();	// boundary boxes in GPS coordinates - noting that RectF is graphics object so Y positive down
	RectF					mTargetGPSBB = new RectF();		// 
	
//	private	GeoRegion		mInitialedTargetRegion = null;
//	private float			mInitializedTargetMarginWidth = 0.0f;
	boolean					mViewportDimensionsSet = false;
	public GeoRegion		mGeoRegionToSupportCurrentView = new GeoRegion();
	public GeoRegion		mGeoRegionToSupportTargetView = new GeoRegion();
	public GeoRegion		mGeoRegionToSupportCurrentViewLowestResolution = new GeoRegion();
	public GeoRegion		mGeoRegionToSupportTargetViewLowestResolution = new GeoRegion();
	
	public volatile boolean mScaleChangeDone = false;

	public enum ContainResult {
		NO,
		YES_INBOUNDARY_DATALOADING,
		YES_DATALOADING,
		YES_INBOUNDARY_DATALOADED,
		YES_DATALOADED
	}
	
	//OpenGL relevant members
	/**
	 * View matrix for this camera. Used for transformations
	 * in OpenGL ES 2.0
	 */
	public float[] 			mCamViewMatrix;
	
	/**
	 * Projection matrix for this camera. Used for transformations
	 * in OpenGL ES 2.0
	 */
	public float[]			mCamProjMatrix;
	
	/**
	 * The Z-value of the near plane of the perspective projection frustum.
	 * Used for rendering GLText.
	 */
	public float			mProjectionZNear;

	public CameraViewport(Activity mParentActivity, double regionWidth, RenderSchemeManager rsm) {  

		double[] zoomLevels = rsm.GetZoomLevels();
		
		mLastRequestedRegionWidthInMeters = regionWidth;
		mMinZoomLevelIndex = 0;
		mMaxZoomLevelIndex = zoomLevels.length - 1;
		mVelocityBaseZoomLevelIndex = 0;
		mPresetZoomScale= new double[mMaxZoomLevelIndex + 1];

		mMaxZoomScale = 0.0;
		for(int i = 0; i<= mMaxZoomLevelIndex; i++) {
			mPresetZoomScale[i] = zoomLevels[i];
			if(mPresetZoomScale[i] > mMaxZoomScale) mMaxZoomScale = mPresetZoomScale[i];
			if(mPresetZoomScale[i] < mMinZoomScale) mMinZoomScale = mPresetZoomScale[i];
		}
		mTargetAltitudeScale= (float)mMaxZoomScale;
		mCurrentAltitudeScale= (float)mMaxZoomScale;
		mLoadedAltitudeScale = mCurrentAltitudeScale;
		mAltitudeScaleChangeRate = (float)rsm.GetRate(RenderSchemeManager.Rates.SCALE_RATE);
		mPitchChangeRate = 1.f;
		mChangeRotationRate = (float)rsm.GetRate(RenderSchemeManager.Rates.ROTATION_RATE);
		mChangeVelocityRate = rsm.GetRate(RenderSchemeManager.Rates.VELOCITY_RATE);
		mChangeOffsetRateDegrees = rsm.GetRate(RenderSchemeManager.Rates.PAN_RATE);
		
		mCamViewMatrix = new float[16];
		mCamProjMatrix = new float[16];
		
		mIdleVelocityThreshold = rsm.GetIdleVelocityThreshold();
		
		LoadVelocityCurves(mParentActivity.getResources());
		if(mVelocityCurvesNotLoadedCorrectly) {
			Log.e(TAG, "Unable to load camera velocity curves for dynamic camera movement.");
		}
//		else {
//			for(PointXY node : mPitchVelocityCurve) {
//				Log.d(TAG, "Pitch curve node: " + node.x + ", " + node.y);
//			}
//			for(PointXY node : mAltitudeVelocityCurve) {
//				Log.d(TAG, "Altitude curve node: " + node.x + ", " + node.y);
//			}
//		}
	}

	public boolean IsLowestZoom() {
//		Log.e(TAG, "In IsLowestZoom " + mTargetAltitudeScale + ", " + mMaxZoomScale+ ", " + (mCurrentAltitudeScale >= mMaxZoomScale * 0.99));
		return mTargetAltitudeScale >= mMaxZoomScale * 0.99;
	}
	
	public void Reset() {
		mTargetAltitudeScale = (float) mPresetZoomScale[0];
		mTargetRotationAngle = 0.0f;	
		mTargetLatitude = 0.0f;
		mTargetLongitude = 0.0f;
		mCurrentAltitudeScale = 0.0f;
		mCurrentRotationAngle = 0.0f;	
		mCurrentLongitude = INVALID_LONGITUDE;
		mCurrentLatitude = INVALID_LATITUDE;
	}

	public void SetStatusBarHeight(int height) {
		mStatusBarHeight = height;
	}

	public void SetViewPortDimensions(float width, float height) {

		mScreenWidthInPixels = (int)width;
		mScreenHeightInPixels = (int)height;

		mAspectRatio = (double)width/(double)height;		// TODO fix initial vs final aspect ration setting, hardcoded for now
		Log.d(TAG, "VP Dimensions: "+ width + "x"+ height + " - "+ mAspectRatio);

		mBaseRegionWidthInMeters = width * mMetersPerPixel; // = 1284 m at 3m/pixel
		mBaseCameraAltitudeInMeters = mBaseRegionWidthInMeters/2.0 * CAMERA_ALTITUDE_TO_VIEWPORT_WIDTH_RATIO;
		
		left = -(width/2f) - 1;
		right  = (width/2f) + 1;
		top = -(height/2f) - 1;
		bottom = (height/2f) + 1;
		
		mUserTestBB.left = left;
		mUserTestBB.right = right;
		mUserTestBB.top = top;
		mUserTestBB.bottom = bottom;
		
		mCurrentAltitudeScale = mTargetAltitudeScale;
		
		mViewportDimensionsSet = true;

		CalculateAllParameters();
	}

	public boolean IsReady() {
		return mViewportDimensionsSet && ViewportCenterIsDefined();	// only criteria needed for camera to be ready
	}
	
	public void SetCameraToRotateWithUser(boolean cameraRotatesWithUser) {
		mCameraRotateWithUser = cameraRotatesWithUser;
	}
	
	public void SetCameraToPitchWithUser(boolean cameraPitchesWithUser) {
		mCameraPitchesWithUser = cameraPitchesWithUser;
	}

//---------------------- set / modify region - sets camera GPS position and altitude scale
	public void SetCameraToShowGeoRegion(GeoRegion geoRegion, float percentWidthMargin, boolean immediate, boolean includeRotationBorder) {	

		if(mViewportDimensionsSet) {
//			mInitialedTargetRegion = geoRegion;	// viewport is not defined yet, so cache request until it is ready
//			mInitializedTargetMarginWidth = percentWidthMargin;
//		}
//		else {
//			mInitialedTargetRegion = null; 
			if(percentWidthMargin > 1.0f || percentWidthMargin < 0f){
				Log.wtf(TAG, "PercentWidthMargin is not a proper percent value! Value must be with 0f ~ 1.0f ");
			}
		    geoRegion.MapToWidthHeightRatio(mAspectRatio);
            float regionWidthInMeters = geoRegion.widthInMeters();
		    
            if(!includeRotationBorder) {			// scale requested width down as camera always adds a rotation border
				double ratio = mScreenWidthInPixels / Math.sqrt(mScreenWidthInPixels * mScreenWidthInPixels + mScreenHeightInPixels *mScreenHeightInPixels) * 0.9995;
				Log.d(TAG, "region scale ratio: " + ratio);
				regionWidthInMeters = (float) (regionWidthInMeters * ratio);
			}
			SetTargetScale((float)(regionWidthInMeters*(1.0f + 2.f*percentWidthMargin)/mBaseRegionWidthInMeters));
//			Log.e(TAG,"Scale from region = " + mTargetAltitudeScale);
			if(immediate) {
				mCurrentAltitudeScale = mTargetAltitudeScale;
			}
			SetGPSPosition(geoRegion.mCenterPoint.x, geoRegion.mCenterPoint.y, immediate);
//			mTargetLongitude = geoRegion.mCenterPoint.x;F
//			mTargetLatitude = geoRegion.mCenterPoint.y;
		}
	}
	
	public GeoRegion GetBoundingGeoRegion() {
		return mGeoRegionToSupportCurrentView;
	}
	
	public GeoRegion GetBoundingLowestResolutionGeoRegion() {
		return mGeoRegionToSupportCurrentViewLowestResolution;
	}
	
//	public ContainResult Contains(CameraViewport geoRegion, float boundaryRatio) {
//		return ContainResult.NO;
//	}
//	

	
//---------------------- set or modify camera GPS position (ie, viewport center)
	public boolean ViewportCenterIsDefined() {
		// if center is set, either from SetGPSPosition() or through SetCameraToShowGeoRegion() 
		return (mCurrentLongitude != INVALID_LONGITUDE && mCurrentLatitude != INVALID_LATITUDE);
	}
	
	public void SetGPSPosition(double newLongitude, double newLatitude, boolean immediate) { 
		mTargetLatitude = (float)newLatitude;
		mTargetLongitude = (float)newLongitude;
		if(immediate || (mCurrentLongitude != INVALID_LONGITUDE && mCurrentLatitude != INVALID_LATITUDE)) {
			mCurrentLatitude = mTargetLatitude;
			mCurrentLongitude = mTargetLongitude;
		}
		CalculateAllParameters();
	}		

	public void SetCameraToRespondToUserVelocity(boolean cameraRespondsToUserVelocity) {
		mCameraRespondsToUserVelocity = cameraRespondsToUserVelocity && !mVelocityCurvesNotLoadedCorrectly;
		if(mCameraRespondsToUserVelocity) Log.d(TAG,"Responding to user velocity!");
	}
	
	public void SetVelocity(double newVelocity, boolean immediate) {
//		if(mCameraRespondsToUserVelocity) Log.e(TAG, "==== SetVelocity: " + newVelocity);
		if(newVelocity >= 0) mTargetVelocity = newVelocity;
		if(immediate || (mTargetVelocity < 0.0)) {
			mCurrentVelocity = mTargetVelocity;
			if(mCameraRespondsToUserVelocity) {		// scale and pitch are set by velocity curves
				mCurrentPitch = CalcPitchFromVelocity(mCurrentVelocity);
				mCurrentAltitudeScale = CalcScaleFromVelocity(mCurrentVelocity);
				mTargetAltitudeScale = mTargetAltitudeScale;	// keep these synched
			}
		}
		CalculateAllParameters();
	}

	public void SetPanStepScale(float stepScale) {
		if(stepScale > 0.1f && stepScale < 5.f) {
			mPanStepScale = stepScale;	
		}
	}
	
	public void panLeft() {
		if(ViewportCenterIsDefined()) {
			double oldLat = mTargetLatitude;
			double oldLong = mTargetLongitude;
			double panAmount = mPanStepSizeInMeters;
			double angleLeftDir = mCurrentRotationAngle+270.0;;
			mTargetLongitude = LongitudeAtLocationPlusHorizontalOffset(oldLat, oldLong, +panAmount * Math.sin(Math.toRadians(angleLeftDir) ));
			mTargetLatitude  = LatitudeAtLocationPlusVerticalOffset(oldLat, oldLong, +panAmount * Math.cos(Math.toRadians(angleLeftDir) ));
			CalculateAllParameters();
		}
	}

	public void panRight() {
		if(ViewportCenterIsDefined()) {
			double oldLat = mTargetLatitude;
			double oldLong = mTargetLongitude;
			double panAmount = mPanStepSizeInMeters;
			double angleRightDir = mCurrentRotationAngle+90.0;
			mTargetLongitude = LongitudeAtLocationPlusHorizontalOffset(oldLat, oldLong, +panAmount * Math.sin(Math.toRadians(angleRightDir) ));
			mTargetLatitude  = LatitudeAtLocationPlusVerticalOffset(oldLat, oldLong, +panAmount * Math.cos(Math.toRadians(angleRightDir) ));
			CalculateAllParameters();
		}
	}

	public void panUp() { 
		if(ViewportCenterIsDefined()) {
			double oldLat = mTargetLatitude;
			double oldLong = mTargetLongitude;
			double panAmount = mPanStepSizeInMeters;
			double angleUpDir = mCurrentRotationAngle;
			mTargetLongitude = LongitudeAtLocationPlusHorizontalOffset(oldLat, oldLong, +panAmount * Math.sin(Math.toRadians(angleUpDir) ));
			mTargetLatitude  = LatitudeAtLocationPlusVerticalOffset(oldLat, oldLong, +panAmount * Math.cos(Math.toRadians(angleUpDir) ));
			CalculateAllParameters();
		}
	}

	public void panDown() {	
		if(ViewportCenterIsDefined()) {
			double oldLat = mTargetLatitude;
			double oldLong = mTargetLongitude;
			double panAmount = mPanStepSizeInMeters;
			double angleDownDir = mCurrentRotationAngle+180.0;
			mTargetLongitude = LongitudeAtLocationPlusHorizontalOffset(oldLat, oldLong, +panAmount * Math.sin(Math.toRadians(angleDownDir) ));
			mTargetLatitude  = LatitudeAtLocationPlusVerticalOffset(oldLat, oldLong, +panAmount * Math.cos(Math.toRadians(angleDownDir) ));
			CalculateAllParameters();
		}
	}
	
	public void FreezePan() {
		if(ViewportCenterIsDefined()) {
			mTargetLatitude = mCurrentLatitude;
			mTargetLongitude = mCurrentLongitude;
			CalculateAllParameters();
		}
	}

	
//---------------------- set or modify scale
	public void SetCameraAltitude(float altitudeInM, boolean immediate) {	
		if(mViewportDimensionsSet && !mCameraRespondsToUserVelocity) {
			SetTargetScale((float) (altitudeInM/mBaseCameraAltitudeInMeters));
			if(immediate || mCurrentAltitudeScale == 0.0f) {
				mCurrentAltitudeScale = mTargetAltitudeScale;
			}
			CalculateAllParameters();
//			Log.d(TAG, "setting target scale: " + mTargetAltitudeScale + ", current scale: "+mCurrentAltitudeScale);
		}
	}
	
	public void SetCameraAltitudeScale(float altitudeScale, boolean immediate) {			
		if(!mCameraRespondsToUserVelocity) {
			SetTargetScale(altitudeScale);

			if(immediate || mCurrentAltitudeScale == 0.0f) {
				mCurrentAltitudeScale = mTargetAltitudeScale;
			}
			CalculateAllParameters();
//			Log.d(TAG, "setting target scale: " + mTargetAltitudeScale + ", current scale: "+mCurrentAltitudeScale);
		}
	}
	
	private void SetTargetScale(float newScale) {
		if(newScale > MAX_ALTITUDE_SCALE) {
			mTargetAltitudeScale = MAX_ALTITUDE_SCALE;
		}
		else {
			mTargetAltitudeScale = newScale;
		}
//		Log.e(TAG, "new target scale: " + newScale );
	}
	
	public float GetAltitudeScale() {			
		return mCurrentAltitudeScale;
	}
	
//	public void UpdateTarget(float scale) {
//		mTargetAltitudeScale = scale;
//		CalculateAllParameters();
//	}
//
	public void SetZoomLevels(double[] zoomLevels) {
		mPresetZoomScale = zoomLevels;	// TODO not fully tested
		
		for(int i=0; i<zoomLevels.length; i++) {  // reset max level
			if(i==0) {
				mMaxZoomScale = zoomLevels[i];
			}
			else {
				if(zoomLevels[i] > mMaxZoomScale) mMaxZoomScale = zoomLevels[i]; 
			}
		}
	}
	
	public void SetZoomIndex(int zoomIndex) {
		mCurrentAltitudeScale = mTargetAltitudeScale = (float) mPresetZoomScale[zoomIndex];
		CalculateAllParameters();
	}
	
	public double ZoomOut() {
		if(!mCameraRespondsToUserVelocity || mCurrentVelocity < mIdleVelocityThreshold) {	// block zoom if dynamic altitude scaling and user is moving
			double newScale = mPresetZoomScale[0];
			for(int zl = 0; zl <= mMaxZoomLevelIndex; zl++) {
				if(mTargetAltitudeScale <= 1.1* mPresetZoomScale[zl]  ){
					if(zl > 0) {
						newScale = mPresetZoomScale[zl-1];
						mVelocityBaseZoomLevelIndex = zl-1;
					}
					else {
						if(!BLOCK_ZOOM_ROLLOVER) {
							newScale = mPresetZoomScale[mMaxZoomLevelIndex];
						}
					}
				}
			}	
			mTargetAltitudeScale = (float)newScale;
			CalcDistanceParameters() ;
		}
		return mTargetAltitudeScale;
	}
	
	public double ZoomIn() {
		if(!mCameraRespondsToUserVelocity || mCurrentVelocity < mIdleVelocityThreshold) {	// block zoom if dynamic altitude scaling and user is moving
			double newScale = mPresetZoomScale[mMaxZoomLevelIndex];
			for(int zl = mMaxZoomLevelIndex; zl >= 0; zl--) {
				//		for(int zl = 0; zl <= mMaxZoomLevelIndex; zl++) {
//							Log.d(TAG, "Zoom level " + zl + ", presetZoomScale: " + mPresetZoomScale[zl] + ", targetAlt: " + mTargetAltitudeScale);
				if(mTargetAltitudeScale >= mPresetZoomScale[zl]  ){
					if(zl < mMaxZoomLevelIndex) {
						newScale = mPresetZoomScale[zl+1];
						mVelocityBaseZoomLevelIndex = zl-1;
						//					Log.d(TAG, ""+newScale);
					}
					else {
						if(!BLOCK_ZOOM_ROLLOVER) {
							newScale = mPresetZoomScale[0];
						}
					}
				}
			}	
//					Log.d(TAG, "Zoom in old: " + mTargetAltitudeScale + ", new: " + newScale + " - numZoom levels: " + mMaxZoomLevelIndex);
			mTargetAltitudeScale = (float)newScale;
			CalcDistanceParameters() ;
		}
		return mTargetAltitudeScale;
	}
	
//---------------------- set / modify rotation
	public void SetViewAngleRelToNorth(float angle, boolean immediate) {
		mTargetRotationAngle = angle;
		if(immediate || mCurrentRotationAngle == 0.0f) {
			mCurrentRotationAngle = mTargetRotationAngle;
		}
	}
	
	public void SetRealPitchAngle(float pitchAngle) {
		mRealCurrentPitch = pitchAngle;
	}

	
// ------------------ update internal model 
	private void CalculateAllParameters() {
		if(mViewportDimensionsSet) {
			CalcDistanceParameters();
			if(ViewportCenterIsDefined()) {
				CalcBoundingBox();
				CalcTargetBoundingBox();
			}
		}
	}

	private void CalcDistanceParameters() {
		double equitorialCircumference = 40075017.0; // m  - taken from wikipedia/Earth
		double meridionalCircumference = 40007860.0; // m  - taken from wikipedia/Earth

		mRegionWidthInMeters = mBaseRegionWidthInMeters * mCurrentAltitudeScale; 
		mRegionHeightInMeters =  mRegionWidthInMeters / mAspectRatio;
		mPanStepSizeInMeters = mRegionWidthInMeters / 2.0 * mPanStepScale ; // step portion of screen width with each pan adjustment, but will be stopped when user releases button
//		Log.e(TAG, "pix calc: " + (equitorialCircumference*Math.cos(Math.toRadians(mCurrentLatitude))) + ", " + mCurrentLatitude+ ", " + mCurrentAltitudeScale);
		mPixelWidthInDeg =  (float)( (double)mMetersPerPixel / (equitorialCircumference*Math.cos(Math.toRadians(mCurrentLatitude))) * 360.0 * mCurrentAltitudeScale );
		mPixelHeightInDeg = (float)( (double)mMetersPerPixel / meridionalCircumference * 360.0 * mCurrentAltitudeScale );
				
		mTargetRegionWidthInMeters = mBaseRegionWidthInMeters * mTargetAltitudeScale; 
		mTargetRegionHeightInMeters =  mTargetRegionWidthInMeters / mAspectRatio;
		
		mLowestResTargetRegionWidthInMeters = mBaseRegionWidthInMeters * mMaxZoomScale; // should never change unless mMaxZoomScale or base width change
		mLowestResTargetRegionHeightInMeters =  mTargetRegionWidthInMeters * mMaxZoomScale;
	}

	private void CalcBoundingBox() {	// calc BB as square region with side length 2M, where M = dist from center to region corner.  This simplifies rotation handling as any rotation of the region is contained in this square
		if(mRegionWidthInMeters != 0.0) {
			double BBDistInMeters = (Math.sqrt(mRegionWidthInMeters * mRegionWidthInMeters + mRegionHeightInMeters * mRegionHeightInMeters))/2.0;
			mCurrentGPSBB.left   = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, -BBDistInMeters);
			mCurrentGPSBB.right  = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, +BBDistInMeters);
			mCurrentGPSBB.top    = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, -BBDistInMeters);	// top/bottom reversed as RectF methods are defined for graphics, y +ve down
			mCurrentGPSBB.bottom = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, +BBDistInMeters);
//			Log.e(TAG, "Scale=" + mCurrentAltitudeScale + ", maxZoom= " + mMaxZoomScale);
			double LowestRestBBDistInMeters;
			if(mCurrentAltitudeScale < mMaxZoomScale) {
				LowestRestBBDistInMeters = BBDistInMeters / mCurrentAltitudeScale * mMaxZoomScale;
			}
			else {
				LowestRestBBDistInMeters = BBDistInMeters;
			}
			mCurrentBaseGPSBB.left   = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, -LowestRestBBDistInMeters);
			mCurrentBaseGPSBB.right  = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, +LowestRestBBDistInMeters);
			mCurrentBaseGPSBB.top    = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, -LowestRestBBDistInMeters);	
			mCurrentBaseGPSBB.bottom = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, +LowestRestBBDistInMeters);

			mGeoRegionToSupportCurrentView.MakeUsingBoundingBox(mCurrentGPSBB.left, mCurrentGPSBB.bottom, mCurrentGPSBB.right, mCurrentGPSBB.top);
			mGeoRegionToSupportCurrentViewLowestResolution.MakeUsingBoundingBox(mCurrentBaseGPSBB.left, mCurrentBaseGPSBB.bottom, mCurrentBaseGPSBB.right, mCurrentBaseGPSBB.top);

		}
	}
	
	public void CalcTargetBoundingBox() {	// calc BB as square region with side length 2M, where M = dist from center to region corner.  This simplifies rotation handling as any rotation of the region is contained in this square
		if(mTargetRegionWidthInMeters != 0.0) {
			double BBDistInMeters = (Math.sqrt(mTargetRegionWidthInMeters * mTargetRegionWidthInMeters + mTargetRegionHeightInMeters * mTargetRegionHeightInMeters))/2.0;
			mTargetGPSBB.left   = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, -BBDistInMeters);
			mTargetGPSBB.right  = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, +BBDistInMeters);
			mTargetGPSBB.top    = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, -BBDistInMeters);
			mTargetGPSBB.bottom = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, +BBDistInMeters);
			double LowestRestBBDistInMeters;
			if(mCurrentAltitudeScale < mMaxZoomScale) {
				LowestRestBBDistInMeters = (Math.sqrt(mLowestResTargetRegionWidthInMeters * mLowestResTargetRegionWidthInMeters + mLowestResTargetRegionWidthInMeters * mLowestResTargetRegionWidthInMeters))/2.0;
			}
			else {
				LowestRestBBDistInMeters = BBDistInMeters;
			}
			mTargetBaseGPSBB.left   = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, -LowestRestBBDistInMeters);
			mTargetBaseGPSBB.right  = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, +LowestRestBBDistInMeters);
			mTargetBaseGPSBB.top    = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, -LowestRestBBDistInMeters);
			mTargetBaseGPSBB.bottom = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, +LowestRestBBDistInMeters);

			mGeoRegionToSupportTargetView.MakeUsingBoundingBox(mTargetGPSBB.left, mTargetGPSBB.bottom, mTargetGPSBB.right, mTargetGPSBB.top);	// note mapping from RectF (graphics, Y +ve down) to RectXY (GPS, Y +ve up)
			mGeoRegionToSupportTargetViewLowestResolution.MakeUsingBoundingBox(mTargetBaseGPSBB.left, mTargetBaseGPSBB.bottom, mTargetBaseGPSBB.right, mTargetBaseGPSBB.top);	// note mapping from RectF (graphics, Y +ve down) to RectXY (GPS, Y +ve up)
		}
	}

	protected float LongitudeAtLocationPlusHorizontalOffset(double refLatitude, double refLongitude, double distInMeters) {
		double equitorialCircumference = 40075017.0; // m  - taken from wikipedia/Earth

		return (float)(refLongitude + distInMeters/(equitorialCircumference*Math.cos(Math.toRadians(refLatitude))) * 360.0);	
	}

	protected float LatitudeAtLocationPlusVerticalOffset(double refLatitude, double refLongitude, double distInMeters) {
		double meridionalCircumference = 40007860.0; // m  - taken from wikipedia/Earth

		return (float)(refLatitude + distInMeters/meridionalCircumference * 360.0);
	}

	public void UpdateViewport() {
		if(ViewportCenterIsDefined()) {
//			Log.d(TAG,"Viewport Center Is Defined!");
			//		Log.d(TAG,"updating viewport: "+ (mCurrentRotationAngle - mTargetRotationAngle));
			if(mCameraRespondsToUserVelocity) {		// scale and pitch are set by velocity curves
//				Log.d(TAG,"In UpdateViewport - mCameraRespondsToUserVelocity = true"); 
				
				updateVelocity();
			}
			
			//IF camera is set to manual zoom mode
			else {
				mTargetPitch = CalcPitchFromAltitude(mTargetAltitudeScale);
				mLoadNewTargetScale = true;
				updateVelocity();
//				mLoadedAltitudeScale = (Math.abs(mCurrentAltitudeScale - mTargetAltitudeScale) <= mAltitudeScaleChangeRate*1.1f) 
//						? mCurrentAltitudeScale : mLoadedAltitudeScale;
				if(FORCE_TAPER_PITCH_WHILE_MOVING){
					float velocity = (mCurrentVelocity > 5.0) ? 5f : (float) mCurrentVelocity;
					mPitchOffset = (Math.min(Math.max(-mRealCurrentPitch, -45), 45) * 2) * (1/(1 + 0.75f*velocity));
				}
				else {
					mPitchOffset = Math.min(Math.max(-mRealCurrentPitch, -45), 45) * 2 ;
				}
			}

//			else {
//				Log.d(TAG,"In UpdateViewport - mCameraRespondsToUserVelocity = false"); 
				if(mCurrentAltitudeScale != mTargetAltitudeScale) {
//					Log.d(TAG, "CurrentAltitude not equal to TargetAltitude");
					float scaleDiff = mTargetAltitudeScale - mCurrentAltitudeScale;
//					mAltitudeToPitchRatio = Math.abs(scaleDiff) / mAltitudeScaleChangeRate;
					if(mTargetAltitudeScale > mCurrentAltitudeScale) {
						if(scaleDiff <= mAltitudeScaleChangeRate) {
							mCurrentAltitudeScale = mTargetAltitudeScale;
						}
						else {
							mCurrentAltitudeScale += mAltitudeScaleChangeRate;
//							Log.d(TAG, "Altitude incremented to " + String.valueOf(mCurrentAltitudeScale));
						}
					}
					else {
						if(scaleDiff >= -mAltitudeScaleChangeRate) {
							mCurrentAltitudeScale = mTargetAltitudeScale;
						}
						else {
							mCurrentAltitudeScale -= mAltitudeScaleChangeRate;
//							mAltitudeScaleFactor -= mAltitudeScaleChangeRate;
//							Log.d(TAG, "Altitude decremented to " + String.valueOf(mCurrentAltitudeScale));
						}
					}
				}
				else {
//					mLoadedAltitudeScale = mCurrentAltitudeScale;
					mScaleChangeDone = true;
				}
//			}
//			Log.e(TAG, "Pitch: " + mCurrentPitch + ", " + mTargetPitch);
			if(mCurrentPitch != mTargetPitch){
				double scaleDiff = mTargetPitch - mCurrentPitch;
				if(mTargetPitch > mCurrentPitch) {
					if(scaleDiff <= mPitchChangeRate) {
						mCurrentPitch = mTargetPitch;
					}
					else {
						mCurrentPitch += mPitchChangeRate;
//						Log.d(TAG, "Pitch incremented to " + String.valueOf(mCurrentPitch));
					}
				}
				else {
					if(scaleDiff >= -mPitchChangeRate) {
						mCurrentPitch = mTargetPitch;
					}
					else {
						mCurrentPitch -= mPitchChangeRate;
//						Log.d(TAG, "Pitch decremented to " + String.valueOf(mCurrentPitch));
					}
				}
			}
//			Log.d(TAG, "Altitude: " + String.valueOf(mCurrentAltitudeScale) + " ||| Pitch: " + String.valueOf(mCurrentPitch));
	
			if(mCurrentLongitude != mTargetLongitude  || mCurrentLatitude != mTargetLatitude) {
				double LongitudeDiff = mTargetLongitude - mCurrentLongitude;
				double latitudeDiff = mTargetLatitude - mCurrentLatitude;
				double changeRate = mChangeOffsetRateDegrees * (double)mCurrentAltitudeScale;
				double distFromTarget = Math.sqrt(LongitudeDiff*LongitudeDiff + latitudeDiff*latitudeDiff);
				if(distFromTarget <= changeRate) {
					mCurrentLongitude = mTargetLongitude;		// if close enough, snap to target
					mCurrentLatitude = mTargetLatitude;
				}
				else {
					mCurrentLongitude += changeRate * LongitudeDiff/distFromTarget;	// otherwise step to it
					mCurrentLatitude += changeRate * latitudeDiff/distFromTarget;
					
				}
				int newTileIndex = GeoTile.GetTileIndex(mCurrentLongitude, mCurrentLatitude);
				if(newTileIndex != mCurrentTileIndex) {
					Log.d(TAG, "--------------------- now in tile#" + newTileIndex);
					mCurrentTileIndex = newTileIndex;
				}

			}
	
			
			if(mCurrentRotationAngle != mTargetRotationAngle) {
				float rotationDiff = mTargetRotationAngle - mCurrentRotationAngle;
				if(rotationDiff < -180) rotationDiff += 360.0f;
				if(rotationDiff >= 180) rotationDiff -= 360.0f;
				float absRotDiff = Math.abs(rotationDiff);
				if(absRotDiff <= mChangeRotationRate) {
					mCurrentRotationAngle = mTargetRotationAngle;
				}
				else {
					float modifiedRotationRate = 1;
					if(FORCE_TAPER_PITCH_WHILE_MOVING){
						modifiedRotationRate = (float) ( 1f / ( 1f + 0.50f*mCurrentVelocity ) );
					}
					mCurrentRotationAngle = (mCurrentRotationAngle + modifiedRotationRate * mChangeRotationRate * rotationDiff/absRotDiff + 360.0f ) % 360.0f;
	//				Log.i(TAG,"Rot Angle " + mCurrentRotationAngle + " | " + mChangeRotationRate + " | " + rotationDiff+ " | " + (rotationDiff/absRotDiff));
				}
			}
			
		}
		CalculateAllParameters();		// call even if not initialized as this can change initialized state
	}	
	
	private void updateVelocity(){
		if(mCurrentVelocity != mTargetVelocity) {
			double velocityDiff = mTargetVelocity - mCurrentVelocity;
			if(mTargetVelocity > mCurrentVelocity) {
				if(velocityDiff <= mChangeVelocityRate) {
					mCurrentVelocity = mTargetVelocity;
				}
				else {
					mCurrentVelocity += mChangeVelocityRate;
				}
			}
			else {
				if(velocityDiff >= -mChangeVelocityRate) {
					mCurrentVelocity = mTargetVelocity;
				}
				else {
					mCurrentVelocity -= mChangeVelocityRate;
				}
			}
			
			if(mCameraRespondsToUserVelocity){
				mTargetPitch = CalcPitchFromVelocity(mCurrentVelocity);
				mTargetAltitudeScale = CalcScaleFromVelocity(mCurrentVelocity);
				mLoadNewTargetScale = true;
				mLoadedAltitudeScale = (mCurrentAltitudeScale > 0) ? mCurrentAltitudeScale : 1f;
			}
//			Log.d(TAG, "New Target Altitude scale calculated!");
		}
		else {
			if(mCameraRespondsToUserVelocity) mLoadNewTargetScale = false;
		}
	}

	
//---------------------- misc methods

	public ArrayList<PointXY> RemoveRedundantPathPointsForCurrentViewport(ArrayList<PointXY> pathNodes) {
	
//		Log.e(TAG, "PixelWidthInDeg = " + mPixelWidthInDeg);
		if(mPixelWidthInDeg > 0.f) {	// in case this API is called before camera is configured
			ArrayList<PointXY> filteredPath = new ArrayList<PointXY>();
			PointXY lastNode = null;
			for(PointXY node : pathNodes) {
				if(lastNode == null) {
					filteredPath.add(node);
					lastNode = node;
				}
				else {
					if( Math.abs((double)(node.x-lastNode.x)) > mPixelWidthInDeg || Math.abs((double)(node.y-lastNode.y)) > mPixelHeightInDeg ) {
						filteredPath.add(node);
						lastNode = node;
					}				
				}
			}
			
			return filteredPath;
		}
		else {
			return null;
		}
	}

	private float CalcPitchFromVelocity(double currentVelocity) {	// pitch curve assumed to be monotonically increasing values
		if(mPitchVelocityCurve.size() < 2) return 0.f;
		int cnt = 0;
		PointXY prevNode = new PointXY(0.f, 0.f);
		for(PointXY node : mPitchVelocityCurve) {
			if(cnt == 0) {
				prevNode = node;
			}
			else {
				if(currentVelocity >= prevNode.x  && currentVelocity < node.x) {
					if(currentVelocity == prevNode.x ) {
//						Log.e(TAG, "New pitch for velocity: " + currentVelocity + " = " + prevNode.y);
						return prevNode.y;
					}
					else {
//						Log.e(TAG, "New pitch for velocity: " + currentVelocity + " = " + (float) (((currentVelocity - prevNode.x) / (node.x - prevNode.x)) * (node.y - prevNode.y) + prevNode.y));
						return (float) (((currentVelocity - prevNode.x) / (node.x - prevNode.x)) * (node.y - prevNode.y) + prevNode.y);  //linear interp
					}
				}
				prevNode = node;
			}
			cnt++;
		}
		return 0.0f;		// if beyond range (either end)
	}
	
	private float CalcPitchFromAltitude(double currentAltitude){
		if(mPitchAltitudeCurve.size() < 2) return 0.f;
		int cnt = 0;
		PointXY prevNode = new PointXY(0.f, 0.f);
		for(PointXY node : mPitchAltitudeCurve) {
			if(cnt == 0) {
				prevNode = node;
			}
			else {
				if(currentAltitude >= prevNode.x  && currentAltitude < node.x) {
					if(currentAltitude == prevNode.x ) {
//						Log.e(TAG, "New pitch for velocity: " + currentAltitude + " = " + prevNode.y);
						return prevNode.y;
					}
					else {
//						Log.e(TAG, "New pitch for velocity: " + currentAltitude + " = " + (float) (((currentAltitude - prevNode.x) / (node.x - prevNode.x)) * (node.y - prevNode.y) + prevNode.y));
						return (float) (((currentAltitude - prevNode.x) / (node.x - prevNode.x)) * (node.y - prevNode.y) + prevNode.y);  //linear interp
					}
				}
				prevNode = node;
			}
			cnt++;
		}
		return 0.0f;		// if beyond range (either end)
	}
	
	private float CalcScaleFromVelocity(double currentVelocity) {	// altitude (scale) curve assumed to be monotonically decreasing values and set for maximum zoom scale... if zoomed in, curve will be scaled down
//		Log.e(TAG, "New velocity: " + currentVelocity);
		if(mAltitudeVelocityCurve.size() < 2) return 0.f;
		int cnt = 0;
		PointXY prevNode = new PointXY(0.f, 0.f);
		float firstValueOnCurve = mAltitudeVelocityCurve.get(0).y;
		float lastValueOnCurve = mAltitudeVelocityCurve.get(mAltitudeVelocityCurve.size()-1).y;
		float prevScaledValue = 0.f;
		float scaledValue = 0.f;
		float zoomDiffScaleRatio = (float) (mPresetZoomScale[mVelocityBaseZoomLevelIndex] / mPresetZoomScale[0]);
		for(PointXY node : mAltitudeVelocityCurve) {
			if(node.y != lastValueOnCurve) {
				scaledValue = node.y * zoomDiffScaleRatio ;		// scale curve to match current zoom level (represented by mVelocityBaseZoomLevelIndex)
				if(scaledValue < lastValueOnCurve) scaledValue = lastValueOnCurve;
			}
			else {
				scaledValue = lastValueOnCurve;
			}
//			Log.d(TAG,"Scale calc node.x= "+node.x +", .y= "+node.y +", scaledY= "+scaledValue+", zoomDiffScaleRatio= "+zoomDiffScaleRatio);
			
			if(cnt == 0) {
				prevNode = node;
				prevScaledValue = scaledValue;
			}
			else {
				if(currentVelocity >= prevNode.x  && currentVelocity < node.x) {
					if(currentVelocity == prevNode.x ) {
						Log.d(TAG, "New scale for velocity: " + currentVelocity + " = " + prevNode.y);
						return prevNode.y;
					}
					else {
						Log.d(TAG, "New scale for velocity: " + currentVelocity + " = " + (float) (((currentVelocity - prevNode.x) / (node.x - prevNode.x)) * (scaledValue - prevScaledValue) + prevScaledValue));
						return (float) (((currentVelocity - prevNode.x) / (node.x - prevNode.x)) * (scaledValue - prevScaledValue) + prevScaledValue);  //linear interp
					}
				}
				prevScaledValue = scaledValue;
				prevNode = node;
			}
			cnt++;

		}
		return (float) mPresetZoomScale[mVelocityBaseZoomLevelIndex];		// if beyond range (either end)
	}


	private void LoadVelocityCurves(Resources res) {

		mPitchVelocityCurve = new ArrayList<PointXY>();
		mPitchAltitudeCurve = new ArrayList<PointXY>();
		mAltitudeVelocityCurve = new ArrayList<PointXY>();

		if(mCameraRespondsToUserVelocity) {
			Log.d(TAG,"Loading velocity_curves.xml!");
		}
		else { 
			if(mVelocityCurvesNotLoadedCorrectly){
				Log.d(TAG,"Velocity curves not loaded properly!!");
			}
		}

		// create file path
		String fileName;
		XmlPullParser parser = Xml.newPullParser();
		BufferedReader br;
		int mode = -1;
		try {
			File path = Environment.getExternalStorageDirectory();
			File file;
			if(mCameraRespondsToUserVelocity){
				file = new File(path, PROFILE_FOLDER + "/" + "velocity_curves.xml"); 
			}
			else {
				file = new File(path, PROFILE_FOLDER + "/" + "scale_pitch_curves.xml"); 
			}
			br = new BufferedReader(new FileReader(file));
		    // auto-detect the encoding from the stream
		    parser.setInput(br);

		    boolean done = false;
		    int eventType = parser.getEventType();   // get and process event
		    
		    while (eventType != XmlPullParser.END_DOCUMENT && !done){
		        String name = null;
                
//		        name = parser.getName();
//		        if(name == null) name = "null";
//		        Log.e(TAG, "eventType:"+eventType + "-" + name + ", mVelocityCurvesNotLoadedCorrectly=" + mVelocityCurvesNotLoadedCorrectly);

		        switch (eventType){
		            case XmlPullParser.START_DOCUMENT:
		                name = parser.getName();
		                break;
		                
		            case XmlPullParser.START_TAG:
		                name = parser.getName();
		                if (name.equalsIgnoreCase("pitch")){
		                	mode = 0;
		                }
		                if (name.equalsIgnoreCase("altitude")){
		                	mode = 1;
		                }
		                
		                if (name.equalsIgnoreCase("node")){
		                	int numAttributes = parser.getAttributeCount();
		                	if(numAttributes < 2) {
		                		Log.e(TAG, "Bad node definition while parsing object in velocity_curves.xml file.  Too few attributes.");
	               				mVelocityCurvesNotLoadedCorrectly = true;
		                	}
		                	else {
		                		//if mCameraRespondsToUserVelocity is true, firstValue is a velocity value
		                		//else if mCameraRespondsToUserVelocity is false, firstValue is a scale value
	                			float firstValue = Float.parseFloat(parser.getAttributeValue(0));
	                			float secondValue = Float.parseFloat(parser.getAttributeValue(1));

	                			if(mode == 0) {
	                				if(mCameraRespondsToUserVelocity) {
	                					mPitchVelocityCurve.add(new PointXY(firstValue, secondValue));
	                				}
	                				else {
	                					mPitchAltitudeCurve.add(new PointXY(firstValue, secondValue));
	                				}
	                			}
	                			else {
	                				mAltitudeVelocityCurve.add(new PointXY(firstValue, secondValue));
	                			}
		                	}
		                }
		                break;

		            case XmlPullParser.END_TAG:
		                name = parser.getName();
		                break;
		            
		            case XmlPullParser.TEXT:
		                break;
		            }
		        eventType = parser.next();
		        }
		} 
		catch (FileNotFoundException e) {
			mVelocityCurvesNotLoadedCorrectly = true;
			e.printStackTrace();
		} 
		catch (IOException e) {
		    // TODO
			mVelocityCurvesNotLoadedCorrectly = true;
			e.printStackTrace();
		} 
		catch (Exception e){
		    // TODO
			mVelocityCurvesNotLoadedCorrectly = true;
			e.printStackTrace();

		}
			
		
		if(mVelocityCurvesNotLoadedCorrectly)  {
			Log.d(TAG, "Pitch curve size = " + mPitchVelocityCurve.size() + ",  Altitude curve size = " + mAltitudeVelocityCurve.size());
			Log.d(TAG, "Scale/Pitch curve size = " + mPitchAltitudeCurve.size());
		}
		
	}
	
	/**
	 * sets the Z-Near value for perspective projection.
	 * @param znear
	 * @return znear value
	 */
	public float setZNear(float znear){
		mProjectionZNear = znear;
		return mProjectionZNear;
	}
	
	/**
	 * If true, the pitch will have gradually less influence from the user
	 * based on the user's current velocity
	 * @param taperPitch
	 */
	public void setForceTaperPitchWhileMoving(boolean taperPitch){
		FORCE_TAPER_PITCH_WHILE_MOVING = taperPitch;
	}
	
	public float getAngleChangeDelta(){
		return mAltitudeScaleChangeRate;
	}
	/**
	 * 
	 * @return The maximum zoom scale, i.e. the distance between the Map and the Camera.
	 */
	public float getMaxZoomScale(){
		return (float)mMaxZoomScale;
	}
	/**
	 * 
	 * @return The minimum zoom scale, i.e. the distance between the Map and the Camera.
	 */
	public float getMinZoomScale(){
		return (float)mMinZoomScale;
	}
	public double getAspectRatio(){
		return mAspectRatio;
	}
	public float getZNear(){
		return mProjectionZNear;
	}
	public double getPanStepSizeMeters(){
		return mPanStepSizeInMeters;
	}
}
