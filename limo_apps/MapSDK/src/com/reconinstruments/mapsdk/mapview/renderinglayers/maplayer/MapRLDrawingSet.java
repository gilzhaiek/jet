package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.util.Log;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WorldObject;
import com.reconinstruments.mapsdk.mapview.MapView.MapViewMode;
import com.reconinstruments.mapsdk.mapview.WO_drawings.NoDataZoneDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.WO_drawings.TerrainDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.TrailDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.renderinglayers.DrawingSet;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer.CustomLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition.MapCompositionItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition.MapCompositionNameItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition.MapCompositionObjectItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;

public class MapRLDrawingSet extends DrawingSet {
// constants
	private final static String 	TAG = "MapRLDrawingSet";
	private final static String RECON_FAKE_GPS_PROVIDER = "FakeGPS";
	private final static boolean INSTANT_VIEW_CHANGE = true;
//	private final Semaphore 		mAccessToWaitingForTilesArray = new Semaphore(1, true);
//	public final Semaphore 			mAccessToDrawingSetLoadDefinition = new Semaphore(1, true);
	private static boolean THIRD_PERSON_VIEW = false;

// members
			CustomLayer 			mAnnotationLayer;
			CameraViewport			mCameraViewport;
	public  String 					mMapCompositionID;					// set by map layer parent - defines which data types to load from which data sources
	public  World2DrawingTransformer	mWorld2DrawingTransformer = null;
	
	// data - all data is stored in paired buffers: current and alternate.  The value of super.mCurIndex defines the current set, and alternate is !current set
	public  boolean[]				mEmptyTileSet = new boolean[2];		// info regarding next GeoRegion to load
	public  float[]					mLoadedHeading = new float[2];		// info regarding next GeoRegion to load
	public  float[]					mLoadedScale = new float[2];
	public  GeoRegion[] 			mLoadedGeoRegion = new GeoRegion[2];						// used to draw data at current zoom scale
	public  GeoRegion[] 			mLoadedLowestResolutionGeoRegion = new GeoRegion[2];		// used to load data from geodata service at lowest resolution - speeds up scale redrawing
//	public	String[]				mDataLoadCallbackTicketNum = new String[2];

	public  int						mImageSize = 0;						// background bitmap 
	public 	Canvas[] 				mBackgroundCanvas = new Canvas[2];
	public 	Bitmap[]				mBackgroundImage = new Bitmap[2];	
	private boolean     			mShowTrailNames = true;

	public 	ArrayList<POIDrawing>	mPOI0 = new ArrayList<POIDrawing>(); // POI data
	public 	ArrayList<POIDrawing>	mPOI1 = new ArrayList<POIDrawing>(); 

//	public  int						mCurrentLoadingTileArrayIndex = 0;
//	public	boolean					mChangeLoadingTileArray = false;
//	public 	ArrayList<GeoRegion>	mLoadingTile0 = new ArrayList<GeoRegion>(); // loading tile regions
//	public 	ArrayList<GeoRegion>	mLoadingTile1 = new ArrayList<GeoRegion>(); 
//
//	public 	float[]					mLatitude = new float[2];			// user position information
//	public 	float[]					mLongitude = new float[2];	
//	public 	float[]					mHeading = new float[2];		

			Map2DLayer 				mParent = null;
	
	
	// predefined classes to speed up rendering
			Paint					mLoadingAreaPaint = null;
			Paint					mClearAreaPaint = null;
			
			PointXY					onDrawCameraViewportCurrentCenter = new PointXY(0.f, 0.f);
			PointXY 				mDrawingCameraViewportCenter =  new PointXY(0.f, 0.f);
			RectF 					mDrawingCameraViewportBoundary = new RectF(); 
			RectF 					mDrawingLoadingArea = new RectF(); 
	public  PointXY					mBackgroundImageCenterInDrawingCoords =  new PointXY(0.f, 0.f);
	public 	RectF					mRegionBoundsDrawingCoords = new RectF();
	public 	RectF					mRegionTestBoundsInGPS = new RectF();
//			Paint 					mResortBBPaint = new Paint();
//			Matrix 					mLoadTransform = new Matrix();
			Matrix 					mRotateOffsetTransform = new Matrix();
			Matrix 					mBGDrawTransform = new Matrix();
			Matrix 					mPOIDrawTransform = new Matrix();
//			PointXY 				mRegionCenter = new PointXY(0.f,0.f);
			float[] 				mOffset = new float[2];
			Bitmap 					mResortReticuleIcon = null;

			private PointXY 				onDrawCameraAltitude = new PointXY(0f, 0f);
			
			CollisionDetector		mCollisionDetector = null;

			
	private Context mContext;
	
	/*private int mPositionHandle, mNormalHandle, mTextureHandle;
	private int mColorUniformHandle, mModelMatrixHandle, mViewMatrixHandle, mProjMatrixHandle;*/
	private float[] colorArray = {0.3f, 1.0f, 0.4f, 1.0f};
	
	private float[] mapModelMatrix;
	
	private float aspectRatio = 428.0f/240.0f;
	
	private double mDeltaLatitude = 0f, mDeltaLongtitude = 0f;
	
	private Location mCurrentLocGPS, mCurrentLocNet;
	private LocationManager mLocationMan;
	private float mCurrentUserSpeed = 0f, mPrevUserSpeed = -999f;
	private float mCurrentAltScale = 0f, mPrevAltScale = -999f;
	
