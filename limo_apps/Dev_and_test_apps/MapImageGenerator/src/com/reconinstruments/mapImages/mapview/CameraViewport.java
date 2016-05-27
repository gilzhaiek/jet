package com.reconinstruments.mapImages.mapview;

import android.graphics.RectF;
import android.util.Log;

import com.reconinstruments.mapImages.helpers.LocationTransformer;

public class CameraViewport extends RectF {
	private static final String TAG = "CameraViewport";

//	private static final double NUDGE_PAN_SCALE = 0.1;
	private static final float DEFAULT_SCALE = 0.0f;
	private static final int MaxZoomLevel = 3;
	private static final int MinZoomLevel = 0;

	// conversion ratios
	float		mMetersPerPixel = LocationTransformer.DISTANCE_PER_PIXEL;
	
	// GPS coords
	public double 	mCenterLongitude;			
	public double	mCenterLatitude;
	// in meters
	double 			mLastRequestedRegionWidthInMeters = 0.0;		
	double			mBaseRegionWidthInMeters;
	double			mRegionWidthInMeters = 0.0;
	double			mRegionHeightInMeters = 0.0;
	double			mTargetRegionWidthInMeters = 0.0;
	double			mTargetRegionHeightInMeters = 0.0;
	double 			mPanStepSizeInMeters;				// in screen (canvas drawing) pixels
	// in screen pixels
	public int		mScreenWidthInPixels;
	public int		mScreenHeightInPixels;
	// unitless ratios
	double			mAspectRatio = 428.0/240.0;
	boolean			mBBDefined = false;
	
	double 			mMinScaleLevel = 0.25;
	double[] 		mPresetZoomScale;	// array of preset zoom scales
	
	float 			mTargetScale = DEFAULT_SCALE;
	float 			mTargetRotationAngle = 0.0f;	// clockwise from north = 0;
	double 			mTargetLongitude = 0.0;
	double 			mTargetLatitude = 0.0;
//	float 			mTargetXOffset = 0.0f;
//	float 			mTargetYOffset = 0.0f;
	float 			mCurrentScale = DEFAULT_SCALE;
	float 			mCurrentRotationAngle = 0.0f;	// clockwise from north = 0;
	double 			mCurrentLongitude = 0.0;			// relative to gps coordinates
	double 			mCurrentLatitude = 0.0;
//	float 			mCurrentXOffset = 0.0f;			// relative to drawing coordinates
//	float 			mCurrentYOffset = 0.0f;
	
	float 			mChangeScaleRate = 0.2f;
	float 			mChangeRotationRate = 3.0f;
	double 			mChangeOffsetRateDegrees = 0.0002;
//	float 			mChangeOffsetRate = 10.0f;

	RectF			mBB = new RectF();			// boundary boxes
	RectF			mTargetBB = new RectF();	// 
	RectF			mUserTestBB = new RectF();	// 
	
//	public  Matrix  mMatrix = new Matrix();

	
	
	public enum ContainResult {
		NO,
		YES_INBOUNDARY_DATALOADING,
		YES_DATALOADING,
		YES_INBOUNDARY_DATALOADED,
		YES_DATALOADED
	}

	public CameraViewport(double regionWidth, double[] zoomLevels, double minZoomLevel, float scaleRate, float rotationRateInDegrees, double panRateInDegrees) {  
		mLastRequestedRegionWidthInMeters = regionWidth;
		mPresetZoomScale= new double[MaxZoomLevel+1];
		
//		mPresetZoomScale[0] = 0.5;	// can change to be less than 1 to accommodate large resorts
//		mPresetZoomScale[1] = 1.5;
//		mPresetZoomScale[2] = 3.0;
//		mPresetZoomScale[3] = 6.0;
		mPresetZoomScale[0] = zoomLevels[0];	// can change to be less than 1 to accommodate large resorts
		mPresetZoomScale[1] = zoomLevels[1];
		mPresetZoomScale[2] = zoomLevels[2];
		mPresetZoomScale[3] = zoomLevels[3];
		
		mMinScaleLevel = minZoomLevel;
//		Log.e(TAG, "zooms:" + mMinScaleLevel + " - " + mPresetZoomScale[0] + ", " + mPresetZoomScale[1] + ", " + mPresetZoomScale[2] + ", " + mPresetZoomScale[3] );
	
		mChangeScaleRate = scaleRate;
		mChangeRotationRate = rotationRateInDegrees;
		mChangeOffsetRateDegrees = panRateInDegrees;

	}

