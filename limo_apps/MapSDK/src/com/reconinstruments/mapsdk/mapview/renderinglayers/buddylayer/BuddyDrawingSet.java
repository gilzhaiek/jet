//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.mapsdk.mapview.renderinglayers.buddylayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.RemoteException;
import android.util.Log;

import com.reconinstruments.mapsdk.R;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoDataServiceState;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataService;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.IGeodataServiceResponse;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.objecttype.GeoBuddyInfo;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Buddy;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;
import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager;
import com.reconinstruments.mapsdk.mapview.WO_drawings.RenderSchemeManager.BitmapTypes;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.renderinglayers.DrawingSet;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.MapRLDrawingSet;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;
import com.reconinstruments.mapsdk.mapview.renderinglayers.texample3D.GLText;

public class BuddyDrawingSet extends DrawingSet {
    // constants
    private final static String         TAG = "BuddyDrawingSet";
    private static long REMOVE_BUDDY_TIME_MS = 300000;                 // 5 mins
    private static long FADE_OFFLINE_BUDDY_TIME_MS = 300000;    // 5 min
    private static float BUDDY_ON_RETICLE_DISTANCE_IN_M = 10000;        // 10 km
    private static float MAX_BUDDY_DETECT_DISTANCE_IN_M = 10000;        // 10 km
        
    // members
    public ArrayList<MapBuddyInfo>      mCurrentBuddyList0 = new ArrayList<MapBuddyInfo>();
    public ArrayList<MapBuddyInfo>      mCurrentBuddyList1 = new ArrayList<MapBuddyInfo>();
    public  World2DrawingTransformer    mWorld2DrawingTransformer = null;
    BuddyLayer mParent = null;
    public  static IGeodataService      mGeodataServiceInterface  = null;
    GeoDataServiceState                         mGeodataServiceState = null;
    public CameraViewport                       mCameraViewport = null;
        
    // predefined classes to speed up rendering
    PointXY                                     onDrawCameraViewportCurrentCenter = new PointXY(0.f, 0.f);
    PointXY                             mDrawingCameraViewportCenter =  new PointXY(0.f, 0.f);
    RectF                                       mDrawingCameraViewportBoundary = new RectF(); 
    RectF                                       mDrawingLoadingArea = new RectF(); 
    Matrix                                      mPOIDrawTransform = new Matrix();
    Bitmap                                      mBuddyReticuleIcon = null;
        
    private MapRLDrawingSet mMapRLDrawingSet;
    private int                         mMapImageSize;
    private boolean             thisIsTheFirstRun = true;
    private float                       mRandZ = 0;
    private float[]                     mTransformedLocation;
    private float[]                     mapModelMatrix;
    private float[]                     mPOIOffset;
        
    // creator  / init / release        
    public BuddyDrawingSet(BuddyLayer parent, RenderSchemeManager rsm, World2DrawingTransformer world2DrawingTransformer) {
        super(rsm);
                
        mWorld2DrawingTransformer = world2DrawingTransformer;
        mParent = parent;
                
        mBuddyReticuleIcon =BitmapFactory.decodeResource(parent.mParentActivity.getResources(), R.drawable.buddy_reticule_icon);

        mTransformedLocation = new float[4];
        mapModelMatrix = new float[16];
        mPOIOffset = new float[16];
    }
        
    public void setMapImageSize(int imageSize){
        mMapImageSize = imageSize;
    }
        
