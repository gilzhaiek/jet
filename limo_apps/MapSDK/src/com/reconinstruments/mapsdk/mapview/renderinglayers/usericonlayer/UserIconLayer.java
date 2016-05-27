package com.reconinstruments.mapsdk.mapview.renderinglayers.usericonlayer;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.MapView.MapViewMode;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.reticleitems.DynamicReticleItemsInterface.IDynamicReticleItems;
import com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.userposition.DynamicUserPositionInterface.IDynamicUserPosition;
import com.reconinstruments.mapsdk.mapview.renderinglayers.RenderingLayer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.Map2DLayer.BackgroundSize;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MeshGL;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;

public class UserIconLayer extends RenderingLayer implements  IDynamicUserPosition, IDynamicReticleItems {
// constants
	private final static String TAG = "UserIconLayer";
	
// members
	private int				mImageScaleMultiplier;
	private int				mMapImageSize;
	boolean					mShowUser;
	World2DrawingTransformer mWorld2DrawingTransformer= null;
	float					mCurrentUserHeading = -1.f;
	float					mCurrentUserPitch = -1.f;
	PointXY					mCurrentUserPosition = null;
	Bitmap					mUserIcon = null;
	Matrix					mDrawTransformMatrix = new Matrix();
	Matrix					mUserIconRotationMatrix = new Matrix();
	float[] 				mUserPosOnScreen = new float[4];
	Bitmap					mUserReticuleIcon = null;
	volatile PointXY 		mDrawingUserPosition = new PointXY(0,0);
	MeshGL					mUserIconMesh;
	float					mUserIconScaleFactor;
	private	RenderSchemeManager mRSM;

	//camera variables
	private PointXY			onDrawCameraViewportCurrentCenter;
	private PointXY			mDrawingCameraViewportCenter;
	
	//GL varibles
	private float[]			mGLOffset;
	private float[]			mUserIconModelMatrix;
	
	
	boolean					thisIsTheFirstRun = true;
	
// methods
	public UserIconLayer(Activity parentActivity, RenderSchemeManager rsm, World2DrawingTransformer world2DrawingTransformer, BackgroundSize bkgdSize) throws Exception {
		super(parentActivity, "UserIcon", rsm);
		mWorld2DrawingTransformer = world2DrawingTransformer;
		mShowUser = false;
        mUserIcon = rsm.GetUserIconBitmap();
        mRSM = rsm;

		mUserReticuleIcon = BitmapFactory.decodeResource(mParentActivity.getResources(), R.drawable.user_arrow_reticle);
		
		switch (bkgdSize) {
			case NORMAL: {
				mImageScaleMultiplier = 2;
				break;
			}
			case MINIMAL: {
				mImageScaleMultiplier = 1;
				break;
			}
		}
		mMapImageSize = 512 * mImageScaleMultiplier;
		onDrawCameraViewportCurrentCenter = new PointXY(0f, 0f);
		mDrawingCameraViewportCenter = new PointXY(0f, 0f);
		mGLOffset = new float[4];
		mUserIconModelMatrix = new float[16];
	}

	public void ShowUserPosition(boolean showUser) {
		mShowUser = showUser;
	}

	public boolean isUserPositionShown() {
		return mShowUser;
	}

	@Override
	public void SetUserHeading(float heading) {
		mCurrentUserHeading = heading;
	}
	
	@Override
	public void SetUserPitch(float pitch){
		mCurrentUserPitch = pitch;
	}
	
	@Override
	public void SetUserPosition(float longitude, float latitude) {
		mCurrentUserPosition = new PointXY(longitude, latitude);
	}

	private boolean haveRequiredUserData() {
		return (mCurrentUserHeading >= 0.f && mCurrentUserHeading < 360.f && mCurrentUserPosition != null);
	}

	@Override
	public boolean CheckForUpdates() {
		return false;
	}
	
