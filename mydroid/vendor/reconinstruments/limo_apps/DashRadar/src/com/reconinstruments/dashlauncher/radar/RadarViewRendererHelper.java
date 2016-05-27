package com.reconinstruments.dashlauncher.radar;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.dashlauncher.radar.maps.drawings.MapDrawings;
import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;
import com.reconinstruments.dashlauncher.radar.maps.objects.MapImagesInfo;
import com.reconinstruments.dashlauncher.radar.prim.Trapezoid;
import com.reconinstruments.dashlauncher.radar.prim.Vector3;
import com.reconinstruments.dashlauncher.radar.render.CommonRender;
import com.reconinstruments.dashlauncher.radar.render.GLDotField;
import com.reconinstruments.dashlauncher.radar.render.GLFocusRect;
import com.reconinstruments.dashlauncher.radar.render.GLUserArrow;
import com.reconinstruments.dashlauncher.radar.render.GLCompass;
import com.reconinstruments.dashlauncher.radar.render.GLDistanceCircle;
import com.reconinstruments.dashlauncher.radar.render.GLDynamicPath;
import com.reconinstruments.dashlauncher.radar.render.GLFrontTexture;
import com.reconinstruments.dashlauncher.radar.render.GLGlobalIconRenderer;
import com.reconinstruments.dashlauncher.radar.render.GLGlobalTextRenderer;
import com.reconinstruments.dashlauncher.radar.render.GLMapRenderer;
import com.reconinstruments.dashlauncher.radar.render.GLObject;
import com.reconinstruments.dashlauncher.radar.render.GLZoomControl;
import com.reconinstruments.dashradar.R;

public class RadarViewRendererHelper extends BaseRendererHelper 
{
	private final static String TAG = "RadarViewRendererHelper";
	
	protected static final float MIN_PITCH				= -75.0f;
	protected static final float MAX_PITCH				= -15.0f;
	
	public static final float RADAR_ANGLE				= -65.0f;
	public static final float RADAR_DISK_RADIUS			= 400.0f;
	public static final float RADAR_DISK_YOFFSET		= 270.0f;
	
	private static final float DISPLAY_RATIO			= (428.0f/240.0f);
	private static final float CAMERA_H_FOV				= 50.0f;
	private static final float CAMERA_W_FOV				= CAMERA_H_FOV*DISPLAY_RATIO;
	private static final float USER_ANGLE_FROM_CENTER	= -1.0f*((CAMERA_H_FOV/2.0f)-5.0f);
	private static final float FOCUS_RECT_FACTOR		= 1.0f;
	
	private static final float BASE_FOCUS_MAX_TOP_ANGLE		= 9.0f;
	private static final float BASE_FOCUS_MIN_BOTTOM_ANGLE	= -15.0f;
	private static final float BASE_FOCUS_RANGE_ANGLE	= BASE_FOCUS_MAX_TOP_ANGLE-BASE_FOCUS_MIN_BOTTOM_ANGLE;
	
	private static final float BASE_FOCUS_RECT_MAX		= -25.0f;
	private static final float BASE_FOCUS_RECT_MIN		= -70.0f;
	private static final float PITCH_MAX				= 0.0f;
	private static final float PITCH_MIN				= -80.0f;
	private static final float PITCH_HORIZON_LIMIT		= -80.0f;
	private static final float PITCH_BIRDS_EYE_LIMIT	= -15.0f;

	private static final float BASE_FOCUS_RECT_RANGE	= BASE_FOCUS_RECT_MAX-BASE_FOCUS_RECT_MIN;
	private static final float BASE_FOCUS_RECT_YOFFSET	= 10.0f;
	
	private static final float ANGLE_PITCH_JUMP			= 2.0f;
	
	private static final float ANGLE_TO_RAD				= (float)Math.PI/180.0f;

	private static float mMaxDistanceToDraw				= 2000.0f;
	private static float mScaleCoeff					= 0.0f;
	
	private boolean mIsRadarMode						= true;
	private boolean mReloadGLObjects					= true;
	protected static int mUnits							= ReconSettingsUtil.RECON_UINTS_METRIC;
	
	// this is set to true when a user has clicked left/right while in map mode
	private boolean mLockYawValue						= false; 
	