    public void UpdateBuddies(Context context) {        // called on separate thread from Draw(), thus the double buffering to avoid data conflicts/delays in the draw() cycle
                
        if(mGeodataServiceInterface != null) {
            IGeodataServiceResponse rc;
            Log.d(TAG, "UpdateBuddies");
            try {
                rc = mGeodataServiceInterface.getBuddies();
                //                              Log.i(TAG, "rc reponseCode= " + rc.mResponseCode.toString() + "   >>>>>>>>  " + ((rc.mBuddyArray == null) ? "buddyArray null" : ""));
                if(rc.mResponseCode == IGeodataServiceResponse.ResponseCodes.BUDDYREQUEST_BUDDIES_ATTACHED) {

                    if(rc.mBuddyArray != null) { // shouldn't happen... but just in case something screws up in geodata service
        
                        ArrayList<MapBuddyInfo> currentBuddyList = GetCurrentBuddyList();
                        ArrayList<MapBuddyInfo> nextBuddyList = new ArrayList<MapBuddyInfo>();
        
                        // removed expired buddies
                        for(MapBuddyInfo curBuddy : currentBuddyList)   {       
                            if(!curBuddy.mExpired) {
                                nextBuddyList.add(curBuddy);
                            }
                        }

                        // process latest items from GeodataService
                        for(GeoBuddyInfo buddy : rc.mBuddyArray) {
                            //                                  mCurrentBuddies.add(new MapBuddyInfo(buddy.mID, buddy.mName, buddy.mLatitude, buddy.mLongitude, System.currentTimeMillis(), mWorld2DrawingTransformer);

                            boolean notInCurrentBuddies = true;
                            for(MapBuddyInfo curBuddy : currentBuddyList)       {       
                                if(curBuddy != null) {
                                    Buddy curBuddyObj = (Buddy) curBuddy.mBuddyDrawing.mDataObject;

                                    if(curBuddyObj.mName != null) { 
                                        if (curBuddyObj.mName.equals(buddy.mName) )     {       // if existing buddy, update info... assumes later data is in array
                                            curBuddyObj.mGPSLocation = new PointXY((float)buddy.mLongitude, (float)buddy.mLatitude);
                                            curBuddy.mLastUpdateTime = System.currentTimeMillis();
                                            curBuddy.mOnline = true;
                                            curBuddy.mExpired = false;
                                            notInCurrentBuddies = false;
                                            Log.d(TAG, "updating buddy=" + buddy.mName + "," + buddy.mLongitude +  ", " + buddy.mLatitude);
                                            break;      
                                        }
                                    }
                                }
                            }
                            Log.i(TAG, "notInCurrentBuddies = " + notInCurrentBuddies);
                            if(notInCurrentBuddies) {                                   // otherwise add it
                                MapBuddyInfo curBuddy = new MapBuddyInfo(context, buddy.mID, buddy.mName, buddy.mLatitude, buddy.mLongitude, System.currentTimeMillis(), mWorld2DrawingTransformer);
                                nextBuddyList.add(curBuddy);

                                Log.d(TAG, "adding new buddy=" + buddy.mName + "," + buddy.mLongitude +  ", " + buddy.mLatitude);
                            }
                        }
                                                
                        SetNextBuddyList(nextBuddyList);
                        mUpdateAvailable = true;
                    }
                }
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.e(TAG, "Error retrieving buddies from geodata service... -- buddy update ignored");
            } 

        }
    }
        