	@Override
	public boolean IsReady() {
		return true;   //		if don't have data, just don't draw user yet... if return haveRequiredUserData(), might lockout mapview from showing map until GPS is fixed
	}


	@Override
	public void Draw(Canvas canvas, CameraViewport camera, String focusedObjectID, Resources res) {
		// @Overide in subclass
		if(mEnabled && mShowUser && camera != null && mWorld2DrawingTransformer != null && haveRequiredUserData()) {
			//			Log.e(TAG, "in Draw...........................");

			PointXY drawingViewportCenter = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(new PointXY((float)camera.mCurrentLongitude, (float) camera.mCurrentLatitude));
			mDrawingUserPosition = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(new PointXY(mCurrentUserPosition.x, mCurrentUserPosition.y));
			
			mDrawTransformMatrix.setTranslate(-(float)drawingViewportCenter.x,-(float)drawingViewportCenter.y);
			mDrawTransformMatrix.postScale(1.0f/camera.mCurrentAltitudeScale, 1.0f/camera.mCurrentAltitudeScale);
			mDrawTransformMatrix.postRotate(-camera.mCurrentRotationAngle);
			mDrawTransformMatrix.postTranslate((float)camera.mScreenWidthInPixels/2.0f, (float)camera.mScreenHeightInPixels/2.0f);

			// last, draw user icon
			float[] vcp = new float[2];
			vcp[0] = (float)drawingViewportCenter.x;
			vcp[1] = (float)drawingViewportCenter.y;
			mDrawTransformMatrix.mapPoints(vcp);

			mUserPosOnScreen[0] = (float)mDrawingUserPosition.x;
			mUserPosOnScreen[1] = (float)mDrawingUserPosition.y;
			mDrawTransformMatrix.mapPoints(mUserPosOnScreen);

			if(camera.mUserTestBB.contains(mUserPosOnScreen[0], mUserPosOnScreen[1]))  {
				mUserIconRotationMatrix.setTranslate(-mUserIcon.getWidth()/2, -mUserIcon.getHeight()/2);
				if(!camera.mCameraRotateWithUser) {
					mUserIconRotationMatrix.postRotate(mCurrentUserHeading-camera.mCurrentRotationAngle);
				}
				mUserIconRotationMatrix.postTranslate(mUserPosOnScreen[0], mUserPosOnScreen[1]);
				canvas.drawBitmap(mUserIcon, mUserIconRotationMatrix, null);	// rotated user icon
			}
		}
	}

	// reticle support
	@Override
	public ArrayList<ReticleItem> GetReticleItems(CameraViewport camera, float withinDistInM) {	// assumes this is called after Draw
		ArrayList<ReticleItem> itemList = new ArrayList<ReticleItem>();
		if(mEnabled && mShowUser && camera != null && mWorld2DrawingTransformer != null && haveRequiredUserData()) {
			float xOffset = mGLOffset[0]/2f;
			float yOffset = mGLOffset[1]/2.3f;
			if(!camera.mUserTestBB.contains(xOffset, -yOffset))  {  
				itemList.add(new ReticleItem(mUserReticuleIcon, new PointXY(xOffset, -yOffset), true));  				// add user icon to reticle if user offscreen
			}
		}
		return itemList;
	}
	