	protected float	mXOffset							= 0;
	protected float	mYOffset							= Util.getZoomYOffset(Util.mZoomMin);
	protected float	mZOffset							= Util.mZoomMin;
	protected float mYaw								= 0.0f;	
	protected float mLockedYaw							= 0.0f;
	protected float mPitch								= 0.0f;
	protected float mPitchOffset						= 0.0f;
	protected float mPitchOverflow						= 0.0f;
	protected float	mRoll								= 0.0f;
	protected float	mYawOffset							= 0.0f; 
	protected float	mCutoffAngle						= -1.0f;	
	
	protected Vector3	mCamOffset						= new Vector3();
	protected Vector3	mCamOrientation					= new Vector3();
	protected Vector3	mCamLocation					= new Vector3();
	protected Vector3	mUserOffset						= new Vector3();
	protected Vector3	mUserOrientation				= new Vector3();
	protected PointF	mUserLocalLoc					= null;	
	protected Location	mUserWorldLoc					= null;
	
	protected float[] 	mFocusPoints					= new float[2];
	protected final int FOCUS_TOP						= 0;
	protected final int FOCUS_BOTTOM					= 1;
	protected Trapezoid	mFocusTrapezoid					= new Trapezoid();
	protected Vector3 	mFocusPoint						= new Vector3();
	
	public static final float USER_ARROW_SIZE_METERS	= 35.0f;
	private float mNominalZoom							= 200.0f;
	
	private int mZoomLevels								= 0;
	private int mCurrentZoomIndex						= 0;
	
	protected LocationTransformer mLocationTransformer	= null;
	
	// Array list that holds all GL Objects being currently drawn in the render cycle.
	protected ArrayList<GLObject> 	mGLObjectArray		= null;
	protected boolean mRefreshGLObjectArray				= true;
	
	// GL Objects
	protected GLUserArrow			mUserArrow			= null;
	protected GLDynamicPath			mUserPath			= null;
	protected GLMapRenderer			mGLMapRenderer		= null;
	protected OOIController			mOOIController		= null;  
	
	protected GLGlobalTextRenderer 	mGLTextRenderer		= null;
	protected GLGlobalIconRenderer 	mGLIconRenderer		= null;
	
	// Map     mode-specific GL Objects	
	protected GLZoomControl			mZoomControlTexture = null;
	protected GLFrontTexture		mFocusRect			= null;
	
	// Radar   mode-specific GL Objects
	protected GLFrontTexture		mDiscMaskCompassTexture	= null;	
	protected GLFrontTexture		mDiscMaskBuddyTexture	= null;
	protected GLFrontTexture		mTopMaskTexture			= null;
	protected GLFrontTexture		mBottomMaskTexture		= null;
	
	// Compass mode-specific GL Objects
	protected GLCompass			mGLCompassFull				= null;
	protected GLCompass			mGLCompassBuddyMode			= null;
	
	// Values that describe the disk in the main radar mask when the following render conditions are true
	// - Maps Pitch   = -63
	// - Aspect Ratio =  50
	// - Disk (X,Y,Z) = (0, 340, 0) - ahead of the user
	// - Disk Radius  =  550
	