    @Override
    public void Draw(Canvas canvas, CameraViewport camera, String focusedObjectID, Resources res) {
                
        ArrayList<MapBuddyInfo> currentBuddyList = GetCurrentBuddyList();
        //              Log.e(TAG, "curBuddyList size =" + currentBuddyList.size());
        float viewScale = camera.GetAltitudeScale();

        // set several Drawing coordinate values CameraViewportCenter, CameraViewportBoundary, a test boundary and UserPosition
        onDrawCameraViewportCurrentCenter.x = (float)camera.mCurrentLongitude;
        onDrawCameraViewportCurrentCenter.y = (float)camera.mCurrentLatitude;
        mDrawingCameraViewportCenter = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(onDrawCameraViewportCurrentCenter);

        mDrawingCameraViewportBoundary = mWorld2DrawingTransformer.TransformGPSRectXYToRectF(camera.mGeoRegionToSupportCurrentView.mBoundingBox);
                
        mPOIDrawTransform.setTranslate(-mDrawingCameraViewportCenter.x,-mDrawingCameraViewportCenter.y);
        mPOIDrawTransform.postScale(1.0f/viewScale,1.0f/viewScale);
        mPOIDrawTransform.postRotate(-camera.mCurrentRotationAngle);
        mPOIDrawTransform.postTranslate(camera.mScreenWidthInPixels/2.0f, camera.mScreenHeightInPixels/2.0f);

        MapBuddyInfo focusedBuddy = null;
                
        for(MapBuddyInfo curBuddy : currentBuddyList)   {       
            // before drawing, update all buddy states based on timeout thresholds...   
            if((System.currentTimeMillis() - curBuddy.mLastUpdateTime) > REMOVE_BUDDY_TIME_MS) {        // flag offline buddy as expired after time limit of no updates
                curBuddy.mExpired = true;
            }
            else {
                if((System.currentTimeMillis() - curBuddy.mLastUpdateTime) > FADE_OFFLINE_BUDDY_TIME_MS) {
                    curBuddy.mOnline = false;   // mark buddy as offline after time limit of no updates
                    curBuddy.mState = WorldObjectDrawing.WorldObjectDrawingStates.DISABLED;
                }
                else {
                    curBuddy.mOnline = true;    // mark buddy as online
                    curBuddy.mState = WorldObjectDrawing.WorldObjectDrawingStates.NORMAL;
                }
            }

            //                  Log.e(TAG, "curBuddy =" + curBuddy.mBuddyDrawing.mDataObject.mName );
            if(curBuddy != null && !curBuddy.mExpired) {

                Buddy curBuddyObj = (Buddy) curBuddy.mBuddyDrawing.mDataObject;
                if(curBuddy.mBuddyDrawing.mLocation.x == 0) {
                    curBuddy.mBuddyDrawing.mLocation = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(curBuddyObj.mGPSLocation);
                }
                float mDrawingX = curBuddy.mBuddyDrawing.mLocation.x;
                float mDrawingY = curBuddy.mBuddyDrawing.mLocation.y;
                float[] dl = new float[2];
                dl[0]=mDrawingX;
                dl[1]=mDrawingY;
                mPOIDrawTransform.mapPoints(dl);
                curBuddy.mScreenCoordinates = new PointXY(dl[0],dl[1]);
                                
                if(mDrawingCameraViewportBoundary.contains(mDrawingX, mDrawingY)) {
                    if(focusedObjectID != null && focusedObjectID.equalsIgnoreCase(curBuddyObj.mObjectID)) {
                        focusedBuddy = curBuddy;
                    }
                    else {
                        Bitmap buddyIcon = mRSM.GetPOIBitmap(RenderSchemeManager.BitmapTypes.BUDDY.ordinal(), curBuddy.mState.ordinal(), viewScale);
                        if(buddyIcon != null) {
                            canvas.drawBitmap(buddyIcon, curBuddy.mScreenCoordinates.x - buddyIcon.getWidth()/2+1, curBuddy.mScreenCoordinates.y - buddyIcon.getHeight()/2+1, null);
                        }
                    }
                }
            }
        }
        if(focusedBuddy != null) {      // draw focused object last so rendered on top
            Bitmap buddyIcon = mRSM.GetPOIBitmap(RenderSchemeManager.BitmapTypes.BUDDY.ordinal(), focusedBuddy.mState.ordinal() + 1, viewScale);
            if(buddyIcon != null) {
                float bitmapVertOffsetPercent = 0.33f;
                canvas.drawBitmap(buddyIcon, focusedBuddy.mScreenCoordinates.x - buddyIcon.getWidth()/2+1, focusedBuddy.mScreenCoordinates.y - (int)(buddyIcon.getHeight() * (0.5 + bitmapVertOffsetPercent))+1, null);
            }

        }
    }
        