	public boolean thisIsTheFirstRun = true;
	private Bitmap mMapBitmap, mUserIconBitmap;
	private BitmapFactory.Options bmpOptions;
	private float mCounter = 0;
	private float DEG2RAD = 0.0174533f;
	private float RAD2DEG = 57.29578f;
	private float mSign = 0f;
	private float initialAltitude = 2.0f;

	private float[] mCameraPos, textToCamVec, camLookAtVec;
	private float[] mMapPanOffset, mPOIOffset;
	
	/**
	 * The angle in degrees between the normal of the map and {@link #MAX_CAM_ANGLE},
	 * to be used when transforming the camera as a function 
	 * of the user's speed. {@link #MIN_CAM_ANGLE} can be set to limit
	 * the range of the angle change.
	 */
	public static volatile float mCamCurrentPitchAngle = 0f;
	
	private float mCamTargetAngle = 0f;
	
	private float mCamAngleDelta = 0f;
	
	private boolean mUpdateAngle = false;
	
	private boolean mVelocityBasedCamera = true;
	
	/**
	 * This constant is the smallest allowable change in angle 
	 * in the camera. 
	 */
	private static final float DELTA_STEP = 1.5f;
	
	/**
	 * This constant is the maximum angle in degrees allowed for
	 * {@link #mCamCurrentPitchAngle}
	 */
	private static final float MAX_CAM_ANGLE = 75f;
			
	/**
	 * This constant is the minimum angle in degrees allowed for 
	 * {@link #mCamCurrentPitchAngle}. At zero, the range of
	 * angle change will be between the normal of the map and 
	 * {@link #MAX_CAM_ANGLE}.
	 */
	private static final float MIN_CAM_ANGLE = 0f;
	
	/**
	 * This constant is the minimum cutoff point for changing
	 * the camera angle. If user speed is at or below this 
	 * point, {@link #mCamCurrentPitchAngle} will be at 
	 * {@code MIN_CAM_ANGLE}. 
	 */
	private static final float MIN_VELOCITY_CUTOFF = 0.05f;
	
	/**
	 * This constant is the cutoff point for changing the camera
	 * angle. When {@link #mCurrentUserSpeed} is at or above 
	 * {@link #MAX_VELOCITY_CUTOFF}, {@link #mCamCurrentPitchAngle} 
	 * will be at its maximum value. <br><br>So, when the user is 
	 * travelling fast enough, the camera angle will look like
	 * it's being thrown back from how fast the user is moving.
	 */
	private static final float MAX_VELOCITY_CUTOFF = 30f;
	
	private static float MAP_SCALE_FACTOR;
	
	private float mCurrentCameraAltitude = 2.0f;
	
	private float mMaxCameraAltitude;
	
	private float mAltitudeScaleFactor = 1f;
	
	private float zNear = 0f;
	
	private float mAdjustedMapScale = 1f, mUserIconScaleFactor = 1f;
	
	private float counter = 0;
	
	/**
	 * A queue of booleans that stores one boolean for every iteration 
	 * of the draw method where {@code loadNewTexture} is true.
	 */
	private ArrayDeque<Boolean> mTextureLoads;
	
// creator	/ init / release	
	public MapRLDrawingSet(Map2DLayer parent, RenderSchemeManager rsm, Context context, boolean velocityBased) {
		super(rsm);
		mVelocityBasedCamera = velocityBased;
		Log.d(TAG, "Constructor: VelocityBasedCamera = " + velocityBased);
		mParent = parent;
		
		mContext = context;
		
		mLoadingAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
		mLoadingAreaPaint.setStyle(Style.FILL) ;
		mLoadingAreaPaint.setColor(rsm.GetPanRolloverBoxBGColor());		
		mLoadingAreaPaint.setAlpha(rsm.GetPanRolloverBoxBGAlpha());
		mLoadingAreaPaint.setAntiAlias(true);

		mClearAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);		// set up paint for onDraw
		mClearAreaPaint.setStyle(Style.FILL) ;
		mClearAreaPaint.setColor(rsm.GetPanRolloverBoxBGColor());		
		mClearAreaPaint.setAlpha(0);
		mClearAreaPaint.setAntiAlias(true);
		
		mResortReticuleIcon = BitmapFactory.decodeResource(parent.mParentActivity.getResources(), R.drawable.resort_reticule_icon);

		mCollisionDetector = new CollisionDetector();