	@Override
	public void Draw(RenderSchemeManager rsm, Resources res, CameraViewport camera, String focusedObjectID, boolean loadNewTexture, MapViewMode viewMode, GLText glText) {		// for OpenGL based rendering
		// @Overide in subclass
		if(mEnabled) {
			float viewScale = camera.mCurrentAltitudeScale;
			onDrawCameraViewportCurrentCenter.x = (float)camera.mCurrentLongitude;
			onDrawCameraViewportCurrentCenter.y = (float)camera.mCurrentLatitude;
			mDrawingCameraViewportCenter = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(onDrawCameraViewportCurrentCenter);
			mDrawingUserPosition = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(mCurrentUserPosition);

			MeshGL userArrowMesh = rsm.getMeshOfType(MeshGL.MeshType.USER_ICON.ordinal());
			Bitmap userIconBitmap = rsm.GetUserIconBitmap();

			if(viewMode == MapViewMode.NORMAL){
				// when in neither explore mode nor find mode, i.e. User Arrow icon is always at the 
				// center of the screen, don't do any offset
				mGLOffset[0] = 0;
				mGLOffset[1] = 0;
				mGLOffset[2] = 0f;
				mGLOffset[3] = 0f;
			}
			else {
				// If in find/explore mode, offset the user icon by it's drawing coordinates 
				// (which are based on the user's real GPS coordinates) minus the 
				// Camera viewport center, which is the drawing coordinate location of
				// the reticle center
				mGLOffset[0] = (mDrawingUserPosition.x-mDrawingCameraViewportCenter.x) / viewScale;
				mGLOffset[1] = -(mDrawingUserPosition.y-mDrawingCameraViewportCenter.y) / viewScale;
				mGLOffset[2] = 0f;
				mGLOffset[3] = 0f;
			}
			
			//scaling and translating the UserIcon Location vector
			android.opengl.Matrix.setIdentityM(mUserIconModelMatrix, 0);
			android.opengl.Matrix.rotateM(mUserIconModelMatrix, 0, 
					(viewMode == MapViewMode.FIND_MODE) ? 0f : camera.mCurrentRotationAngle,  0f, 0f, 1f);
			android.opengl.Matrix.scaleM(mUserIconModelMatrix, 0, 2, 2, 2);
			android.opengl.Matrix.multiplyMV(mGLOffset, 0, mUserIconModelMatrix, 0, mGLOffset, 0);

			//----------- Draw user arrow mesh ----------------
			if(camera.mUserTestBB.contains(mGLOffset[0]/2f, -mGLOffset[1]/2.3f)){
				//scaling and translating the Mesh
				mUserIconScaleFactor = camera.getMaxZoomScale() * mMapImageSize/32;
				android.opengl.Matrix.setIdentityM(userArrowMesh.mModelMatrix, 0);
				if((viewMode != MapViewMode.NORMAL)){ // MapViewMode is in FIND_MODE or EXPLORE_MODE
					android.opengl.Matrix.translateM(userArrowMesh.mModelMatrix, 0, mGLOffset[0], mGLOffset[1], mGLOffset[2]);
					if(viewMode == MapViewMode.FIND_MODE) android.opengl.Matrix.rotateM(userArrowMesh.mModelMatrix, 0, -mCurrentUserHeading, 0, 0, 1f);
					else android.opengl.Matrix.rotateM(userArrowMesh.mModelMatrix, 0, camera.mCurrentRotationAngle - mCurrentUserHeading, 0, 0, 1f);
				}
				android.opengl.Matrix.scaleM(userArrowMesh.mModelMatrix, 0, 
						mUserIconScaleFactor, mUserIconScaleFactor, mUserIconScaleFactor);
				android.opengl.Matrix.translateM(userArrowMesh.mModelMatrix, 0, 0f, 0f, ((viewMode == MapViewMode.EXPLORE_MODE)) ? 0.25f : 0.9f);
				
				userArrowMesh.loadMeshTexture(userIconBitmap, thisIsTheFirstRun, 1);
//				userArrowMesh.enableAlphaTesting = true;
//				userArrowMesh.enableRadialGradient = false;
				userArrowMesh.drawMesh(camera.mCamViewMatrix, camera.mCamProjMatrix);
			}
			
			if(thisIsTheFirstRun) thisIsTheFirstRun = false;
		}
	}
	
	public PointXY getUserArrowDrawingPosition(){
		return mDrawingCameraViewportCenter;
	}
	
	public PointXY getCurrentUserPosition(){
		return mCurrentUserPosition;
	}

}