    public void Draw(RenderSchemeManager rsm, CameraViewport camera, String focusedObjectID, Resources res, boolean loadNewTexture, GLText glText){
        ArrayList<MapBuddyInfo> currentBuddyList = GetCurrentBuddyList();
        float viewScale = camera.mCurrentAltitudeScale;
        float currentPitchAngle = MapRLDrawingSet.mCamCurrentPitchAngle;
        onDrawCameraViewportCurrentCenter.x = (float) camera.mCurrentLongitude;
        onDrawCameraViewportCurrentCenter.y = (float) camera.mCurrentLatitude;
        mDrawingCameraViewportCenter = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(onDrawCameraViewportCurrentCenter);
                
        if (thisIsTheFirstRun) {
            mRandZ = 0.02f;
        }
        MapBuddyInfo focusedBuddy = null;
                
        for(MapBuddyInfo curBuddy : currentBuddyList)   {       
            // before drawing, update all buddy states based on timeout thresholds...   
            if((System.currentTimeMillis() - curBuddy.mLastUpdateTime) > REMOVE_BUDDY_TIME_MS) {        // flag offline buddy as expired after time limit of no updates
                curBuddy.mExpired = true;
            }
            else {
                curBuddy.mOnline = true;    // mark buddy as online
                curBuddy.mState = curBuddy.mBuddyDrawing.mState = WorldObjectDrawing.WorldObjectDrawingStates.NORMAL;
            }

            if(curBuddy != null && !curBuddy.mExpired) {

                Buddy curBuddyObj = (Buddy) curBuddy.mBuddyDrawing.mDataObject;
                curBuddy.mBuddyDrawing.mLocation = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(curBuddyObj.mGPSLocation);

                //calculate POI offset due to panning
                mPOIOffset[0] = (float)(-mDrawingCameraViewportCenter.x)/viewScale;
                mPOIOffset[1] = (float)(mDrawingCameraViewportCenter.y)/viewScale;
                mPOIOffset[2] = 0;
                mPOIOffset[3] = 0;
                                
                //Transforming the Map offset vector and POI offset Vector.
                //Used when panning the camera in Explore mode.
                android.opengl.Matrix.setIdentityM(mapModelMatrix, 0);
                android.opengl.Matrix.rotateM(mapModelMatrix, 0, camera.mCurrentRotationAngle,  0f, 0f, 1f);
                android.opengl.Matrix.scaleM(mapModelMatrix, 0, 2, 2, 2);
                android.opengl.Matrix.multiplyMV(mPOIOffset, 0, mapModelMatrix, 0, mPOIOffset, 0);
                                
                android.opengl.Matrix.setIdentityM(mapModelMatrix, 0);
                android.opengl.Matrix.translateM(mapModelMatrix, 0, 0, camera.mScreenHeightInPixels, 0);
                android.opengl.Matrix.scaleM(mapModelMatrix, 0, 2/viewScale, 2/viewScale, 2/viewScale);
                boolean hasFocus = curBuddy.mBuddyDrawing.mDataObject.mObjectID.equalsIgnoreCase(focusedObjectID);
                if(hasFocus){
                    focusedBuddy = curBuddy;
                }
                else {
                    curBuddy.mBuddyDrawing.Draw(rsm, null, camera, currentPitchAngle, mapModelMatrix, mPOIOffset, mMapImageSize, hasFocus, loadNewTexture);
                }
            }
        }

        //draw focused buddy last
        if(focusedBuddy != null){
            focusedBuddy.mBuddyDrawing.Draw(rsm, null, camera, currentPitchAngle, mapModelMatrix, mPOIOffset, mMapImageSize, true, loadNewTexture);
        }
                
        if(thisIsTheFirstRun) thisIsTheFirstRun = false;
    }

    // reticle support
    public ArrayList<ReticleItem> GetReticleItems(CameraViewport camera, float withinDistInM) {  /// assumed to be called after Draw()
        float viewScale = camera.mCurrentAltitudeScale;
        ArrayList<ReticleItem> itemList = new ArrayList<ReticleItem>();
        for(MapBuddyInfo curBuddy : GetCurrentBuddyList())      {       
            Buddy curBuddyObj = (Buddy) curBuddy.mBuddyDrawing.mDataObject;
            float distToBuddyInM = World2DrawingTransformer.DistanceBetweenGPSPoints(curBuddyObj.mGPSLocation.x, curBuddyObj.mGPSLocation.y, onDrawCameraViewportCurrentCenter.x,  onDrawCameraViewportCurrentCenter.y);
            curBuddy.mBuddyDrawing.mLocation = mWorld2DrawingTransformer.TransformGPSPointToDrawingPoint(curBuddyObj.mGPSLocation);
            float[] locationWithOffset = curBuddy.mBuddyDrawing.getLocationWithOffset();
            if(!camera.mUserTestBB.contains(locationWithOffset[0]/2f, -locationWithOffset[1]/2.3f) 
               && distToBuddyInM < BUDDY_ON_RETICLE_DISTANCE_IN_M && curBuddy.mBuddyDrawing.mLocation != null)  {  
                itemList.add(new ReticleItem(BitmapTypes.BUDDY.ordinal(), curBuddy.mBuddyDrawing.mLocation));                           // add user icon to reticle if user offscreen
            }
        }
        return itemList;
    }