	public void AdjustClosestZoomScale(double scale) {
		int closest=0;
//		double closestScale = mPresetZoomScale[0];
		for(int i=1; i<4;i++) {
			if(Math.abs(scale-mPresetZoomScale[i]) < Math.abs(scale-mPresetZoomScale[i-1])) {
				closest = i;
			}
		}
		mPresetZoomScale[closest] = scale;
	}

	public void SetViewPortDimensions(int width, int height) {
//		mScreenWidthInPixels = width;
//		mScreenHeightInPixels = height;
		mScreenWidthInPixels = 428;
		mScreenHeightInPixels = 240;
		mAspectRatio = (double)width/(double)height;		// TODO fix initial vs final aspect ration setting, hardcoded for now
		Log.i(TAG, "VP Dimensions: "+ mScreenWidthInPixels + "x"+ mScreenHeightInPixels + " - "+ mAspectRatio);

		mBaseRegionWidthInMeters = mScreenWidthInPixels * mMetersPerPixel;
		
		left   = 0;
		right  = mScreenWidthInPixels - 1;
		top = 0;
		bottom = mScreenHeightInPixels -1;
		
		mUserTestBB.left = left - 20;
		mUserTestBB.right = right + 20;
		mUserTestBB.top = top - 20;
		mUserTestBB.bottom = bottom + 20;
		
//		mZoomLevel = 0;
		if(mTargetScale == DEFAULT_SCALE) {
			mTargetScale = (float)mPresetZoomScale[0];
			for(int zl = 1; zl <= MaxZoomLevel; zl++) {
				if(mLastRequestedRegionWidthInMeters < mBaseRegionWidthInMeters / mPresetZoomScale[zl] ){
					//				mZoomLevel++;
					mTargetScale = (float)mPresetZoomScale[zl];
				}
			}	
		}
		mCurrentScale = mTargetScale;
		CalcDistanceParameters();
	}

	public boolean showPathNames() {
//		Log.i(TAG, "in showPathNames() " + mRegionWidthInMeters);
		if(mTargetScale > 1.1*mPresetZoomScale[0]) return true;  // TODO in future, make this screen independent  in terms of m/display cm
		else return false;
	}
	
	// adjust viewport center
	private void CalculateAllParameters() {
		CalcDistanceParameters();
		CalcBoundingBox();
		CalcTargetBoundingBox();
	}

	private void CalcDistanceParameters() {
		mRegionWidthInMeters = mBaseRegionWidthInMeters / mCurrentScale; 
		mRegionHeightInMeters =  mRegionWidthInMeters / mAspectRatio;
		mPanStepSizeInMeters = mRegionWidthInMeters / 2.0; // step 1/10 of screen width with each pan adjustment, but will be stopped when user releases button

		mTargetRegionWidthInMeters = mBaseRegionWidthInMeters / mTargetScale; 
		mTargetRegionHeightInMeters =  mTargetRegionWidthInMeters / mAspectRatio;
	}

	public void CalcTargetBoundingBox() {	// does not include pan offsets
		double BBDistInMeters = (Math.sqrt(mTargetRegionWidthInMeters * mTargetRegionWidthInMeters + mTargetRegionHeightInMeters * mTargetRegionHeightInMeters))/2.0;
//		Log.i(TAG,"target w/h:" +mTargetRegionWidthInMeters + ", " +mTargetRegionHeightInMeters + ", " + BBDistInMeters+ ", " + mCurrentLatitude+ ", " + mCurrentLongitude);
		mTargetBB.left   = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, -BBDistInMeters);
		mTargetBB.right  = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, +BBDistInMeters);
		mTargetBB.top    = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, -BBDistInMeters);
		mTargetBB.bottom = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, +BBDistInMeters);
//		Log.i(TAG, "vpTarBB:"+ BBDistInMeters +" - "+ mTargetBB.left +" - "+ mTargetBB.right +" ; "+ mTargetBB.top +" - "+ mTargetBB.bottom);
	}

	private void CalcBoundingBox() {	// does not include pan offsets
		if(mRegionWidthInMeters != 0.0) {
//			left   = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, -mRegionWidthInMeters);
//			right  = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, +mRegionWidthInMeters);
//			top    = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, -mRegionHeightInMeters);
//			bottom = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, +mRegionHeightInMeters);
//			Log.i(TAG, "vpRect: "+ left  +" - "+ right +" ; "+ top +" - "+ bottom+" | "+ mCurrentLatitude +" - "+ mCurrentLongitude);
			
			double BBDistInMeters = (Math.sqrt(mRegionWidthInMeters * mRegionWidthInMeters + mRegionHeightInMeters * mRegionHeightInMeters))/2.0;
			mBB.left = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, -BBDistInMeters);
			mBB.right  = LongitudeAtLocationPlusHorizontalOffset(mCurrentLatitude, mCurrentLongitude, +BBDistInMeters);
			mBB.top    = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, -BBDistInMeters);	// top/bottom reversed as RectF methods are defined for graphics, y +ve down
			mBB.bottom = LatitudeAtLocationPlusVerticalOffset(mCurrentLatitude, mCurrentLongitude, +BBDistInMeters);