		bmpOptions = new BitmapFactory.Options();
	}
	
	
	public boolean Init(World2DrawingTransformer world2DrawingTransformer, int imageSize, CustomLayer annotationLayer) {
		mWorld2DrawingTransformer = world2DrawingTransformer;
		mAnnotationLayer = annotationLayer;
		Log.d(TAG, ">>>>>>>> Map scale is: " + imageSize);
		for(int i=0; i<2; i++) {
			mLoadState[i] = DrawingSetLoadState.INACTIVE;
			mLoadedScale[i] = 0.f;
			mLoadedHeading[i] = 0.f;
			mEmptyTileSet[i] = false;
			mLoadedGeoRegion[i] = new GeoRegion();
			mLoadedLowestResolutionGeoRegion[i] = new GeoRegion();
//			mDataLoadCallbackTicketNum[i] = ""; 
		}
		
		mapModelMatrix = new float[16];
		mCameraPos = new float[3];
		textToCamVec = new float[3];
		camLookAtVec = new float[3];
		mMapPanOffset = new float[4];
		mPOIOffset = new float[4];
		
		mLocationMan = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

		mImageSize = imageSize;
		Log.d(TAG, ">|>|>|> ImageSize = " + mImageSize);
		try {
			Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
			for(int i=0; i<2; i++) {
				if(mBackgroundImage[i] == null || mBackgroundCanvas[i] == null) {
					mBackgroundImage[i] = Bitmap.createBitmap(imageSize, imageSize, conf); // this creates a MUTABLE bitmap
					mBackgroundCanvas[i] = new Canvas(mBackgroundImage[i]);
				}
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Failed to initialize DrawingSet background images and canvases.");
			ReleaseImageResources();
			return false; // fail
		}
		return true;

	}
	
	public void ResetDrawingState() {
		mIgnoreCurrentLoad = true;
		mUpdateAvailable = false;
		for(int i=0; i<2; i++) {
			mLoadState[i] = DrawingSetLoadState.INACTIVE;	// ignore previously loaded data
		}
	}
	
	public void release() {
		ReleaseImageResources();
	}

	public void ReleaseImageResources() {
		for(int i=0; i<2; i++) {
			if(mBackgroundImage[i] != null) mBackgroundImage[i].recycle();
			mBackgroundImage[i] = null;
			mBackgroundCanvas[i] = null;
		}
		mImageSize = 0;
	}
	
// Overridden methods
	@Override
	public void CancelLoad(boolean cancelLoad) {
		super.CancelLoad(cancelLoad);
	}
	//Opengl rendering
	@SuppressLint("NewApi")
	public void Draw(RenderSchemeManager rsm, Resources res, CameraViewport camera, String focusedObjectID, boolean loadNewTexture, MapViewMode viewMode, GLText glText) {		// for OpenGL based rendering
//		if(loadNewTexture)	Log.d("MapRLDrawingSet", "onDraw() called! loadNewTexture = " + String.valueOf(loadNewTexture));
//		Log.d(TAG, "update available: " + mUpdateAvailable + "  >>>>>>>  Current Load state: " + mLoadState[mCurIndex].toString());
		onDrawCameraViewportCurrentCenter.x = (float)camera.mCurrentLongitude;
		onDrawCameraViewportCurrentCenter.y = (float)camera.mCurrentLatitude;
		mDrawingCameraViewportCenter = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(onDrawCameraViewportCurrentCenter);
		mDrawingCameraViewportBoundary = mWorld2DrawingTransformer.TransformGPSRectXYToRectF(camera.mGeoRegionToSupportCurrentView.mBoundingBox);
		
		mBackgroundImageCenterInDrawingCoords = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(mLoadedGeoRegion[mCurIndex].mCenterPoint);

		mMaxCameraAltitude = camera.getMaxZoomScale() * (mImageSize / 8);
		float viewScale = mCurrentCameraAltitude = camera.mCurrentAltitudeScale;
		float rotateAngle = 0;
		mCurrentUserSpeed = (float) camera.mCurrentVelocity;				// added by STEVE for testing
		
		if(camera.mCameraPitchesWithUser) {
			rotateAngle = camera.mCurrentRotationAngle;
			float horizonUserAngle = +30;
			if(camera.mPitchOffset > horizonUserAngle) {
				mCamCurrentPitchAngle = (float) camera.mPitchOffset + 90.f - horizonUserAngle;
			}
			else {
				float lowerHemisphereScale = (float) ((90.f - camera.mCurrentPitch)/(45.f + horizonUserAngle));
				float offset = -lowerHemisphereScale * horizonUserAngle;
				mCamCurrentPitchAngle = (float) Math.max((90.f + ((camera.mPitchOffset * lowerHemisphereScale) + offset)), 0.f);
//				Log.e(TAG,"DrawingSet mPitchOffset: " + camera.mCurrentPitch + ", " + camera.mPitchOffset + ", " + offset+ ", " + lowerHemisphereScale );
			}
		}
		else {
			mCamCurrentPitchAngle = 0.f;
			camera.mCurrentRotationAngle = (viewMode == MapViewMode.FIND_MODE) ? 0f : camera.mCurrentRotationAngle;
            rotateAngle = camera.mCurrentRotationAngle;
        }
		mAltitudeScaleFactor = GetLoadedScale() / camera.mCurrentAltitudeScale;
		
		MeshGL mapMesh = rsm.getMeshOfType(MeshGL.MeshType.MAP.ordinal());

		//HACK: if mCamCurrentPitchAngle is greater than 90deg, the cos()
		// function will be negative, flipping the Z-axis in setLookAt(),
		// so we'll flip the sign just for this case
		float sign = (mCamCurrentPitchAngle > 90f) ? -1f : 1f;
		
		//Zoom-mode or Velocity-mode camera
		if(THIRD_PERSON_VIEW){
			if(mCamCurrentPitchAngle >= MIN_CAM_ANGLE && mCamCurrentPitchAngle <= MAX_CAM_ANGLE) ;
			else if(mCamCurrentPitchAngle < MIN_CAM_ANGLE) mCamCurrentPitchAngle =  MIN_CAM_ANGLE;
			else mCamCurrentPitchAngle =  MAX_CAM_ANGLE;
			android.opengl.Matrix.setLookAtM(camera.mCamViewMatrix, 0, 
					mCameraPos[0] = 0f, //eye x position
					mCameraPos[1] = -(float)(mMaxCameraAltitude*Math.sin(mCamCurrentPitchAngle * DEG2RAD)), //eye y 
					mCameraPos[2] = (float)(mMaxCameraAltitude * Math.cos(mCamCurrentPitchAngle * DEG2RAD)), //eye z
					0f, //lookat x
					0f,	//lookat y
					0f,	//lookat z
					0.0f, 1f, 0.0f); //up vector
		}
		else {
			android.opengl.Matrix.setLookAtM(camera.mCamViewMatrix, 0, 
					mCameraPos[0] = 0f, //eye x position
					mCameraPos[1] = 0f, //eye y position
					mCameraPos[2] = mMaxCameraAltitude,// * mImageSize/4f, //eye z position 
					0f, 							//lookat x
					(float)(mMaxCameraAltitude*Math.sin(mCamCurrentPitchAngle * DEG2RAD)), //lookat y 
					(float)(mMaxCameraAltitude * (1.f- Math.cos(mCamCurrentPitchAngle * DEG2RAD))), //lookat z
					0.0f, sign, 0.0f); //up vector
		}

		camLookAtVec[0] = 0f - mCameraPos[0];
		camLookAtVec[1] = (float)(mMaxCameraAltitude*Math.sin(((!mVelocityBasedCamera) ? mCamCurrentPitchAngle : mCurrentUserSpeed) * DEG2RAD)) - mCameraPos[1];
		camLookAtVec[2] = 0f - mCameraPos[2];

		//calculate map offset due to panning
		mMapPanOffset[0] = (float)(mBackgroundImageCenterInDrawingCoords.x-mDrawingCameraViewportCenter.x)/viewScale;
		mMapPanOffset[1] = -(float)(mBackgroundImageCenterInDrawingCoords.y-mDrawingCameraViewportCenter.y)/viewScale;
		mMapPanOffset[2] = 0f;
		mMapPanOffset[3] = 0;
		
		//calculate POI offset due to panning
		mPOIOffset[0] = (float)(-mDrawingCameraViewportCenter.x)/viewScale;
		mPOIOffset[1] = (float)(mDrawingCameraViewportCenter.y)/viewScale;
		mPOIOffset[2] = 0;
		mPOIOffset[3] = 0;
		
		//Transforming the Map offset vector and POI offset Vector.
		//Used when panning the camera in Explore mode.
		android.opengl.Matrix.setIdentityM(mapModelMatrix, 0);
		android.opengl.Matrix.rotateM(mapModelMatrix, 0, rotateAngle,  0f, 0f, 1f);
		android.opengl.Matrix.scaleM(mapModelMatrix, 0, 2, 2, 2);
		android.opengl.Matrix.multiplyMV(mMapPanOffset, 0, mapModelMatrix, 0, mMapPanOffset, 0);
		android.opengl.Matrix.multiplyMV(mPOIOffset, 0, mapModelMatrix, 0, mPOIOffset, 0);
		
		mAdjustedMapScale = mAltitudeScaleFactor * mImageSize * 2f;
		android.opengl.Matrix.setIdentityM(mapMesh.mModelMatrix, 0);
		android.opengl.Matrix.translateM(mapMesh.mModelMatrix, 0, mMapPanOffset[0], 
																  mMapPanOffset[1], 
																  mMapPanOffset[2]);
		android.opengl.Matrix.rotateM(mapMesh.mModelMatrix, 0, rotateAngle, 0f, 0f, 1f);
		android.opengl.Matrix.scaleM(mapMesh.mModelMatrix, 0, mAdjustedMapScale, mAdjustedMapScale, mAdjustedMapScale);
		
		if(loadNewTexture || thisIsTheFirstRun){
			mMapBitmap = mBackgroundImage[mCurIndex];
			mapMesh.loadMeshTexture(mMapBitmap, true);
		}
		mapMesh.enableAlphaTesting = false;
		mapMesh.enableRadialGradient = false;
		mapMesh.radialGradientStart = 1.75f;
		mapMesh.radialGradientEnd = 2f;
		mapMesh.drawMesh(camera.mCamViewMatrix, camera.mCamProjMatrix);
		
		/*glText.setCameraProps(mCameraPos, camLookAtVec, rotateAngle, camera.mCurrentAltitudeScale);
		glText.begin(1f, 1f, 1f, 1f, camera.mCamViewMatrix, camera.mCamProjMatrix, camera.getZNear());
		glText.drawC("Chocolate chip, oatmeal, peanut butter too", 0f, 0.8f, 0.1f, 0, 0, 0);
		glText.end();*/
		
		android.opengl.Matrix.setIdentityM(mapModelMatrix, 0);
		android.opengl.Matrix.translateM(mapModelMatrix, 0, 0, camera.mScreenHeightInPixels, 0);
		android.opengl.Matrix.scaleM(mapModelMatrix, 0, 2/viewScale, 2/viewScale, 2/viewScale);
		POIDrawing focusedPOI = null;
		//for each POI, render if not frustum-culled
		for (POIDrawing poiDrawing : GetCurrentPOI()) {

			// if not frustum-culled
			if(mDrawingCameraViewportBoundary.contains(poiDrawing.mLocation.x, poiDrawing.mLocation.y)) {
				if (poiDrawing.mDataObject.mObjectID.equalsIgnoreCase(focusedObjectID)) {
					focusedPOI = poiDrawing;
				} else {
					poiDrawing.Draw(mRSM, mPOIDrawTransform, camera, mCamCurrentPitchAngle, mapModelMatrix, mPOIOffset, mImageSize, false, loadNewTexture);
				}
			}
		}
		if(focusedPOI != null){
			focusedPOI.Draw(mRSM,mPOIDrawTransform, camera, mCamCurrentPitchAngle, mapModelMatrix, mPOIOffset, mImageSize, true, loadNewTexture);
		}
		if(thisIsTheFirstRun) thisIsTheFirstRun = false;
	}

	// TODO get rid of excess (old) parameters
	public void Draw(Canvas canvas, CameraViewport cameraViewport, String focusedObjectID, Resources res) {
		// draw background first
		
		if(!mInitialized) return;		// shouldn't happen but inserted because it is happening in debug
		
		float viewScale = cameraViewport.GetAltitudeScale();	

		// set several Drawing coordinate values CameraViewportCenter, CameraViewportBoundary, a test boundary and UserPosition
		onDrawCameraViewportCurrentCenter.x = (float)cameraViewport.mCurrentLongitude;
		onDrawCameraViewportCurrentCenter.y = (float)cameraViewport.mCurrentLatitude;
		mDrawingCameraViewportCenter = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(onDrawCameraViewportCurrentCenter);
//		Log.d(TAG, "mDrawingCameraViewportCenter = " + mDrawingCameraViewportCenter.x + ", " + mDrawingCameraViewportCenter.y);
		mDrawingCameraViewportBoundary = mWorld2DrawingTransformer.TransformGPSRectXYToRectF(cameraViewport.mGeoRegionToSupportCurrentView.mBoundingBox);
		
		mBackgroundImageCenterInDrawingCoords = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(mLoadedGeoRegion[mCurIndex].mCenterPoint);
		
		Log.d(TAG, "BackgroundImageCenter = (" + mBackgroundImageCenterInDrawingCoords.x + ", " + mBackgroundImageCenterInDrawingCoords.y + ")  |||||"
				+ "  DrawingCameraViewportCenter =  (" + mDrawingCameraViewportCenter.x + ", " + mDrawingCameraViewportCenter.y + ") ");
		
		float borderScale = 1.2f;		// provide border that's slightly larger to handle rotation and POIs on border
//		RectF mDrawingCameraViewportTestBoundary = new RectF(mDrawingCameraViewportBoundary.left / borderScale, mDrawingCameraViewportBoundary.top / borderScale, mDrawingCameraViewportBoundary.right * borderScale, mDrawingCameraViewportBoundary.bottom * borderScale);

		float loadedScale = GetLoadedScale();
		
		mBGDrawTransform.reset();
		mBGDrawTransform.setTranslate(-mImageSize/2.f,-mImageSize/2.f);
		mBGDrawTransform.postScale(loadedScale/viewScale,loadedScale/viewScale);
		mBGDrawTransform.postRotate(-cameraViewport.mCurrentRotationAngle);
		mRotateOffsetTransform.setRotate(-cameraViewport.mCurrentRotationAngle);
		mOffset[0] = (float)(mBackgroundImageCenterInDrawingCoords.x-mDrawingCameraViewportCenter.x)/viewScale;
		mOffset[1] = (float)(mBackgroundImageCenterInDrawingCoords.y-mDrawingCameraViewportCenter.y)/viewScale;
		mRotateOffsetTransform.mapPoints(mOffset);
		mBGDrawTransform.postTranslate(mOffset[0] + cameraViewport.mScreenWidthInPixels/2.0f, mOffset[1] + cameraViewport.mScreenHeightInPixels/2.0f);
		
		
		mPOIDrawTransform.setTranslate(-mDrawingCameraViewportCenter.x,-mDrawingCameraViewportCenter.y);
		mPOIDrawTransform.postScale(1.0f/viewScale,1.0f/viewScale);  
		mPOIDrawTransform.postRotate(-cameraViewport.mCurrentRotationAngle);
		mPOIDrawTransform.postTranslate(cameraViewport.mScreenWidthInPixels/2.0f, cameraViewport.mScreenHeightInPixels/2.0f);

		
		// then draw existing bitmap on top
		canvas.drawBitmap(mBackgroundImage[mCurIndex], mBGDrawTransform, null);  
	

		// then overlay POIs that are within viewport (oriented to face user)
		POIDrawing focusedPOI = null; 
		
		for(POIDrawing poiDrawing : GetCurrentPOI()) {
           if(poiDrawing.mDataObject.mObjectID.equalsIgnoreCase(focusedObjectID)) {
        	   focusedPOI = poiDrawing;
           }
           else {
        	   poiDrawing.Draw(canvas, res, mRSM, mPOIDrawTransform, cameraViewport.mCurrentAltitudeScale, false, res);
           }
		}
		if(focusedPOI != null) {
			focusedPOI.Draw(canvas, res, mRSM, mPOIDrawTransform, cameraViewport.mCurrentAltitudeScale, true, res);  // draw focused item last so it's on top
		}

	}

// reticle support
	public ArrayList<ReticleItem> GetReticleItems(CameraViewport camera, float withinDistInM) {	// assumes this is called after Draw
		
		ArrayList<ReticleItem> itemList = new ArrayList<ReticleItem>();
//		float margin = RETICULE_DISTANCE;
//		mDrawingResortTestBoundary.left =   (float)(mLocationTransformer.TransformLongitude(mBestMapDrawing.mResortInfo.BoundingBox.top, mBestMapDrawing.mResortInfo.BoundingBox.left)) - RETICULE_DISTANCE/viewScale;
//		mDrawingResortTestBoundary.right =  (float)(mLocationTransformer.TransformLongitude(mBestMapDrawing.mResortInfo.BoundingBox.top, mBestMapDrawing.mResortInfo.BoundingBox.right)) + RETICULE_DISTANCE/viewScale;
//		mDrawingResortTestBoundary.bottom = (float)mMaxDrawingY - (float)(mLocationTransformer.TransformLatitude(mBestMapDrawing.mResortInfo.BoundingBox.top, mBestMapDrawing.mResortInfo.BoundingBox.left)) + RETICULE_DISTANCE/viewScale;   // top and bottom flipped as GPS are positive up and graphics is positive down
//		mDrawingResortTestBoundary.top =    (float)mMaxDrawingY - (float)(mLocationTransformer.TransformLatitude(mBestMapDrawing.mResortInfo.BoundingBox.bottom, mBestMapDrawing.mResortInfo.BoundingBox.left)) - RETICULE_DISTANCE/viewScale;
//
//		
//		if(camera != null && !mDrawingResortTestBoundary.contains((float)mDrawingCameraViewportCenter.x, (float)mDrawingCameraViewportCenter.y)) {
//			itemList.add(new ReticleItem(mResortReticuleIcon, mDrawingUserPosition));  				// add user icon to reticle if user offscreen
//		}
		return itemList;
	}

	
// methods unique to DS subclass
// set value methods
	public void DefineNextMapToLoad(GeoRegion nextGeoRegion, GeoRegion nextLowestResolutionGeoRegion, float nextScale, float nextHeading, String mapCompositionID) {
		SetNextGeoRegion(nextGeoRegion);
		SetNextLowestResolutionGeoRegion(nextLowestResolutionGeoRegion);
		SetNextScale(nextScale);
		SetNextHeading(nextHeading);
//		SetNextTicketNumber("");
		mMapCompositionID = mapCompositionID;
		SetLoadingIndicator(true);  // mark alternate set as "loading"
	}
	
//	public void SetNextTicketNumber(String nextTicketNum) {
//		mDataLoadCallbackTicketNum[ = nextTicketNum;
//	}
//	public void ClaimCallbackTicket(String ticketNum) {
//		SetNextTicketNumber("");
//	}

	public boolean allMissingTiles() {
		return (mLoadState[mCurIndex] == DrawingSetLoadState.LOADED && mEmptyTileSet[mCurIndex]);
	}

	public void AddPOIObjects(TreeMap<Integer, ArrayList<POIDrawing>> poiDrawingObjectArrays, ArrayList<MapCompositionItem> mapComposition, float scale) {	// always add objects to non-current array, then flag for update
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		if(nextIndex == 0) {
			mPOI0.clear();
			for(Integer index : poiDrawingObjectArrays.keySet()) {
				if(mCancelLoad){ 
					Log.d(TAG,"Canceling adding POI objects!");
					return;
				}
				MapCompositionItem mcItem = mapComposition.get(index);
				if(mcItem.mObjectClass == WorldObjectDrawing.WO_Class.POI && mcItem.mDrawingConstraint.IncludeInDrawing(scale)) {	// if the drawing constraints for this item type are satisfied
					mPOI0.addAll(poiDrawingObjectArrays.get(index));
				}
			}
		}
		else {
			mPOI1.clear();
			for(Integer index : poiDrawingObjectArrays.keySet()) {
				if(mCancelLoad) {
					Log.d(TAG,"Canceling adding POI objects!");
					return;
				}
				MapCompositionItem mcItem = mapComposition.get(index);
				if(mcItem.mObjectClass == WorldObjectDrawing.WO_Class.POI && mcItem.mDrawingConstraint.IncludeInDrawing(scale)) {	// if the drawing constraints for this item type are satisfied
					mPOI1.addAll(poiDrawingObjectArrays.get(index));
				}
			}
		}
	}

	public int ImageSize() {
		return mImageSize;
	}

	public void ShowTrailNames(boolean showTrailNames) {
		mShowTrailNames = showTrailNames;
	}

//	public void SetUserLocation(float newLatitude, float newLongitude) {
//		int nextIndex = (mCurIndex == 0) ? 1 : 0;
//		mLatitude[nextIndex] = newLatitude;
//		mLongitude[nextIndex] = newLongitude;
//	}
//
//	public void SetUserHeading(float userHeading) {
//		int nextIndex = (mCurIndex == 0) ? 1 : 0;
//		mHeading[nextIndex] = userHeading;
//	}
//
	public void SetNextGeoRegion(GeoRegion nextGR) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadedGeoRegion[nextIndex] = new GeoRegion(nextGR);
	}
	
	public void SetNextLowestResolutionGeoRegion(GeoRegion nextGRLR) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadedLowestResolutionGeoRegion[nextIndex] = new GeoRegion(nextGRLR);
	}
	
	public void SetNextScale(float nextScale) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadedScale[nextIndex] = nextScale;
	}
	
	public void SetNextHeading(float nextHeading) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadedHeading[nextIndex] = nextHeading;
	}
	
	public void SetNextState(DrawingSetLoadState newState) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		mLoadState[nextIndex] = newState;
	}
	
	public void setVelocityBasedCamera(boolean velocityBasedCamera){
		mVelocityBasedCamera = velocityBasedCamera;
	}
	
	/**
	 * Sets a third person view of the map, centering around
	 * the User Icon arrow. In this mode, user icon arrow is always
	 * visible
	 * @param setThirdPersonView
	 */
	public void setThirdPersonView(boolean setThirdPersonView){
		THIRD_PERSON_VIEW = setThirdPersonView;
	}
	
	
	@Override
	public boolean SwitchIfUpdateReady() {
//		Log.e(TAG, "SwitchIfUpdateReady: " +  mUpdateAvailable);
		if(mUpdateAvailable) {
			mUpdateAvailable = false;
//			if(!mIgnoreCurrentLoad) {
				mLoadState[mCurIndex] = DrawingSetLoadState.INACTIVE;
				mCurIndex = (mCurIndex == 0) ? 1 : 0;
				mLoadState[mCurIndex] = DrawingSetLoadState.LOADED;
				mInitialized = true;			// true after any data is loaded and stays true forever
				mIgnoreCurrentLoad = false;
				Log.d(TAG,"  Drawing set switched...");
			
				return true;
//			}
//			else {
//				return false;
//			}
		}
		return false;
	}