    public ArrayList<POIDrawing> GetFocusableItems() {          
        ArrayList<POIDrawing> resultList = new ArrayList<POIDrawing>();
        for(MapBuddyInfo curBuddy : GetCurrentBuddyList()) {
            resultList.add(curBuddy.mBuddyDrawing);
        }
        return resultList;
    }
        
    @Override
    public boolean SwitchIfUpdateReady() {
        if(mUpdateAvailable) {
            mUpdateAvailable = false;
            mCurIndex = (mCurIndex == 0) ? 1 : 0;
            return true;
        }
        return false;
    }

    // get value methods
    public ArrayList<MapBuddyInfo> GetCurrentBuddyList() {
        if(mCurIndex == 0) {
            //                  if(mCurrentBuddyList0.size() == 0) Log.e(TAG, "???????? Buddy list is EMPTY"); 
            return mCurrentBuddyList0;
        }
        else {
            //                  if(mCurrentBuddyList1.size() == 0) Log.e(TAG, "???????? Buddy list is EMPTY");
            return mCurrentBuddyList1;
        }
    }

    public ArrayList<MapBuddyInfo> GetNextBuddyList() {
        if(mCurIndex == 1) {
            return mCurrentBuddyList0;
        }
        else {
            return mCurrentBuddyList1;
        }
    }

    public void SetNextBuddyList(ArrayList<MapBuddyInfo> buddyList) {
        //              Log.d(TAG, ">>>>> Adding Buddy info");
        if(mCurIndex == 1) {
            mCurrentBuddyList0 = buddyList;
        }
        else {
            mCurrentBuddyList1 = buddyList;
        }
    }
        

        
    class MapBuddyInfoComparator implements Comparator<MapBuddyInfo> {
        public int compare(MapBuddyInfo buddy1, MapBuddyInfo buddy2) {
            return (int) Math.abs(buddy1.mDistToMeInM - buddy2.mDistToMeInM);
        }
    }
        
        
    public ArrayList<BuddyItem> GetSortedBuddyList() {
        float longitude = onDrawCameraViewportCurrentCenter.x;
        float latitude = onDrawCameraViewportCurrentCenter.y;
        return GetSortedBuddyList(longitude, latitude);
                
    }