//			Log.i(TAG, "vpBB:   "+ BBDistInMeters +" - "+ mBB.left +" - "+ mBB.right +" ; "+ mBB.top +" - "+ mBB.bottom);
			mBBDefined = true;

		}
	}
	public boolean BoundingBoxDefined() {
		return mBBDefined;
	}
	
	
	protected float LongitudeAtLocationPlusHorizontalOffset(double refLatitude, double refLongitude, double distInMeters) {
		double equitorialCircumference = 40075017.0; // m  - taken from wikipedia/Earth

		return (float)(refLongitude + distInMeters/(equitorialCircumference*Math.cos(Math.toRadians(refLatitude))) * 360.0);	
	}

	protected float LatitudeAtLocationPlusVerticalOffset(double refLatitude, double refLongitude, double distInMeters) {
		double meridionalCircumference = 40007860.0; // m  - taken from wikipedia/Earth

		return (float)(refLatitude + distInMeters/meridionalCircumference * 360.0);
	}

	
	public ContainResult Contains(CameraViewport geoRegion, float boundaryRatio) {
		// add logic to set state
		
		return ContainResult.NO;
	}
	

	
	//---------------------- adjusting scale
	public void SetScale(float scale, boolean immediate) {			
		mTargetScale = scale;
		if(immediate || mCurrentScale == 0.0f) {
			mCurrentScale = mTargetScale;
		}
//		Log.d(TAG, "setting scale " + mScale);
//		CalculateAllParameters();
	}
	
	public float GetCurrentScale() {			
		return mCurrentScale;
	}
	
	public void FreezePan() {
//		mTargetRotationAngle = mCurrentRotationAngle;	
		mTargetLatitude = mCurrentLatitude;
		mTargetLongitude = mCurrentLongitude;
		CalculateAllParameters();
	}
	public void UpdateTarget(float scale) {
		mTargetScale = scale;
		CalculateAllParameters();
	}

	public double ZoomIn() {
		double newScale = mPresetZoomScale[0];
		for(int zl = 0; zl <= MaxZoomLevel; zl++) {
			if(mTargetScale >= 0.98* mPresetZoomScale[zl]  ){
				if(zl < MaxZoomLevel) {
					newScale = mPresetZoomScale[zl+1];
				}
				else { // == MaxZoomLevel
					newScale = mPresetZoomScale[0]; // cycle around
				}
			}
		}	
		mTargetScale = (float)newScale;
		CalcDistanceParameters() ;
		return mTargetScale;
	}
	
	public double ZoomOut() {
		double newScale = mPresetZoomScale[3];
		for(int zl = 3; zl >= MinZoomLevel; zl--) {
			if(mTargetScale <= mPresetZoomScale[zl]  ){
				if(zl > MinZoomLevel) {
					newScale = mPresetZoomScale[zl-1];
				}
				else { // == MinZoomLevel
					newScale = mPresetZoomScale[3];	// cycle around
				}
			}
		}	
		mTargetScale = (float)newScale;
		CalcDistanceParameters() ;
		return mTargetScale;
	}
	
	public double SetScaleToHoldLocations(double lat1, double long1, double lat2, double long2) {
		// set mScale such that it holds both points... capped at minimum scale
		
		//TODO future
		
		return 1.0;
	}
	
	public void SetMinScaleForResortWidthInKms(double resortWidthInKms) {
		boolean atZoom0 = (mTargetScale == mPresetZoomScale[0]);
		
		mPresetZoomScale[0] = mBaseRegionWidthInMeters / 1000.0 / resortWidthInKms;;
		if(mPresetZoomScale[0] < mMinScaleLevel) mPresetZoomScale[0] = mMinScaleLevel;
	
		if(atZoom0) {
			mTargetScale = (float)mPresetZoomScale[0];
//			CalculateAllParameters();
		}
	}

	//---------------------- adjusting rotation
	public void SetViewAngleRelToNorth(float angle, boolean immediate) {
		mTargetRotationAngle = angle;
//		Log.e(TAG,"Setting map rot angle: "+angle+ ", user angle:"+(360.0-angle));
		if(immediate || mCurrentRotationAngle == 0.0f) {
			mCurrentRotationAngle = mTargetRotationAngle;
		}
	}

	//---------------------- adjusting center
	public void SetCenter(double newLatitude, double newLongitude, boolean immediate) { 
		mTargetLatitude = (float)newLatitude;
		mTargetLongitude = (float)newLongitude;
		if(immediate || (mCurrentLongitude == 0.0f && mCurrentLatitude == 0.0f)) {
			mCurrentLatitude = mTargetLatitude;
			mCurrentLongitude = mTargetLongitude;
		}
	}
	
	protected void limitTargetPans() {
		// don't let target get to far ahead of screen, may cause extraneous loading that is inconsistent with user experience
//		double limitFactor = 100.0;
//		if(mTargetLongitude < mCurrentLongitude - limitFactor * mChangeOffsetRateDegrees) {
//			mTargetLongitude = mCurrentLongitude - limitFactor * mChangeOffsetRateDegrees;
////			Log.i(TAG, "limiting target long below: " + mTargetLongitude+ " - "+ mCurrentLongitude);
//		}
//		if(mTargetLongitude > mCurrentLongitude + limitFactor * mChangeOffsetRateDegrees) {
//			mTargetLongitude = mCurrentLongitude + limitFactor * mChangeOffsetRateDegrees;
////			Log.i(TAG, "limiting target long above: " + mTargetLongitude+ " - "+ mCurrentLongitude);
//		}
//		if(mTargetLatitude < mCurrentLatitude - limitFactor * mChangeOffsetRateDegrees) {
//			mTargetLatitude = mCurrentLatitude - limitFactor * mChangeOffsetRateDegrees;
////			Log.i(TAG, "limiting target lat below: " + mTargetLatitude+ " - "+ mCurrentLatitude);
//		}
//		if(mTargetLatitude > mCurrentLatitude + limitFactor * mChangeOffsetRateDegrees) {
//			mTargetLatitude = mCurrentLatitude + limitFactor * mChangeOffsetRateDegrees;
////			Log.i(TAG, "limiting target lat above: " + mTargetLatitude+ " - "+ mCurrentLatitude);
//		}
	}
	
	public void panLeft(boolean nudge) {
		double oldLat = mTargetLatitude;
		double oldLong = mTargetLongitude;
		double panAmount = mPanStepSizeInMeters;
		double angleLeftDir = mCurrentRotationAngle+270.0;;
		mTargetLongitude = LongitudeAtLocationPlusHorizontalOffset(oldLat, oldLong, +panAmount * Math.sin(Math.toRadians(angleLeftDir) ));
		mTargetLatitude  = LatitudeAtLocationPlusVerticalOffset(oldLat, oldLong, +panAmount * Math.cos(Math.toRadians(angleLeftDir) ));
//		Log.i(TAG,"Panleft:  " + angleLeftDir + " | "+ oldLat + ", "+ mTargetLatitude + " - "+ oldLong + ", "+ mTargetLongitude );
//		limitTargetPans();	
	}

	public void panRight(boolean nudge) {
		double oldLat = mTargetLatitude;
		double oldLong = mTargetLongitude;
		double panAmount = mPanStepSizeInMeters;
		double angleRightDir = mCurrentRotationAngle+90.0;
		mTargetLongitude = LongitudeAtLocationPlusHorizontalOffset(oldLat, oldLong, +panAmount * Math.sin(Math.toRadians(angleRightDir) ));
		mTargetLatitude  = LatitudeAtLocationPlusVerticalOffset(oldLat, oldLong, +panAmount * Math.cos(Math.toRadians(angleRightDir) ));
//		Log.i(TAG,"Panright: " + angleRightDir + " | "+ oldLat + ", "+ mTargetLatitude + " - "+ oldLong + ", "+ mTargetLongitude );
//		limitTargetPans();	
	}

	public void panUp(boolean nudge) { 
		double oldLat = mTargetLatitude;
		double oldLong = mTargetLongitude;
		double panAmount = mPanStepSizeInMeters;
		double angleUpDir = mCurrentRotationAngle;
//		Log.e(TAG,"Panup: "+ (int)angleUpDir + ": "+ (Math.sin(Math.toRadians(angleUpDir))) + ", "+ Math.cos(Math.toRadians(angleUpDir) ));
		mTargetLongitude = LongitudeAtLocationPlusHorizontalOffset(oldLat, oldLong, +panAmount * Math.sin(Math.toRadians(angleUpDir) ));
		mTargetLatitude  = LatitudeAtLocationPlusVerticalOffset(oldLat, oldLong, +panAmount * Math.cos(Math.toRadians(angleUpDir) ));
//		Log.i(TAG,"Panup:    " + angleUpDir + " | "+ oldLat + ", "+ mTargetLatitude + " - "+ oldLong + ", "+ mTargetLongitude );
		limitTargetPans();	
//		Log.i(TAG,"Panupclipped: " + oldLat + ", "+ mTargetLatitude + " - "+ oldLong + ", "+ mTargetLongitude );
	}

	public void panDown(boolean nudge) {	
		double oldLat = mTargetLatitude;
		double oldLong = mTargetLongitude;
		double panAmount = mPanStepSizeInMeters;
		double angleDownDir = mCurrentRotationAngle+180.0;
		mTargetLongitude = LongitudeAtLocationPlusHorizontalOffset(oldLat, oldLong, +panAmount * Math.sin(Math.toRadians(angleDownDir) ));
		mTargetLatitude  = LatitudeAtLocationPlusVerticalOffset(oldLat, oldLong, +panAmount * Math.cos(Math.toRadians(angleDownDir) ));
//		Log.i(TAG,"Pandown:  " + angleDownDir + " | "+ oldLat + ", "+ mTargetLatitude + " - "+ oldLong + ", "+ mTargetLongitude );
		limitTargetPans();	
	}
	
	//---------------------- reset
	public void Reset() {
		mTargetScale = DEFAULT_SCALE;
		mTargetRotationAngle = 0.0f;	
		mTargetLatitude = 0.0f;
		mTargetLongitude = 0.0f;
		mCurrentScale = 0.0f;
		mCurrentRotationAngle = 0.0f;	
		mCurrentLongitude = 0.0f;
		mCurrentLatitude = 0.0f;
	}
	
