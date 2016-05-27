package com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.subclass;

import java.util.ArrayList;
import java.util.TreeMap;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.text.TextPaint;
import android.util.Log;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortInfoResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.ResortRequest;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability.DataSources;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.MapView.MapViewMode;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.geodataservice.DynamicGeoDataInterface.IDynamicGeoData;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer.BackgroundSize;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MapRLDrawingSet;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL.MeshType;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticuleLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;
import com.reconinstruments.mapsdk.mapview.renderinglayers.usericonlayer.UserIconLayer;
import com.reconinstruments.utils.SettingsUtil;

public class SnowReticleLayer extends ReticuleLayer implements IDynamicGeoData {
// constants
	private final static String TAG = "SnowReticleLayer";
	private final static int MAX_RESORTS_ON_RETICLE = 5; 
	private final static float MAX_DISTANCE_TO_RESORTS = 500.0f; // kms
	private final static float MAX_DISPLAY_DISTANCE_TO_RESORTS = 50.0f; // kms
	private final static float MAX_DISPLAY_DISTANCE_TO_RESORTS_IF_IN_RESORT = 20.0f; // kms
	private final static int MAX_NUMBER_OF_RESORTS = 15 ;
	private final static float RESORT_FOCUS_HORIZONTAL_THRESHOLD = 15.f ;
	
	/*
	 * This is the default Z-Offset of each reticle item, where +Z
	 * goes out of the screen.
	 */
	protected static final float		Z_OFFSET_RET_ITEM = 0.04f;
	
	protected static final float		Z_OFFSET_RET_ICON = 0.03f;

// members
	protected SnowReticleDrawingSet		mSnowReticleDrawingSet;
	protected Bitmap					mResortReticleIcon = null;
	protected Bitmap					mResortReticleIconFocused = null;
	protected ArrayList<ResortInfoResponse> 	mResortReticleItems;
	float 								mCurrentRotationAngle = 0.f;
	protected Matrix					mReticleImageTransformMatrix = new Matrix();
	TreeMap<Float, ResortInfoResponse> sortedResortInfo = new TreeMap<Float, ResortInfoResponse>();  // preallocate for efficiency
	
	boolean								mNameResortByPointingAtIt = false;
	
	int									mMaxNameBoxAlpha = 255;
	int									mMaxNameBoxOutlineAlpha = 255;
	boolean								mUpdatingCamera = false;
	boolean 							mFirstLabel = true;
	float 								mTimeFactor = 1.f;
	protected Paint		    			mResortLabelPaint;				// TODO remove when these items moved to their own layers

	boolean 							mUnitsAreMetric;


	public  static IGeodataService		mGeodataServiceInterface  = null;
	protected GeoDataServiceState		mGeodataServiceState = null;
	protected ArrayList<ResortInfoResponse> mResortList = null;
	PointXY								mLastCameraPosition = null;
	ResortInfoResponse					mCurrentResort = null;
	ResortInfoResponse					mNewResort = null;
	long								mNewResortTime = 0;
	long								mTimeInCurrentResort = 0;
	protected float[] 					mDrawingReticleItemCenter = new float[2];
	boolean								mLoadingResorts = false;
	PointXY 							resortDrawingCoords;
	PointXY 							mCurCameraPosition;
	
	/**
	 * Reference to the maplayer's drawing set
	 */
	MapRLDrawingSet						mMapLayerDrawingSet;
	
	/**
	 * Reference to the User icon layer
	 */
	protected UserIconLayer				mUserIconLayer;
	
	
	/**
	 * This matrix is used for calculating the distance between the 
	 * center of the reticule and an item on its circumference.
	 */
	protected float[]					mReticuleItemDistMatrix;
	
	/**
	 * The vector represents the distance between the center of
	 * the reticule and an item on its circumference.
	 */
	protected float[]					mReticleItemDistVector;
	
	protected float[]					mZOffsetMatrix;
	