// state query methods
	public boolean IsLoadingData() {
		return IsNextLoading();
	}

	
	public boolean IsNextLoading() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return (mLoadState[nextIndex] == DrawingSetLoadState.LOADING);
	}
		

	
// get value methods
	public ArrayList<POIDrawing> GetCurrentPOI() {
		if(mCurIndex == 0) {
			if(mPOI0 == null) Log.d(TAG, "POI array is null");
//			else if(mPOI0.size() == 0) Log.d(TAG, "POI array is empty");
			return mPOI0;
		}
		else {
			if(mPOI1 == null) Log.d(TAG, "POI array is null");
//			else if(mPOI1.size() == 0) Log.d(TAG, "POI array is empty");
			return mPOI1;
		}
	}

	public Bitmap GetImage() {
		return mBackgroundImage[mCurIndex];
	}
	public DrawingSetLoadState GetNextLoadState() {
		return GetNextState();
	}
	
	public GeoRegion GetLoadedGeoRegion() {
		return mLoadedGeoRegion[mCurIndex];
	}
	
	public float GetLoadedScale() {
		return mLoadedScale[mCurIndex];
	}
	
	public float GetLoadedHeading() {
		return mLoadedHeading[mCurIndex];
	}
	
	public GeoRegion GetNextGeoRegion() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadedGeoRegion[nextIndex];
	}
	
	public GeoRegion GetNextLowestResolutionGeoRegion() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadedLowestResolutionGeoRegion[nextIndex];
	}

	public float GetNextScale() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadedScale[nextIndex];
	}
	
	public float GetNextHeading() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadedHeading[nextIndex];
	}
	
	public DrawingSetLoadState GetNextState() {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		return mLoadState[nextIndex];
	}
	