//	public void SetTargetXOffset(float xOffset) {
//		mTargetXOffset = xOffset;
//	}
//	
//	public void SetTargetYOffset(float yOffset) {
//		mTargetYOffset = yOffset;
//	}


	public void UpdateViewport() {
//		Log.i(TAG,"updating viewport: "+ (mCurrentRotationAngle - mTargetRotationAngle));
		if(mCurrentScale != mTargetScale) {
			float scaleDiff = mTargetScale - mCurrentScale;
			if(mTargetScale > mCurrentScale) {
				if(scaleDiff <= mChangeScaleRate) {
					mCurrentScale = mTargetScale;
				}
				else {
					mCurrentScale += mChangeScaleRate;
				}
			}
			else {
				if(scaleDiff >= -mChangeScaleRate) {
					mCurrentScale = mTargetScale;
				}
				else {
					mCurrentScale -= mChangeScaleRate;
				}
			}
		}

		if(mCurrentLongitude != mTargetLongitude  || mCurrentLatitude != mTargetLatitude) {
			double LongitudeDiff = mTargetLongitude - mCurrentLongitude;
			double latitudeDiff = mTargetLatitude - mCurrentLatitude;
			double changeRate = mChangeOffsetRateDegrees / (double)mCurrentScale;
			double distFromTarget = Math.sqrt(LongitudeDiff*LongitudeDiff + latitudeDiff*latitudeDiff);
			if(distFromTarget <= changeRate) {
				mCurrentLongitude = mTargetLongitude;		// if close enough, snap to target
				mCurrentLatitude = mTargetLatitude;
			}
			else {
				mCurrentLongitude += changeRate * LongitudeDiff/distFromTarget;	// otherwise step to it
				mCurrentLatitude += changeRate * latitudeDiff/distFromTarget;
				
			}
//			Log.i(TAG,"TargetLong " + mTargetLongitude + " - " + mCurrentLongitude);
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
				mCurrentRotationAngle = (mCurrentRotationAngle + mChangeRotationRate * rotationDiff/absRotDiff + 360.0f ) % 360.0f;
//				Log.i(TAG,"Rot Angle " + mCurrentRotationAngle + " | " + mChangeRotationRate + " | " + rotationDiff+ " | " + (rotationDiff/absRotDiff));
			}
		}

		CalculateAllParameters();
	}
	
	
}