	protected float[]					mCurrentZOffset;
	
	protected float[]					mReticleItemsToRender;
	
	/**
	 * This is the distance between the drawing viewport center and
	 * the user icon, which can be greater than 0 while in zoom/pan mode
	 */
	protected float[]					mUserIconDistanceFromCenter;
	
	
// methods	
	public SnowReticleLayer(Activity parentActivity, RenderSchemeManager rsm, World2DrawingTransformer world2DrawingTransformer, boolean rolloverNamingEnabled, BackgroundSize bkgdSize) throws Exception {
		super(parentActivity, rsm, world2DrawingTransformer, rolloverNamingEnabled, bkgdSize);
		
		
		mSnowReticleDrawingSet = new SnowReticleDrawingSet(rsm);
		
		mMaxNameBoxAlpha = mRSM.GetPanRolloverBoxBGAlpha();
		mMaxNameBoxOutlineAlpha = mRSM.GetPanRolloverBoxOutlineAlpha();
		
		mResortLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
		mResortLabelPaint.setTextSize(mRSM.GetResortNameTextSize());
		mResortLabelPaint.setAntiAlias(true);
		mResortLabelPaint.setTextAlign(Align.CENTER);
		mResortLabelPaint.setTypeface(Typeface.SANS_SERIF);
		mResortLabelPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));		
		mResortLabelPaint.setColor(mRSM.GetResortNameTextColor());		
		mResortLabelPaint.setAlpha(255);

		mUnitsAreMetric = SettingsUtil.getUnits(mParentActivity) == SettingsUtil.RECON_UINTS_METRIC;
		
		mResortReticleIcon = BitmapFactory.decodeResource(mParentActivity.getResources(), R.drawable.resort_reticule_icon);
		mResortReticleIconFocused = BitmapFactory.decodeResource(mParentActivity.getResources(), R.drawable.resort_reticule_icon_focus);
		
//		PointXY pointList[] = {new PointXY(100f, 0f), //buddy
//				new PointXY(0f, -900f), 				//carpark
//				new PointXY(-500f, 0f),				//chairlift
//				new PointXY(0f, 324f),				//info
//				new PointXY(-2f, 2f)};					//restaurant
		mResortReticleItems = new ArrayList<ResortInfoResponse>();