	protected Vector3			mDiskCenter = null;
	protected GLDistanceCircle	mGLCircle	= null;
	protected GLFocusRect 		mGLFocusRect[] = new GLFocusRect[4];
	
	
	public RadarViewRendererHelper(Context context, boolean isRadarMode, LocationTransformer locationTransformer, int zoomLevels) 
	{		
		super(context);
		
		mLocationTransformer = locationTransformer;
		
		mUserArrow		= new GLUserArrow();
		mUserLocalLoc	= new PointF(0, 0);
		mUserWorldLoc	= new Location("");
		
		mUserPath = new GLDynamicPath(0.2f, 0.2f, 1.0f, 1.0f, 5.0f);

		mIsRadarMode = isRadarMode;
		
		mZoomLevels	= zoomLevels;
		mZoomControlTexture = new GLZoomControl(mZoomLevels);
		mFocusRect			= new GLFrontTexture(1, 70, 70);
		mFocusRect.SetOffsets(0.0f, BASE_FOCUS_RECT_YOFFSET, 0);
		
		mTopMaskTexture			= new GLFrontTexture(1);
		mBottomMaskTexture		= new GLFrontTexture(1);
		mDiscMaskCompassTexture	= new GLFrontTexture(1);
		mDiscMaskBuddyTexture	= new GLFrontTexture(1);
		
		mGLTextRenderer = new GLGlobalTextRenderer(); 
		mGLIconRenderer = new GLGlobalIconRenderer();
		
		// -- Mask circle descriptor variables -- //
		mDiskCenter = new Vector3(0, RADAR_DISK_YOFFSET, 0);
		mGLCircle	= new GLDistanceCircle(mDiskCenter, RADAR_DISK_RADIUS, 64);
		for(int i = 0; i < 4; i++) {
			mGLFocusRect[i] = new GLFocusRect(mDiskCenter);
		}
		// -------------------------------------- //
		
		mOOIController	 	= new OOIController(mLocationTransformer, mGLTextRenderer, mGLIconRenderer, mGLCircle, mContext);
		
		mGLMapRenderer		= new GLMapRenderer();
		
		mGLCompassFull 		= new GLCompass(RADAR_ANGLE);
		mGLCompassBuddyMode	= new GLCompass(RADAR_ANGLE);
		
		// TODO		
		mGLObjectArray	= new ArrayList<GLObject>(16);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mUnits = ReconSettingsUtil.getUnits(mContext);
		mUserPath.Clear();
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	public void SetMapDrawings(MapImagesInfo mapImagesInfo, MapDrawings mapDrawings, Context context){
		ClearMapElements();
		mOOIController.SetPOIs(mapDrawings.POIDrawings);
		mGLMapRenderer.setMapImages(mapImagesInfo);
		mRefreshGLObjectArray = true;		
	}
	
	public void UpdateBuddies(Bundle bundle, Location userLocation, float maxZoomLevel, Context context){
		mOOIController.UpdateBuddies(bundle, userLocation, maxZoomLevel, context);
	}
	
	public void RefreshGLDrawObjects()
	{
		mGLObjectArray.clear();
		
		if(MapsLoaded())
		{
			mGLObjectArray.add(mGLMapRenderer);
		}
				
		mGLObjectArray.add(mDiscMaskCompassTexture);
		mGLObjectArray.add(mDiscMaskBuddyTexture);
		mGLObjectArray.add(mGLCompassFull);
		mGLObjectArray.add(mGLCompassBuddyMode);
		mGLObjectArray.add(mTopMaskTexture);
		mGLObjectArray.add(mBottomMaskTexture);
				
		mGLObjectArray.add(mUserArrow);
		
		mGLObjectArray.add(mOOIController);
		
		mGLObjectArray.add(mFocusRect);
		mGLObjectArray.add(mZoomControlTexture);
		
		mRefreshGLObjectArray = false;
	}
	
	public boolean MapsLoaded(){
		return mGLMapRenderer.hasRenderObjects();
	}

	public void SetRadarMode(boolean isRadarMode)
	{
		mIsRadarMode = isRadarMode;
	}
	
	protected PointF GetFocusPointF(float distance) {
		return new PointF((float)Math.sin(mUserOrientation.x*Math.PI/180.0f)*distance, (float)Math.cos(mUserOrientation.x*Math.PI/180.0f)*distance);
	}
	
	protected float GetFocusAngle(){
		float retAngle = BASE_FOCUS_MAX_TOP_ANGLE-(BASE_FOCUS_RANGE_ANGLE/BASE_FOCUS_RECT_RANGE)*(-1.0f*(mPitchOffset-BASE_FOCUS_RECT_MAX));
		//Log.v(TAG,"retAngle="+retAngle +" mPitchOffset="+mPitchOffset);
		return retAngle;
	}
	
	protected void UpdateFocusPoint(){
		if(MapsLoaded()) {
			float foucsAngle = GetFocusAngle();
			
			CalcFocusPoints(foucsAngle-12.0f, foucsAngle);
			//mFocusPoint.set(mUserLocalLoc.x+(float)Math.sin(mCamOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_BOTTOM],mUserLocalLoc.y+(float)Math.cos(mCamOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_BOTTOM], 0.0f);
			mFocusPoint.set(mUserLocalLoc.x+(float)Math.sin(mCamOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_BOTTOM],mUserLocalLoc.y+(float)Math.cos(mCamOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_BOTTOM], 0.0f);
			
			float fTopScale = 27.0f/(140.0f+2.0f*(31.0f+mPitchOffset));
			float fBottomScale = 18.0f/(140.0f+2.0f*(15.0f+mPitchOffset));
			
			PointF pointTop = new PointF((float)Math.sin(mCamOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_TOP], (float)Math.cos(mCamOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_TOP]);
			PointF pointBottom = new PointF((float)Math.sin(mCamOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_BOTTOM],(float)Math.cos(mCamOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_BOTTOM]);
			
			//Log.v(TAG,mCamOrientation.x+" - "+pointTop.x+","+pointTop.y+" : "+pointBottom.x+","+pointBottom.y);
			
			float deltaXTop = pointTop.y*fTopScale;
			float deltaYTop = -1.0f*pointTop.x*fTopScale;
			
			float deltaXBottom = pointBottom.y*fBottomScale;
			float deltaYBottom = -1.0f*pointBottom.x*fBottomScale;
			
			mFocusTrapezoid.Set(Trapezoid.TOP_LEFT, mUserLocalLoc.x+pointTop.x-deltaXTop, mUserLocalLoc.y+pointTop.y-deltaYTop);
			mFocusTrapezoid.Set(Trapezoid.TOP_RIGHT, mUserLocalLoc.x+pointTop.x+deltaXTop, mUserLocalLoc.y+pointTop.y+deltaYTop);
			mFocusTrapezoid.Set(Trapezoid.BOTTOM_RIGHT, mUserLocalLoc.x+pointBottom.x+deltaXBottom, mUserLocalLoc.y+pointBottom.y+deltaYBottom);
			mFocusTrapezoid.Set(Trapezoid.BOTTOM_LEFT, mUserLocalLoc.x+pointBottom.x-deltaXBottom, mUserLocalLoc.y+pointBottom.y-deltaYBottom);
			
			// >>>> -------------- Debug --------- 
			//Log.v(TAG,"focusVector="+focusVector+" mFocusRectF.top="+mFocusRectF.top+" mFocusRectF.bottom="+mFocusRectF.bottom);
			/*pointTop = new PointF((float)Math.sin(mUserOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_TOP], (float)Math.cos(mUserOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_TOP]);
			pointBottom = new PointF((float)Math.sin(mUserOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_BOTTOM], (float)Math.cos(mUserOrientation.x*Math.PI/180.0f)*mFocusPoints[FOCUS_BOTTOM]);
			
			deltaXTop = pointTop.y*fTopScale;
			deltaYTop = -1.0f*pointTop.x*fTopScale; 
			
			deltaXBottom = pointBottom.y*fBottomScale;
			deltaYBottom = -1.0f*pointBottom.x*fBottomScale;
			
			PointF pointTopRight = new PointF(pointTop.x+deltaXTop, pointTop.y+deltaYTop);
			PointF pointTopLeft = new PointF(pointTop.x-deltaXTop, pointTop.y-deltaYTop);
			PointF pointBottomLeft = new PointF(pointBottom.x-deltaXBottom, pointBottom.y-deltaYBottom);
			PointF pointBottomRight = new PointF(pointBottom.x+deltaXBottom, pointBottom.y+deltaYBottom);
			
			mGLFocusRect[0].setDrawParams(mUserOrientation, mCamOffset, mUserOffset, pointTopRight);
			mGLFocusRect[1].setDrawParams(mUserOrientation, mCamOffset, mUserOffset, pointTopLeft);
			mGLFocusRect[2].setDrawParams(mUserOrientation, mCamOffset, mUserOffset, pointBottomLeft);
			mGLFocusRect[3].setDrawParams(mUserOrientation, mCamOffset, mUserOffset, pointBottomRight);*/
			// <<<< -------------- Debug ---------
			
			mCamLocation.set(mUserLocalLoc.x+(float)Math.sin(mCamOrientation.x*Math.PI/180.0f)*-1.0f*mYOffset, mUserLocalLoc.y+(float)Math.cos(mCamOrientation.x*Math.PI/180.0f)*-1.0f*mYOffset, 0.0f);
			//Log.v(TAG,"mUserLocalLoc: "+mUserLocalLoc.x+","+mUserLocalLoc.y +" mCamLocation: "+mCamLocation.x+","+mCamLocation.y+" mYOffset=-"+mYOffset);
		}
	}
	
	public boolean HasBuddies() {
		return mOOIController.HasBuddies();
	}
	
	public void SetUserLocation(Location location)
	{
		float x = (float)mLocationTransformer.TransformLongitude(location.getLongitude());
		float y = (float)mLocationTransformer.TransformLatitude(location.getLatitude());
		
		if (x != mUserLocalLoc.x || y != mUserLocalLoc.y && (!(x == 0 && y == 0)))
		{
			mUserLocalLoc.set(x, y);
			mUserWorldLoc.set(location);
			
			mUserOffset.set(-mUserLocalLoc.x, -mUserLocalLoc.y, 0.0f);
			//Log.v(TAG,"User "+mUserLocalLoc.x+","+ mUserLocalLoc.y);
			
			mUserPath.AddPoint(x, y);
		}
	}
	
	public void SetNominalZoomLevel(float nominalZoomLevel) {
		mNominalZoom = nominalZoomLevel;
	}
	
	public void SetCurrentZoom(int currentZoomIndex, float zoomLevel) 
	{
		mCurrentZoomIndex = currentZoomIndex;
		mScaleCoeff = zoomLevel/(float) Math.tan(90.0f*Math.PI/200.0f);
	}
	
	protected void UpdateCamOrientation(float yaw) 
	{
		mCamOrientation.set(yaw + mYawOffset, mPitch, 0);
	}
	
	public void SetCamera(float pitch, float pitchOffset, float yOffset, float yawOffset, float cutoffAngle, float zoomLevel)
	{
		while (mYawOffset > 360.0f)	mYawOffset -= 360.0f;
		while (mYawOffset < 000.0f)	mYawOffset += 360.0f;
		
		mScaleCoeff = zoomLevel/(float) Math.tan(90.0f*Math.PI/200.0f);

		mYOffset = yOffset;
		mZOffset = zoomLevel;
		mYawOffset = yawOffset;
		mCutoffAngle = cutoffAngle;
		
		setHeadingDirection(mYaw);
		//Log.v(TAG,"Offset="+mXOffset+","+mYOffset+","+mZOffset);
		mCamOffset.set(mXOffset, mYOffset, mZOffset).mult(-1);
		
		// pitchOffset: -90.0 is looking down, 0.0 is horizon
		// pitch: 0 is looking down, -90.0 is horizon
		
		float deltaAngle = pitchOffset - mPitchOffset;
		if(pitchOffset < BASE_FOCUS_RECT_MIN) {
			mPitchOffset = BASE_FOCUS_RECT_MIN;
			mPitchOverflow = deltaAngle;
			mPitch = pitch-mPitchOverflow*(pitch-PITCH_BIRDS_EYE_LIMIT)/(PITCH_MIN-BASE_FOCUS_RECT_MIN);
		}
		else if(pitchOffset > BASE_FOCUS_RECT_MAX) {
			mPitchOffset = BASE_FOCUS_RECT_MAX;
			mPitchOverflow = deltaAngle;
			mPitch = pitch-mPitchOverflow*(pitch-PITCH_HORIZON_LIMIT)/(PITCH_MAX-BASE_FOCUS_RECT_MAX);
		} else {
			mPitchOffset = pitchOffset;
			mPitchOverflow = 0.0f;
			mPitch = pitch;
		}
		
		//mPitch = pitch-mPitchOverflow;
		
		//Log.v(TAG,"deltaAngle="+deltaAngle+" pitch="+pitch+" mPitchOffset="+mPitchOffset+" mPiltchOverflow="+mPitchOverflow+" mPitch="+mPitch+" pitchOffset="+pitchOffset);
		mUserOrientation.set((mLockedYaw - mYaw) + mYawOffset, mPitch, 0);
	}
	
	protected void LoadGLObjects(GL10 gl) 
	{
		SetFog(gl, mMaxDistanceToDraw); 
		
		mGLTextRenderer.loadGLTextures(gl, 24);
		mGLIconRenderer.loadGLTextures(gl, mContext);
				
		mTopMaskTexture.LoadGLTexture(gl, mContext, R.raw.map_mask_top, 0);
		mBottomMaskTexture.LoadGLTexture(gl, mContext, R.raw.map_mask_bottom, 0);
		mDiscMaskCompassTexture.LoadGLTexture(gl, mContext, R.raw.map_mask_disc_compass, 0);
		mDiscMaskBuddyTexture.LoadGLTexture(gl, mContext, R.raw.map_mask_disc_buddy, 0);
				
		mZoomControlTexture.LoadGLTexture(gl, mContext, R.raw.zoom_1, 0);
		mZoomControlTexture.LoadGLTexture(gl, mContext, R.raw.zoom_2, 1);
		mZoomControlTexture.LoadGLTexture(gl, mContext, R.raw.zoom_3, 2);
		mZoomControlTexture.LoadGLTexture(gl, mContext, R.raw.zoom_4, 3);
		
		mFocusRect.LoadGLTexture(gl, mContext, R.raw.focus_rect, 0);
		
		mUserArrow.LoadGLTexture(gl, mContext, R.raw.radar_user, 0);
						
		mGLMapRenderer.loadGLObjects();
		
		mGLCompassFull.LoadGLTexture(gl, mContext, R.raw.compass_texture, 0);		
		mGLCompassBuddyMode.LoadGLTexture(gl, mContext, R.raw.compass_texture_simple, 0);
		
		mReloadGLObjects = false;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
		
		mReloadGLObjects = true;
	}
	@Override
	public void onDrawFrame(GL10 gl)
	{
		super.onDrawFrame(gl);
		
		if(mReloadGLObjects)	  { LoadGLObjects(gl); }
		if(mRefreshGLObjectArray) { RefreshGLDrawObjects(); }
		
		// Redraw background color
		gl.glClearColor(CommonRender.FOG_GRAY_COLOR_R, CommonRender.FOG_GRAY_COLOR_G, CommonRender.FOG_GRAY_COLOR_B, 1.0f);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		
		// Reset the Modelview Matrix
		gl.glLoadIdentity(); 
		
		updateGLDrawElementParams();
		
		for(GLObject obj: mGLObjectArray)
		{
			obj.draw(gl);
		} 
		
		// Debug
		//mGLCircle.draw(gl);
		/*for(int i = 0; i < 4; i++) {
			mGLFocusRect[i].draw(gl);
		}*/
	}
	
	public void SetGLCircleParams(float yPos, float radius) {
		mGLCircle.SetPosition(0, yPos, 0); 	// Perfect 2.1f
		mGLCircle.SetRadius(radius); 	// Perfcet 1.15
	}

	// Old
	protected float CalcCenterPoint(){
		float retCenterPoint = (float)(-1.0f*mCamOffset.y/(Math.sin(ANGLE_TO_RAD*(90.0f+mCamOrientation.y))));
		return retCenterPoint;
	}
	
	protected float GetCamDistanceToUser(){
		return (-1.0f*mCamOffset.y)/(float)Math.cos(ANGLE_TO_RAD*(90.0f+USER_ANGLE_FROM_CENTER));
	}
	
	protected void CalcFocusPoints(float bottomAngle, float topAngle){
		if(topAngle < bottomAngle) topAngle = bottomAngle;
		if(bottomAngle < USER_ANGLE_FROM_CENTER) bottomAngle = USER_ANGLE_FROM_CENTER;
		
		float camToUserDistance = (float)Math.sqrt(mCamOffset.y*mCamOffset.y+mZOffset*mZOffset);
		float userAngle = 180.0f-Math.abs(USER_ANGLE_FROM_CENTER)-(90.0f+mCamOrientation.y);
	
		//Log.v(TAG,"A="+mCamOrientation.y+" Y="+mCamOffset.y+" Z="+mZOffset+" D="+camToUserDistance+" AT="+angleOfTarget+" CA="+cameraAngle+" CP1="+centerPoint+" CP2="+CalcCenterPoint());

		// Center
		/*float cameraAngle = topAngle-USER_ANGLE_FROM_CENTER-1.0f;//(topAngle+bottomAngle)/2.0f-USER_ANGLE_FROM_CENTER;
		float angleOfTarget = 180.0f-userAngle-cameraAngle;
		float centerFocusPoint = camToUserDistance*(float)Math.sin(ANGLE_TO_RAD*cameraAngle)/(float)Math.sin(ANGLE_TO_RAD*angleOfTarget);
		if(centerFocusPoint < 0.0f) centerFocusPoint = mMaxDistanceToDraw;
		if(centerFocusPoint > mMaxDistanceToDraw) centerFocusPoint = mMaxDistanceToDraw;*/ 
		
		// Top
		float cameraAngle = topAngle-USER_ANGLE_FROM_CENTER;
		float angleOfTarget = 180.0f-userAngle-cameraAngle;
		mFocusPoints[FOCUS_TOP] = camToUserDistance*(float)Math.sin(ANGLE_TO_RAD*cameraAngle)/(float)Math.sin(ANGLE_TO_RAD*angleOfTarget);
		if(mFocusPoints[FOCUS_TOP] < 0 || mFocusPoints[FOCUS_TOP] > mMaxDistanceToDraw) mFocusPoints[FOCUS_TOP] = mMaxDistanceToDraw;
		
		// Bottom
		cameraAngle = bottomAngle-USER_ANGLE_FROM_CENTER;
		angleOfTarget = 180.0f-userAngle-cameraAngle;
		mFocusPoints[FOCUS_BOTTOM] = camToUserDistance*(float)Math.sin(ANGLE_TO_RAD*cameraAngle)/(float)Math.sin(ANGLE_TO_RAD*angleOfTarget);
		
		//return centerFocusPoint;
	}
	
	public void updateGLDrawElementParams()
	{
		UpdateFocusPoint();
		
		if(!mGLMapRenderer.IsHidden())
		{
			mGLMapRenderer.setDrawParams(mCamOrientation, mCamOffset, mUserOffset);
			mGLMapRenderer.setParams(mIsRadarMode, mMaxDistanceToDraw, mUserLocalLoc);
		}
		
		if(!mUserPath.IsHidden())
		{
			mUserPath.setDrawParams(mCamOrientation, mCamOffset, mUserOffset);
		}
		
		if(!mUserArrow.IsHidden())
		{
			mUserArrow.setDrawParams(mUserOrientation, mCamOffset, mUserOffset);
			mUserArrow.SetScaleF(mLocationTransformer.TransformMetersValue(USER_ARROW_SIZE_METERS)*(mZOffset/mNominalZoom));
		}
		
		if(!mOOIController.IsHidden())
		{
			mOOIController.setDrawParams(mCamOrientation, mCamOffset, mUserOffset);
			mOOIController.SetParams(mFocusTrapezoid, mFocusPoint, mCamLocation, mUserLocalLoc, mUserWorldLoc, mMaxDistanceToDraw);
		}
		
		if(!mZoomControlTexture.IsHidden())
		{
			mZoomControlTexture.setTextureIndex(mCurrentZoomIndex);
		}
		
		if(!mGLCompassFull.IsHidden())
		{
			mGLCompassFull.setParams(mYaw + mYawOffset); 
		}
		if(!mGLCompassBuddyMode.IsHidden())
		{
			mGLCompassBuddyMode.setParams(mYaw + mYawOffset); 
		}
		
		if(!mFocusRect.IsHidden()) {
			mFocusRect.SetOffsets(0, BASE_FOCUS_RECT_YOFFSET+(2.0f*(45.0f+mPitchOffset)), 0.0f);
		}
		 
		mGLCircle.setDrawParams(mUserOrientation, mCamOffset, mUserOffset);

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) 
	{
		super.onSurfaceChanged(gl, width, height);
	}
	
	public void ClearMapElements(){	}
			
	public void setHeadingDirection(float yaw) 
	{
		mYaw = yaw;
		if(!mLockYawValue)
		{
			UpdateCamOrientation(mYaw);	
			mLockedYaw = mYaw;
		}	
		else{ UpdateCamOrientation(mLockedYaw);	}
	}
	
	public void SetMaxDistanceToDraw(float maxDistanceToDraw){
		mMaxDistanceToDraw = maxDistanceToDraw;
	}
	
	public void SetYawLock(boolean isLocked)
	{
		if(!mLockYawValue) // if yaw was not previously locked
		{
			mLockedYaw		= mYaw;
		}
		mLockYawValue	= isLocked;
	}
	
	public GLFrontTexture	GetBottomMaskTexture()		{ return mBottomMaskTexture; }
	public GLFrontTexture	GetTopMaskTexture()			{ return mTopMaskTexture; }
	public GLFrontTexture	GetDiscMaskCompassTexture()	{ return mDiscMaskCompassTexture; }
	public GLFrontTexture	GetDiscMaskBuddyTexture()	{ return mDiscMaskBuddyTexture; }
	public GLCompass		GetCompassFullTexture()		{ return mGLCompassFull; }
	public GLCompass		GetCompassBuddyModeTexture(){ return mGLCompassBuddyMode; }
	public GLMapRenderer	GetGLMapRenderer()			{ return mGLMapRenderer; }
	public GLZoomControl	GetZoomControlTexture()		{ return mZoomControlTexture; }
	public GLFrontTexture	GetFocusRect()				{ return mFocusRect; }
	public OOIController	getOOIController()			{ return mOOIController; }
	public GLUserArrow		getUserArrow()				{ return mUserArrow; }
}