//	public PointXY GetUserLocation() {
//		return (new PointXY(mLongitude[mCurIndex], mLatitude[mCurIndex]) );
//	}
//	
//	public float GetHeading() {
//		return mHeading[mCurIndex];
//	}
//	
	public ArrayList<POIDrawing> GetFocusableObjects() {		
		ArrayList<POIDrawing> resultList = new ArrayList<POIDrawing>();
		for(POIDrawing poiDrawing : GetCurrentPOI()) {
			switch(poiDrawing.mType) {
				case CHAIRLIFT_ACCESS: 
				case CARACCESS_PARKING: 
				case SKIRESORTSERVICE_INFO:
				case SKIRESORTSERVICE_RESTAURANT: {
					resultList.add(poiDrawing);
					break;
				}
				default: // do nothing
			}
		}
		return resultList;

	}
	
	public float[] getPanOffset(){
		return mMapPanOffset;
	}


// other methods
	public void CreateNextImageFromObjects(TreeMap<Integer, ArrayList<WorldObjectDrawing>> objectArrays, ArrayList<MapCompositionItem> mapComposition, GeoRegion loadingGeoRegion, float scale, float heading, World2DrawingTransformer world2DrawingTransformer, RenderSchemeManager rsm) {
		int nextIndex = (mCurIndex == 0) ? 1 : 0;
		
		// convert loadingGeoRegion into drawing coordinates...
		RectF mLoadingGeoRegionBoundaryInDrawingCoords = world2DrawingTransformer.TransformGPSRectXYToRectF(loadingGeoRegion.mBoundingBox);
		PointXY mLoadingGeoRegionCenterInDrawingCoords =  world2DrawingTransformer.TransformGPSPointToDrawingPoint(loadingGeoRegion.mCenterPoint);
		Matrix drawing2BitmapTransformMatrix = new Matrix();
		
		float fImageSize = (float)ImageSize();
		double doubleDrawScale = ((double)fImageSize / (double) (mLoadingGeoRegionBoundaryInDrawingCoords.right - mLoadingGeoRegionBoundaryInDrawingCoords.left + 1));
		float drawScale = (float)doubleDrawScale;
//		Log.d(TAG, "CreateNewImageFromObjects - translate 1: " + mLoadingGeoRegionCenterInDrawingCoords.x + ", " + mLoadingGeoRegionCenterInDrawingCoords.y + " ||| drawScale = " + doubleDrawScale);
//		Log.d(TAG, "drawScale = " + doubleDrawScale
//				+ " ||| fImageSize = " + String.format("%.2f", fImageSize) 
//				+ " ||| GeoBox.right = " + String.format("%.3f", mLoadingGeoRegionBoundaryInDrawingCoords.right)
//				+ " ||| GeoBox.left = " + String.format("%.3f", mLoadingGeoRegionBoundaryInDrawingCoords.left));
		drawing2BitmapTransformMatrix.setTranslate((float)(- mLoadingGeoRegionCenterInDrawingCoords.x),(float)(-mLoadingGeoRegionCenterInDrawingCoords.y));	// move region to drawing origin
		drawing2BitmapTransformMatrix.postScale(drawScale,drawScale);																						// scale it to size of bitmap
		drawing2BitmapTransformMatrix.postTranslate(fImageSize/2.0f, fImageSize/2.0f);									// move center to bitmap center
		
		Matrix poiDrawing2BitmapTransformMatrix = new Matrix();
		poiDrawing2BitmapTransformMatrix.setTranslate((float)(- mLoadingGeoRegionCenterInDrawingCoords.x),(float)(-mLoadingGeoRegionCenterInDrawingCoords.y));	// move region to drawing origin
		poiDrawing2BitmapTransformMatrix.postScale(drawScale,drawScale);	
//		poiDrawing2BitmapTransformMatrix.postRotate(-mLoadedHeading[mCurIndex == 0 ? 1 : 0]);
		poiDrawing2BitmapTransformMatrix.postTranslate(fImageSize/2.0f, fImageSize/2.0f);									// move center to bitmap center

		// set background to land by default
		Paint landPaint = rsm.GetAreaPaint(0);
		mBackgroundCanvas[nextIndex].drawColor(landPaint.getColor()); 
		
		float viewportWidthScaleRelativeToImageSize = (float)(1.0f/drawScale);
		
		mCollisionDetector.Reset();
		
		mEmptyTileSet[nextIndex] = true;
		
		for(Integer index : objectArrays.keySet()) {
			if(mCancelLoad) return;
			MapCompositionItem mcItem = mapComposition.get(index);
			if(mcItem.mDrawingConstraint.IncludeInDrawing(viewportWidthScaleRelativeToImageSize)) {	
				ArrayList<WorldObjectDrawing> drawingsArray = objectArrays.get(index);
//				Log.d(TAG, "CreateNextImageFromObjects - loop index: " + index + " (" + mapComposition.get(index).mObjectType + ") with " + drawingsArray.size() + " drawing objects");
			
				if(mcItem instanceof MapCompositionObjectItem) { 
					if(mcItem.mObjectClass == WorldObjectDrawing.WO_Class.NO_DATA_ZONE) { 
						for(WorldObjectDrawing drawingObject : drawingsArray) {
							if(mCancelLoad) return;
							((NoDataZoneDrawing) drawingObject).Draw(mBackgroundCanvas[nextIndex], rsm, drawing2BitmapTransformMatrix, viewportWidthScaleRelativeToImageSize);
						}
					}
					if(mcItem.mObjectClass == WorldObjectDrawing.WO_Class.TERRAIN) { 
						mEmptyTileSet[nextIndex] = false;
						for(WorldObjectDrawing drawingObject : drawingsArray) {
							if(mCancelLoad) return;
							((TerrainDrawing) drawingObject).Draw(mBackgroundCanvas[nextIndex], rsm, drawing2BitmapTransformMatrix, viewportWidthScaleRelativeToImageSize);
						}
					}
					if(mcItem.mObjectClass == WorldObjectDrawing.WO_Class.TRAIL) {  															
						mEmptyTileSet[nextIndex] = false;
						for(WorldObjectDrawing drawingObject : drawingsArray) {
							if(mCancelLoad) return;
							((TrailDrawing) drawingObject).Draw(mBackgroundCanvas[nextIndex], heading, rsm, drawing2BitmapTransformMatrix, viewportWidthScaleRelativeToImageSize);
						}			
					}		
				}
				if(mcItem instanceof MapCompositionNameItem) { 
					for(WorldObjectDrawing drawingObject : drawingsArray) {
						if(mCancelLoad) return;
						if(!((TrailDrawing) drawingObject).mDataObject.mName.equalsIgnoreCase(WorldObject.UNKNOWN_ITEM_NAME)) {
							((TrailDrawing) drawingObject).DrawNames(mBackgroundCanvas[nextIndex], rsm, drawing2BitmapTransformMatrix, viewportWidthScaleRelativeToImageSize, mCollisionDetector);
						}
					}
				}
				
			}
		}
		
		mAnnotationLayer.Draw(mBackgroundCanvas[nextIndex], drawScale, drawing2BitmapTransformMatrix, poiDrawing2BitmapTransformMatrix, "", mContext.getResources());

	}
	
	/**
	 * Returns a value bound within {@code min} and {@code max}.
	 * @param value
	 * @param min
	 * @param max
	 * @return
	 */
	private float getWithinMinMax(float value, float min, float max){
		return (float) Math.max(min, Math.min(max, value));
	}
	
	private float lerp(float a, float b, float t){
		return a + t * (b - a);
	}

}