//		for(int i = 0; i < 5; i++){
//			mResortReticleItems.add(new ResortInfoResponse(BitmapTypes.values()[i].toString(), 0, pointList[i], new RectXY(-100, 300, 600, -200), true));
//			Log.i(TAG, "resort reticle item " + mResortReticleItems.get(i).mName +" created!");
//		}
		
		mReticuleItemDistMatrix = new float[16];
		mReticleItemDistVector = new float[4];
		mZOffsetMatrix = new float[16];
		mCurrentZOffset = new float[4];
		mUserIconDistanceFromCenter = new float[2];	
	}
	
	@Override
	public void Resume() {
		super.Resume();
		mUnitsAreMetric = SettingsUtil.getUnits(mParentActivity) == SettingsUtil.RECON_UINTS_METRIC;
//		Log.d(TAG, "units are metric 1 = " + mUnitsAreMetric);
	}

	@Override
	public void Draw(Canvas canvas, CameraViewport camera, String focusedObjectID, Resources res) {
		if(mEnabled) {
			// draw reticle
			mReticleImageTransformMatrix.setTranslate(((float)camera.mScreenWidthInPixels-mReticuleImage.getWidth())/2.0f,((float)camera.mScreenHeightInPixels-mReticuleImage.getHeight())/2.0f);
			canvas.drawBitmap(mReticuleImage, mReticleImageTransformMatrix, null);	
			
			ResortInfoResponse focusedResortInfo = null;
			float closestResortX = 99999.f;
			
			mResortReticleItems = mSnowReticleDrawingSet.GetCurrentResortReticleItems();
//			mResortReticleItems = (ArrayList<ResortInfoResponse>)mSnowReticleDrawingSet.GetCurrentResortReticleItems().clone();
			
			int cnt = 0;
			for(ResortInfoResponse resortInfo : mResortReticleItems) {		// draw resort icons first
				resortDrawingCoords = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(resortInfo.mLocation);
				mDrawingReticleItemCenter[0] = (float)resortDrawingCoords.x;
				mDrawingReticleItemCenter[1] = (float)resortDrawingCoords.y;
				mDrawTransformMatrix.mapPoints(mDrawingReticleItemCenter);
//				if(cnt==0) Log.d(TAG, "Resort Icon 0: " + resortDrawingCoords.x + ", "+ resortDrawingCoords.y + ", "+ mDrawingResortCenter[0] + ", "+ mDrawingResortCenter[1] + ", "+ camera.mCurrentRotationAngle );
				
				float diffX = mDrawingReticleItemCenter[0]-mDrawingViewPortCenter[0];
				float diffY = mDrawingReticleItemCenter[1]-mDrawingViewPortCenter[1];
				float mag = (float) Math.sqrt(diffX*diffX + diffY*diffY);
				float rx = diffX/mag * RETICULE_DISTANCE + mDrawingViewPortCenter[0];
				float ry = diffY/mag * RETICULE_DISTANCE + mDrawingViewPortCenter[1];
				resortInfo.mScreenLocation = new PointXY(rx, ry);
				float testX = Math.abs(rx - mDrawingViewPortCenter[0]);
//				Log.e(TAG, "ResortReticle: " + resortInfo.mName + ", " + testX + ", " + closestResortX + ", " + focusedResortInfo);
				if(testX < RESORT_FOCUS_HORIZONTAL_THRESHOLD && (ry-mDrawingViewPortCenter[1]) < 0.f && testX < closestResortX) {
					focusedResortInfo = resortInfo;
					closestResortX = testX;
				}
				cnt++;
			}
			
			for(ResortInfoResponse resortInfo : mResortReticleItems) {		// draw resort icons first
				if(mNameResortByPointingAtIt && resortInfo == focusedResortInfo) {
					canvas.drawBitmap(mResortReticleIconFocused, resortInfo.mScreenLocation.x - mResortReticleIcon.getWidth()/2, resortInfo.mScreenLocation.y-mResortReticleIcon.getHeight()/2, null);				// fixed user icon
					}
				else {
					canvas.drawBitmap(mResortReticleIcon, resortInfo.mScreenLocation.x - mResortReticleIcon.getWidth()/2, resortInfo.mScreenLocation.y-mResortReticleIcon.getHeight()/2, null);				// fixed user icon
				}
			}
			
			// draw reticle items if they exist
			for(ReticleItem item : mReticleItems) {
				float diffX = item.mDrawingLocation.x-mDrawingViewPortCenter[0];
				float diffY = item.mDrawingLocation.y-mDrawingViewPortCenter[1];
				float mag = (float) Math.sqrt(diffX*diffX + diffY*diffY);
				float rx = diffX/mag * RETICULE_DISTANCE + mDrawingViewPortCenter[0];
				float ry = diffY/mag * RETICULE_DISTANCE + mDrawingViewPortCenter[1];
				canvas.drawBitmap(item.mImage, rx- item.mImage.getWidth()/2, ry-item.mImage.getHeight()/2, null);				// fixed user icon
			}
			
			if(mNameResortByPointingAtIt && focusedResortInfo != null) {
				DisplayResortName(canvas, focusedResortInfo, camera, true, 1000, 1, -camera.mStatusBarHeight);
			}
			else {
				boolean blockRollover = false;
				if(mCurrentResort != null) {
					if(mNewResort== null || !mNewResort.mName.equalsIgnoreCase(mCurrentResort.mName)) {
//						Log.d(TAG, "Changing resort: " + mNewResort + ", " + mCurrentResort);
						mNewResort = mCurrentResort;
						mNewResortTime = System.currentTimeMillis();
						if(mFirstLabel) mTimeFactor = 2.0f;

					}
					long displayTime =  System.currentTimeMillis() - mNewResortTime;
//					Log.e(TAG, "Display time for new resort: " + displayTime);
					if(displayTime < 2500*mTimeFactor) {		
						DisplayResortName(canvas, mNewResort, camera, false, displayTime, mTimeFactor, -camera.mStatusBarHeight);
						blockRollover = true;
					}
					else {
						mFirstLabel = false;
						mTimeFactor = 1.0f;
					}
				}
				else {
					DisplayResortName(canvas, mCurrentResort, camera, false, 2000, 1, -camera.mStatusBarHeight); // make sure it turns name off after leaving
					mNewResort = null;
				}
				if(!blockRollover) {
					// show rollover if enabled and an item in focus
					if(mRollOverResult != null) {	// put description of object with focus on map
						mClosestItemBoxPaint.setAlpha(mMaxNameBoxAlpha);
						mClosestItemBoxOutlinePaint.setAlpha(mMaxNameBoxOutlineAlpha);
						mClosestItemOutlinePaint.setAlpha(255);
						mClosestItemPaint.setAlpha(255);
						
						float xoffset = mClosestItemPaint.measureText(mRollOverResult.mDescription)/2.f;
						mDescRect.left = (float)(camera.mScreenWidthInPixels/2-(xoffset*1.2));
						mDescRect.top = (float)(camera.mScreenHeightInPixels-26 -20);
						mDescRect.right = (float)(camera.mScreenWidthInPixels/2+(xoffset*1.2)); 
						mDescRect.bottom = (float)(camera.mScreenHeightInPixels+15 -20);
						canvas.drawRect(mDescRect, mClosestItemBoxPaint);
						canvas.drawRect(mDescRect, mClosestItemBoxOutlinePaint);
						canvas.drawText(mRollOverResult.mDescription, camera.mScreenWidthInPixels/2, camera.mScreenHeightInPixels -20, mClosestItemOutlinePaint);
						canvas.drawText(mRollOverResult.mDescription, camera.mScreenWidthInPixels/2, camera.mScreenHeightInPixels -20, mClosestItemPaint);
					}
				}
			}
		}
	}
	
	@Override
	public void Draw(RenderSchemeManager rsm, Resources res, CameraViewport camera, String focusedObjectID, boolean loadNewTexture, MapViewMode viewMode, GLText glText){
			if(thisIsTheFirstRun) {
				Log.i(TAG, ">>>>>> Entering SnowReticleLayer DRAW!!");
			}
			MeshGL reticleMesh = rsm.getMeshOfType(MeshGL.MeshType.RETICLE.ordinal());
			mReticuleScaleFactor = camera.getMaxZoomScale() * mMapImageSize/12f;
			float altitudeScale = camera.mCurrentAltitudeScale;
			
			//Draw reticle circle with dot
			android.opengl.Matrix.setIdentityM(reticleMesh.mModelMatrix, 0);
			android.opengl.Matrix.scaleM(reticleMesh.mModelMatrix, 0, mReticuleScaleFactor, mReticuleScaleFactor, mReticuleScaleFactor);
			android.opengl.Matrix.translateM(reticleMesh.mModelMatrix, 0, 0, 0, Z_OFFSET_RET_ICON);
			reticleMesh.loadMeshTexture(mReticuleImage, thisIsTheFirstRun, 1);
			reticleMesh.drawMesh(camera.mCamViewMatrix, camera.mCamProjMatrix);
			
			// draw reticle items on top of circle
			for(ReticleItem reticleItem : mReticleItems) {
				mDrawingReticleItemCenter[0] = (float)reticleItem.mDrawingLocation.x;
				mDrawingReticleItemCenter[1] = (float)reticleItem.mDrawingLocation.y;
				float diffX = mDrawingReticleItemCenter[0]-mDrawingViewPortCenter[0];
				float diffY = mDrawingReticleItemCenter[1]-mDrawingViewPortCenter[1];
				float inverseSqrt = 1f / (float)Math.sqrt(diffX*diffX + diffY*diffY);
				float rx = diffX * inverseSqrt;
				float ry = diffY * inverseSqrt;
				mReticleItemDistVector[0] = rx;
				mReticleItemDistVector[1] = -ry;
				
				//scaling mReticleItemDistVector to be equal to the distance between the reticle center
				// and its edge
				android.opengl.Matrix.setIdentityM(mReticuleItemDistMatrix, 0);
				android.opengl.Matrix.scaleM(mReticuleItemDistMatrix, 0, mReticuleScaleFactor/2.1f, mReticuleScaleFactor/2.1f, 1f);
				android.opengl.Matrix.multiplyMV(mReticleItemDistVector, 0, mReticuleItemDistMatrix, 0, mReticleItemDistVector, 0);
				
				//matrix to scale the Z-Offset of reticle items, so they appear above the map
				android.opengl.Matrix.setIdentityM(mZOffsetMatrix, 0);
				android.opengl.Matrix.scaleM(mZOffsetMatrix, 0, mReticuleScaleFactor, mReticuleScaleFactor, mReticuleScaleFactor);
				
				//calculate the proper Z-offset
				mCurrentZOffset[0] = mCurrentZOffset[1] = 0; 
				mCurrentZOffset[2] = Z_OFFSET_RET_ITEM;
				mCurrentZOffset[3] = 1f;
				android.opengl.Matrix.multiplyMV(mCurrentZOffset, 0, mZOffsetMatrix, 0, mCurrentZOffset, 0);
				
				// if this reticle item is a POI, load cached POI mesh from RenderSchemeManager
				// else, load a default mesh and apply a new texture
				if(reticleItem.isPOIItem()){
					reticleItem.mMesh = rsm.getPOIMesh(reticleItem.getPOIID(), 0, camera.getMaxZoomScale());
				}
				else {
					reticleItem.mMesh = rsm.getMeshOfTypeWithTexture(MeshType.RETICLE_ITEM.ordinal(), reticleItem.mImage);
				}
				android.opengl.Matrix.setIdentityM(reticleItem.mMesh.mModelMatrix, 0);
				android.opengl.Matrix.rotateM(reticleItem.mMesh.mModelMatrix, 0, camera.mCurrentRotationAngle, 0, 0, 1f);
				android.opengl.Matrix.translateM(reticleItem.mMesh.mModelMatrix, 0, mReticleItemDistVector[0], mReticleItemDistVector[1], mCurrentZOffset[2]); 
				android.opengl.Matrix.rotateM(reticleItem.mMesh.mModelMatrix, 0, -camera.mCurrentRotationAngle, 0, 0, 1f);
				android.opengl.Matrix.scaleM(reticleItem.mMesh.mModelMatrix, 0, mReticuleScaleFactor/5f, mReticuleScaleFactor/5f, mReticuleScaleFactor/5f);
				reticleItem.mMesh.enableAlphaTesting = true;
				reticleItem.mMesh.drawMesh(camera.mCamViewMatrix, camera.mCamProjMatrix);
			}
			
			if(thisIsTheFirstRun) thisIsTheFirstRun = false;
	}	
	
	private void DisplayResortName(Canvas canvas, ResortInfoResponse resort, CameraViewport camera, boolean showDistance, long displayTime, float timeFactor, int yOffset) {
		if(resort == null  ||  displayTime > (int)(3000 * timeFactor)) return;
		
		String resortName = resort.mName;
		int testLen = 40;
		if(showDistance) {
			testLen = 30;
		}
		if(resortName.length() > testLen + 3) {
			resortName = resortName.substring(0, testLen) + "...";
		}
		if(showDistance) {
			String distanceStr = "";
//			Log.d(TAG, "units are metric 2 = " + mUnitsAreMetric);

			if(mUnitsAreMetric) { 
				if(resort.mDistanceFromTargetPoint < 1.f) {
					resortName = resortName + ", " + ((int)(resort.mDistanceFromTargetPoint * 1000)) + "m";
				}
				else {
					resortName = resortName + ", " + ((int)resort.mDistanceFromTargetPoint) + "km";
				}
			}
			else {
				float distMiles = resort.mDistanceFromTargetPoint * 0.621371f;
		
				if(distMiles < 1.f) {
					resortName = resortName + ", " + ((int)(distMiles * 5280)) + "ft";
				}
				else {
					resortName = resortName + ", " + ((int)distMiles) + "Mi";
				}
			}
		}

		int newAlpha = 255;
		if(displayTime < 200*timeFactor) {
			newAlpha = (int)((float)displayTime/(200.f * timeFactor) * 255.f);
		}
		if(displayTime >= (int)(200 * timeFactor) && displayTime < (int)(1700 * timeFactor)) {
			newAlpha = 255;
		}
		if(displayTime >= (int)(1700 * timeFactor) && displayTime < (int)(2100 * timeFactor) ){
			newAlpha = (int)( ((2100.f * timeFactor) - (float)displayTime)/(400.f* timeFactor) * 255.f);
		}
		if(displayTime >= (int)(2100 * timeFactor)) {
			newAlpha = 0;
		}
		mClosestItemBoxPaint.setAlpha((int)(newAlpha * (float)mMaxNameBoxAlpha / 255.f));
		mClosestItemBoxOutlinePaint.setAlpha((int)(newAlpha * (float)mMaxNameBoxOutlineAlpha / 255.f));
		mClosestItemOutlinePaint.setAlpha(newAlpha);
		mResortLabelPaint.setAlpha(newAlpha);
		
		float xoffset = mClosestItemPaint.measureText(resortName)/2.f;
		mDescRect.left = (float)(camera.mScreenWidthInPixels/2-(xoffset*1.2));
		mDescRect.top = (float)(camera.mScreenHeightInPixels+yOffset-20-26);
		mDescRect.right = (float)(camera.mScreenWidthInPixels/2+(xoffset*1.2)); 
		mDescRect.bottom = (float)(camera.mScreenHeightInPixels+yOffset-20+15);
		canvas.drawRect(mDescRect, mClosestItemBoxPaint);
		canvas.drawRect(mDescRect, mClosestItemBoxOutlinePaint);
		canvas.drawText(resortName, camera.mScreenWidthInPixels/2, camera.mScreenHeightInPixels+yOffset-20, mClosestItemOutlinePaint);
		canvas.drawText(resortName, camera.mScreenWidthInPixels/2, camera.mScreenHeightInPixels+yOffset-20, mResortLabelPaint);
	}

	@Override
	public void SetGeodataServiceInterface(IGeodataService geodataServiceInterface) throws RemoteException {
		mGeodataServiceInterface = geodataServiceInterface;
	}

	@Override
	public boolean IsReady() {
//		return mResortList != null;
		return true;
	}
	
	@Override
	public boolean CheckForUpdates() {
		if(mEnabled) {
			boolean newData = mSnowReticleDrawingSet.SwitchIfUpdateReady();
			if (newData) Log.i(TAG, "Got new data");
			return newData;
		}
		return false;
	}

	public void SetResortNamingByPoint(boolean nameResortByPointingAtIt) {
		// TODO Auto-generated method stub
		mNameResortByPointingAtIt = nameResortByPointingAtIt;
	}

	@Override
	public boolean SetGeodataServiceState(GeoDataServiceState geoDataServiceState) {
		// TODO Auto-generated method stub
		mGeodataServiceState = geoDataServiceState;
 		
 		if(mGeodataServiceState!= null && mDrawingViewPortCenter[0] != 0.f && mResortList == null) {
 			GetResortList();
 		}
		return true;
	}
	
	@Override
	public void SetCameraPosition(CameraViewport camera) {		// when camera changes, recalc new drawing center
		if(!mUpdatingCamera) {
//			Log.d(TAG, "In SetCameraPosition and Updating Camera!");
			mUpdatingCamera = true;
			mCurCameraPosition = new PointXY((float)camera.mCurrentLongitude, (float)camera.mCurrentLatitude);

			PointXY drawingCameraViewportCenter = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(mCurCameraPosition);

			mDrawTransformMatrix.setTranslate(-drawingCameraViewportCenter.x,-drawingCameraViewportCenter.y);
			mDrawTransformMatrix.postScale(1.0f/camera.mCurrentAltitudeScale, 1.0f/camera.mCurrentAltitudeScale);
			mDrawTransformMatrix.postRotate(-camera.mCurrentRotationAngle);
			//		mDrawTransformMatrix.postRotate(-mCurrentRotationAngle);
			mDrawTransformMatrix.postTranslate(camera.mScreenWidthInPixels/2.0f, camera.mScreenHeightInPixels/2.0f);

			mDrawingViewPortCenter[0] = (float)drawingCameraViewportCenter.x;
			mDrawingViewPortCenter[1] = (float)drawingCameraViewportCenter.y;
//			mDrawTransformMatrix.mapPoints(mDrawingViewPortCenter);

			if(mResortList == null) {
				if(mGeodataServiceState!= null && mDrawingViewPortCenter[0] != 0.f) {
					GetResortList();
				}
			}
			else {
				//	 		if(mLastCameraPosition != null && CalcDistanceInKmBetween(mLastCameraPosition, curCameraPosition) < 0.02 ) {
				// 				return;	// don't update unless moved at least 20m
				// 			}
				mLastCameraPosition = mCurCameraPosition;

				sortedResortInfo.clear();
				boolean resortsOnScreen = false;
				boolean inAtLeastOneResort = false;
				//			Log.d(TAG, "Resort list size " + mResortList.size());
				for(ResortInfoResponse resortInfo : mResortList) {					// then load data from files into cache...  noting that Transcoders generally ask for more data (ie, larger GR) than geodata service clients actually request (so don't need to add extra here)
					resortInfo.mDistanceFromTargetPoint = CalcDistanceInKmBetween(resortInfo.mLocation, mCurCameraPosition);
					if(resortInfo.mGPSBoundaryBox.Contains(mCurCameraPosition.x, mCurCameraPosition.y)) {
						resortInfo.mTargetPointWithinResortBoundingBox = true;
						inAtLeastOneResort = true;
					}
					else {
						resortInfo.mTargetPointWithinResortBoundingBox = false;
					}
					sortedResortInfo.put(resortInfo.mDistanceFromTargetPoint, resortInfo);  // as TreeMap, keys will automatically be in sorted distance order
				}

				ResortInfoResponse lastCurrentResort = mCurrentResort;
				mCurrentResort = null;
				float maxDistance = MAX_DISPLAY_DISTANCE_TO_RESORTS;
				if(inAtLeastOneResort) {
					//				maxDistance = maxDistance = MAX_DISTANCE_TO_RESORTS_IF_IN_RESORT;
					// find closest resort where the user is within it's BB
					for(Float distance : sortedResortInfo.keySet()) {
						ResortInfoResponse resort = sortedResortInfo.get(distance);
						if(resort.mTargetPointWithinResortBoundingBox) {
							mCurrentResort = resort;		// mark closest resort that user is within BB as current resort
							//						Log.e(TAG,  "lastCurrentResort resort " + mCurrentResort.mName) ;
							break;
						}
					}
				}
				mSnowReticleDrawingSet.ClearResortReticleItems();
				if(!inAtLeastOneResort || camera.IsLowestZoom() ) { 
					int cnt = 0;
					for(Float distance : sortedResortInfo.keySet()) {
						//				Log.d(TAG, "Sorted Resort distance " + distance + ", " + maxDistance);
						if(distance < maxDistance) { 
							ResortInfoResponse resort = sortedResortInfo.get(distance);
							if(mCurrentResort == null || !resort.mName.equalsIgnoreCase(mCurrentResort.mName)) {
								mSnowReticleDrawingSet.AddResortReticleItem(resort);
							}
							if(++cnt >= MAX_RESORTS_ON_RETICLE) {	// limit resort icons to max
								break;
							}
						}
						else {
							break;
						}
					}
				}
				mSnowReticleDrawingSet.CheckNewSetReady();

				//			Log.e(TAG, "Resort reticle icons " + mResortReticleItems.size());
				//			for(ResortInfoResponse resortInfo : mResortReticleItems) {
				//				Log.e(TAG, "Resort reticle icons for: " + resortInfo.mName + ", " + resortInfo.mLocation.x + ", " + resortInfo.mLocation.y);
				//			}

			}
			mUpdatingCamera = false;
		}
		
	}
	
	@Override
	public void SetCameraHeading(float heading) {
//		Log.e(TAG, "SetCameraHeading " + heading);
//		mCurrentRotationAngle = heading;
	}
	
	
	private void GetResortList() {
//		if(!mLoadingResorts) {			// removed as not functional for tile-base snow data
//			mLoadingResorts = true;
//			GetResortListThread newRLLoadThread = new GetResortListThread();
//			newRLLoadThread.start();
//		}
	}
	
	private class GetResortListThread extends Thread {

		@Override
	    public void run() {
	 		IGeodataServiceResponse response;
			ResortRequest resortRequest = new ResortRequest(mCurCameraPosition, MAX_DISTANCE_TO_RESORTS, MAX_NUMBER_OF_RESORTS, DataSources.MD_SKI);
			try {
				response = mGeodataServiceInterface.getClosestResorts(resortRequest);
				if(response != null && response.mResortsArray != null) {
					mResortList = response.mResortsArray;
//					for(ResortInfoResponse resortInfo : mResortList) {
//						Log.e(TAG, "Resort info: " + resortInfo.mName);
//					}
				}
			} catch (RemoteException e) {
				Log.e(TAG, "Error accessing GeodataService getClosestResorts. Message: " + e.getMessage());
				mResortList = null;
			}
			mLoadingResorts = false;
	    }
	}

	public float CalcDistanceInKmBetween(PointXY point1, PointXY point2) {
		float meridionalCircumference = 40007860.0f; // m  - taken from wikipedia/Earth
		float equitorialCircumference = 40075017.0f; // m  - taken from wikipedia/Earth
//		Log.i(TAG,"CalcDistance: " + point1.x + ", " + point1.y + ", "+ point2.x + ", " + point2.y);
		float hAngleDiff = Math.abs(point1.x - point2.x);
		if(hAngleDiff > 180.f) hAngleDiff = 360.f - hAngleDiff;	
		
		float hDistInKm = (hAngleDiff / 360.f * (float)(equitorialCircumference*Math.cos(Math.toRadians((point1.y + point2.y)/2.f))))/1000.f; 
		float vDistInKm = ((point1.y - point2.y) / 360.f * meridionalCircumference)/1000.f; 
		return (float) Math.sqrt(hDistInKm*hDistInKm + vDistInKm*vDistInKm) ; 
	}

	public void setMapDrawingSetCallback(MapRLDrawingSet mapDrawingSet){
		mMapLayerDrawingSet = mapDrawingSet;
	}
	
	public void setUserIconLayerCallback(UserIconLayer userIconLayer){
		mUserIconLayer = userIconLayer;
	}

}