    /**
     * Sorts the current list of buddies prioritized by distance.
     * @param longitude
     * @param latitude
     * @return a <code>List</code> of buddies sorted by distance
     */
    public ArrayList<BuddyItem> GetSortedBuddyList(float longitude, float latitude) { 
                
        boolean isDebug = false;
        if (isDebug) {//for testing purpose-- hard code some buddies.
            ArrayList<BuddyItem> list = new ArrayList<BuddyItem>();
                          
            list.add(new BuddyItem("Me", new PointXY(longitude, latitude ), 0.0f));
                          
            //                     //701W Georgia St, Vancouver, BC, Seymour/Robson 49.280708, -123.118732
            //                     PointXY p1= new PointXY(-123.118732f, 49.280708f);
            //                     float d1 = World2DrawingTransformer.DistanceBetweenGPSPoints(p1.x, p1.y, longitude, latitude);
            //                     BuddyItem i1= new BuddyItem("Simon-1", p1, d1);
            //                     list.add(i1);
            //                     
            //                     
            //                     //1188 Howe St, Vancouver, BC V6Z, 49.277866, -123.127186, Seymore +Davie
            //                     PointXY p2= new PointXY(-123.127186f, 49.277866f);
            //                     float d2 = World2DrawingTransformer.DistanceBetweenGPSPoints(p2.x, p2.y, longitude, latitude);
            //                     BuddyItem i2= new BuddyItem("Simon-2", p2, d2);
            //                     list.add(i2);
            //                     
            //                     
            //                     //1066 Hastings St W, Vancouver, BC 49.287916, -123.119741, Thurlow + hastings
            //                     PointXY p3= new PointXY(-123.119741f, 49.287916f);
            //                     float d3 = World2DrawingTransformer.DistanceBetweenGPSPoints(p3.x, p3.y, longitude, latitude);
            //                     BuddyItem i3= new BuddyItem("Simon-3", p3, d3);
            //                     list.add(i3);
                          
            //whistler buddy 1
            PointXY p1= new PointXY(-122.948620f, 50.114912f);
            float d1 = World2DrawingTransformer.DistanceBetweenGPSPoints(p1.x, p1.y, longitude, latitude);
            BuddyItem i1= new BuddyItem("Bob", p1, d1);
            list.add(i1);
                           
                           
            //whistler lost lake lodge
            PointXY p2= new PointXY(-122.934923f, 50.122200f);
            float d2 = World2DrawingTransformer.DistanceBetweenGPSPoints(p2.x, p2.y, longitude, latitude);
            BuddyItem i2= new BuddyItem("Simon", p2, d2);
            list.add(i2);
                           
                           
            //whistler catholic church
            PointXY p3= new PointXY(-122.970955f, 50.126586f);
            float d3 = World2DrawingTransformer.DistanceBetweenGPSPoints(p3.x, p3.y, longitude, latitude);
            BuddyItem i3= new BuddyItem("Joe", p3, d3);
            list.add(i3);
                           
                           
            return  list;
        }
                   

        ArrayList<MapBuddyInfo> currentBuddyList = GetCurrentBuddyList();

        ArrayList<MapBuddyInfo> currentBuddyList1 = new ArrayList<MapBuddyInfo>();

        for (MapBuddyInfo curBuddy : currentBuddyList) {
            if (curBuddy == null || curBuddy.mExpired) {
                continue;
            }

            Buddy curBuddyObj = (Buddy) curBuddy.mBuddyDrawing.mDataObject;
            if (curBuddyObj == null)
                continue;

            float distToBuddyInM = World2DrawingTransformer.DistanceBetweenGPSPoints(curBuddyObj.mGPSLocation.x, curBuddyObj.mGPSLocation.y, longitude,
                                                                                     latitude);
            curBuddy.mDistToMeInM = distToBuddyInM;
            currentBuddyList1.add(curBuddy);
        }

        // sort the arraly list
        Collections.sort(currentBuddyList1, new MapBuddyInfoComparator());

        ArrayList<BuddyItem> itemList = new ArrayList<BuddyItem>();
        itemList.add(new BuddyItem("Me", new PointXY(longitude, latitude ), 0.0f));
        int i = 0;
        for (MapBuddyInfo curBuddy : currentBuddyList1) {
            Buddy curBuddyObj = (Buddy) curBuddy.mBuddyDrawing.mDataObject;
                        
            if(curBuddy.mDistToMeInM <= MAX_BUDDY_DETECT_DISTANCE_IN_M){
                Log.d(TAG, "No." + i + ", buddy=" + curBuddyObj.mName + ", distance=" + curBuddy.mDistToMeInM + ", location=" + curBuddyObj.mGPSLocation.x + ", "
                      + curBuddyObj.mGPSLocation.y);
        
                itemList.add(new BuddyItem(curBuddyObj.mName, curBuddyObj.mObjectID, curBuddyObj.mGPSLocation, curBuddy.mDistToMeInM));
            }
        }

        return itemList;
                
    }//GetSortedBuddyList

    public void setMapDrawingSetCallback(MapRLDrawingSet mapDrawingSet){
        mMapRLDrawingSet = mapDrawingSet;
    }

}
