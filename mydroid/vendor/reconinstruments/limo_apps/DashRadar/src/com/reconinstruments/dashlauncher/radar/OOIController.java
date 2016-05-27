package com.reconinstruments.dashlauncher.radar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.reconinstruments.connect.messages.BuddyInfoMessage.BuddyInfo;
import com.reconinstruments.dashlauncher.radar.maps.drawings.POIDrawing;
import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;
import com.reconinstruments.dashlauncher.radar.maps.objects.POI;
import com.reconinstruments.dashlauncher.radar.prim.Trapezoid;
import com.reconinstruments.dashlauncher.radar.prim.Vector3;
import com.reconinstruments.dashlauncher.radar.render.CommonRender;
import com.reconinstruments.dashlauncher.radar.render.GLDistanceCircle;
import com.reconinstruments.dashlauncher.radar.render.GLGlobalIconRenderer;
import com.reconinstruments.dashlauncher.radar.render.GLGlobalTextRenderer;
import com.reconinstruments.dashlauncher.radar.render.GLMapObject;
import com.reconinstruments.dashlauncher.radar.render.GLPoi;
import com.reconinstruments.dashlauncher.radar.render.OOIGrouper;

public class OOIController extends GLMapObject
{
	protected static final String TAG = "OOIController";
	
	protected static final float CIRCLE_FTARGET_X 			= 0.0f;
	protected static final float CIRCLE_FTARGET_Y 			= 0.27f;
	protected static final float CIRCLE_FTARGET_MAX_DIST	= 0.27f;
	
	protected ArrayList<RadarOOI> mBOIs			= null;		// Buddies
	protected ArrayList<RadarOOI> mBundledPOIs	= null;		// POIs
	protected ArrayList<RadarOOI> mBundledBOIs	= null;		// Bundled Buddies
	protected SortedMap<Float, RadarOOI> mFocusedOOIs = null;
	protected SortedMap<Float, RadarOOI> mSortedOOIs = null;
	protected OOIGrouper mOOIGrouper = null;
	//protected OOIGrouper mRadarOOIGrouper = null;
	
	protected Trapezoid	mFocusTrapezoid = null;
	protected Vector3	mFocusPoint 	= null;
	protected Vector3	mCamLocation	= new Vector3();
	protected Vector3 	mUserLocation	= new Vector3(); 
	
	private Location mUserWorldLoc  = new Location("");
	
	private float		mMaxDistanceToDraw		= 0;
	protected GLGlobalTextRenderer mGLDynamicText = null;
	protected GLGlobalIconRenderer mGLDynamicIcon = null;
	
	protected GLDistanceCircle mCircularDrawArea = null;
	protected Vector3  mDrawAreaOrigin	= new Vector3();
	protected Point3f  pTemp			= new Point3f();
	protected Matrix3f mTemp			= new Matrix3f();
	
	protected boolean mBuddiesEnabled	= true;
	protected boolean mRestEnabled		= false;
	protected boolean mLiftsEnabled		= false;
	
	protected RadarOOI mFocusOOI		= null;
	
	protected LocationTransformer mLocationTransformer 	= null;
	
	protected boolean mIsRadarMode = true;
	
	protected Context mContext = null;

	public OOIController(LocationTransformer locationTransformer, GLGlobalTextRenderer glDynamicText, GLGlobalIconRenderer glDynamicIcon, GLDistanceCircle circularDrawArea, Context context) 
	{
		mContext = context;
		mLocationTransformer = locationTransformer;
		
		mGLDynamicText = glDynamicText;
		mGLDynamicIcon = glDynamicIcon;		
		
		mCircularDrawArea = circularDrawArea;
		
		mBundledPOIs = new ArrayList<RadarOOI>();
		mBOIs = new ArrayList<RadarOOI>();
		mBundledBOIs = new ArrayList<RadarOOI>();
		mSortedOOIs = new TreeMap<Float, RadarOOI>();
		mFocusedOOIs = new TreeMap<Float, RadarOOI>();
		mOOIGrouper = new OOIGrouper(mContext, mGLDynamicText, mGLDynamicIcon);
		//mRadarOOIGrouper = new OOIGrouper(mContext, mGLDynamicText, mGLDynamicIcon);
		
		BuddyRefreshThread buddyRefreshThread = new BuddyRefreshThread();
		buddyRefreshThread.setPriority(Thread.MIN_PRIORITY);
		buddyRefreshThread.start();			
	}
	
	public void SetBuddiesEnabled(boolean enabled) {	
		mBuddiesEnabled = enabled;
	}

	public void SetRestEnabled(boolean enabled) {	
		mRestEnabled = enabled;	
	}

	public void SetLiftsEnabled(boolean enabled) {	
		mLiftsEnabled = enabled;	
	}
	
	public void SetPOIs(ArrayList<POIDrawing> poiDrawings)
	{
		mBundledPOIs.clear();
		
		ArrayList<RadarOOI> tmpPOIs = new ArrayList<RadarOOI>();
		
		for(int index = 0; index < poiDrawings.size(); index++)
		{
			POIDrawing poiDrawing = poiDrawings.get(index);			
			int poiType = poiDrawing.mPoi.Type;
			
			// Only adds buddy, restaurant and chair-lift POI's JIRA - MODLIVE-556
			if(poiType == POI.POI_TYPE_BUDDY || poiType == POI.POI_TYPE_CHAIRLIFTING || poiType == POI.POI_TYPE_RESTAURANT)
			{
				Vector3 poiVector3 = new Vector3((float) poiDrawing.mLocation.x, (float) poiDrawing.mLocation.y, 0);
	
				// Log.v(TAG, "["+poiDrawing.mPoi.Name+"] - x="+poiDrawing.mLocation.x+" y="+poiDrawing.mLocation.y);
				GLPoi glIcon = new GLPoi(poiDrawing.mPoi.Type, poiDrawing.mPoi.Name, poiVector3, mContext);
	
				Location worldLoc = new Location("");
				worldLoc.setLongitude(poiDrawing.mPoi.Location.x);
				worldLoc.setLatitude(poiDrawing.mPoi.Location.y);
				
				RadarOOI poi = new RadarOOI(worldLoc, poiVector3, glIcon, poiDrawing.mPoi.Type);
				poi.mIcon.setGLDrawObjects(mGLDynamicText, mGLDynamicIcon);
				tmpPOIs.add(poi);
			}
		}
		
		mOOIGrouper.SetPOIs(tmpPOIs, false);
		//mOOIGrouper.DebugPrintMatrices();
	}
	
	public void UpdateBuddies(Bundle bundle, Location userLocation, float maxDistance, Context context)
	{
		synchronized (mBOIs) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			ArrayList<Bundle> buddyInfoBundleList = (ArrayList)bundle.getParcelableArrayList("BuddyInfoBundle");
			
			//Log.i(TAG, "buddyBundle.length: " + buddyInfoBundleList.size() + " mOOIsLength: " + mOOIs.size());
			
			for(Bundle buddyInfoBundle : buddyInfoBundleList)
			{
				Location buddyLocation = (Location) buddyInfoBundle.getParcelable("location");
				
				BuddyInfo buddyInfo  = new BuddyInfo(
						buddyInfoBundle.getInt("id"),
						buddyInfoBundle.getString("name"),
						buddyLocation.getLatitude(),
						buddyLocation.getLongitude());
				
				//Log.i(TAG, "buddyInfoID=" + buddyInfo.localId + ", name=" + buddyInfo.name +  ", location=" + buddyInfo.location.getLatitude() + " " + buddyInfo.location.getLongitude());
	
				if(buddyInfo != null && buddyInfo.location != null && userLocation != null)
				{
					// Get local coordinates for each buddy 'b'
					float localX = (float)mLocationTransformer.TransformLongitude(buddyInfo.location.getLongitude());
					float localY = (float)mLocationTransformer.TransformLatitude(buddyInfo.location.getLatitude());
					
					// Try to find an existing buddy
					RadarBuddy buddyOOI = GetBuddy(buddyInfo.localId);
					if(buddyOOI != null) // If buddy exists, update it
					{
						//Log.i(TAG, "Updating buddy[" + buddyOOI.mName + "]");
						buddyOOI.Update(buddyInfo.location, userLocation.distanceTo(buddyInfo.location), localX, localY, 0);
					}
					else
					{
						//Log.i(TAG, "Adding buddy[" + buddyInfo.name + "]");
						AddBuddy(buddyInfo, localX, localY, userLocation.distanceTo(buddyInfo.location), context);
					}
				}
			}
			
			if(!mIsRadarMode) {
				mOOIGrouper.SetBuddies(mBOIs, false);
			}
		}
	}
	
	
	private RadarOOI GetOOI(ArrayList<RadarOOI> oois, int index)
	{		
		RadarOOI retOOI = null;
		try {
			
			if (index >= 0 && index < oois.size())
			{
				retOOI = oois.get(index);
			}
		} catch (Exception e) { retOOI = null;}
		
		return retOOI;
	}
	
	public void SetParams(Trapezoid focusTrapezoid, Vector3 focusPoint, Vector3 camLocation, PointF userLocation, Location userWorldLoc, float maxDistanceToDraw)
	{
		mFocusTrapezoid = focusTrapezoid;
		mFocusPoint = focusPoint;
		mUserWorldLoc.set(userWorldLoc);
		mMaxDistanceToDraw = maxDistanceToDraw;
		mCamLocation.set(camLocation);	
		mUserLocation.set(userLocation.x, userLocation.y, 0.0f);
		
		if(!mIsRadarMode ) { // || (mOOIs.size() == 0
			mOOIGrouper.SetPOIGroups(mCamLocation, mBundledPOIs);
			mOOIGrouper.SetBOIGroups(mCamLocation, mBundledBOIs, mIsRadarMode);
		} 
	}	
	
    private float GetWorldDistanceFromUser(RadarOOI radarOOI)
    {
    	return mUserWorldLoc.distanceTo(radarOOI.mWorldLoc);
    }  
    
	private float DistanceFromCamLocation(RadarOOI radarOOI){
		//Log.v(TAG,radarOOI.mIcon.mName+" "+radarOOI.GetPosition().x+","+radarOOI.GetPosition().y+" - "+mFocusPoint.x+","+mFocusPoint.y);
		return Util.GetLocalDistanceBetweenPositions(radarOOI.GetPosition(), mCamLocation);
	}
	
	private float DistanceFromFocusPoint(RadarOOI radarOOI){
		//Log.v(TAG,radarOOI.mIcon.mName+" "+radarOOI.GetPosition().x+","+radarOOI.GetPosition().y+" - "+mFocusPoint.x+","+mFocusPoint.y);
		return Util.GetLocalDistanceBetweenPositions(radarOOI.GetPosition(), mFocusPoint);
	}
	
	private float DistanceBetweenCamToUser() {
		return Util.GetLocalDistanceBetweenPositions(mUserLocation, mCamLocation);
	}
	
	private void UpdateFocusOOI() {
		// Find the last OOI that is hasn't finished the scorlling - sorted by distance
		Iterator ooisIterator = mFocusedOOIs.entrySet().iterator();
		while(ooisIterator.hasNext())
		{
			Map.Entry mapOOI =(Map.Entry)ooisIterator.next();
			RadarOOI radarOOI = ((RadarOOI)mapOOI.getValue());
			if(!radarOOI.FinishedGroupScroll()) {
				mFocusOOI = radarOOI;
				//Log.v(TAG,radarOOI.mIcon.mName+" is mFocusOOI");
				return;
			}
		}

		// All groups were scrolled - starting from the closest while reseting all the OOIs
		boolean firstKey = true;
		ooisIterator = mFocusedOOIs.entrySet().iterator();
		while(ooisIterator.hasNext())
		{
			Map.Entry mapOOI =(Map.Entry)ooisIterator.next();
			RadarOOI radarOOI = ((RadarOOI)mapOOI.getValue());
			radarOOI.ResetScroll(); // Reset all the OOIs in the group
			if(firstKey) {
				mFocusOOI = radarOOI;
				firstKey = false;
				//Log.v(TAG,mFocusOOI.mIcon.mName+" reset to mFocusOOI");	
			}
		}
	}
	
	private void UpdateFocusedOOIs()
	{
		mFocusOOI = null;
		mFocusedOOIs.clear();
		
		float maxDistance = Util.GetLocalDistanceBetweenPositions(mFocusTrapezoid.Get(Trapezoid.TOP_LEFT), new PointF(mFocusPoint.x, mFocusPoint.y));
		
		/*Log.v(TAG,"maxDistance="+maxDistance+" " + mFocusTrapezoid.Get(Trapezoid.TOP_LEFT).x + "," + mFocusTrapezoid.Get(Trapezoid.TOP_LEFT).y  
				+" - "+ mFocusTrapezoid.Get(Trapezoid.TOP_RIGHT).x+","+ mFocusTrapezoid.Get(Trapezoid.TOP_RIGHT).y
				+" - " +mFocusTrapezoid.Get(Trapezoid.BOTTOM_RIGHT).x+"," +mFocusTrapezoid.Get(Trapezoid.BOTTOM_RIGHT).y
				+" - " +mFocusTrapezoid.Get(Trapezoid.BOTTOM_LEFT).x+"," +mFocusTrapezoid.Get(Trapezoid.BOTTOM_LEFT).y);*/
		
		for(int index = 0; index < mBundledPOIs.size(); index++)
		{
			RadarOOI radarOOI = GetOOI(mBundledPOIs, index);
			if(!DrawOOI(radarOOI))  { continue;}
			
			float poiToFocusPoint = DistanceFromFocusPoint(radarOOI);
			
			//Log.v(radarOOI.mIcon.mName,poiToFocusPoint+" "+radarOOI.GetPosition().x+","+radarOOI.GetPosition().y);
			
			if(poiToFocusPoint <= maxDistance) {
				if(mFocusTrapezoid.Contains(radarOOI.GetPosition().x, radarOOI.GetPosition().y)) {
					mFocusedOOIs.put(poiToFocusPoint, radarOOI);
					//Log.v(TAG,radarOOI.mIcon.mName+" poiToFocusPoint="+poiToFocusPoint+" finishedGroup="+radarOOI.FinishedGroupScroll());
				}
			}
		}
			
		for(int index = 0; index < mBundledBOIs.size(); index++)
		{
			RadarOOI radarOOI = GetOOI(mBundledBOIs, index);
			if(!DrawOOI(radarOOI))  { continue;}
			
			float poiToFocusPoint = DistanceFromFocusPoint(radarOOI);
			if(poiToFocusPoint <= maxDistance) {
				if(mFocusTrapezoid.Contains(radarOOI.GetPosition().x, radarOOI.GetPosition().y)) {
					mFocusedOOIs.put(poiToFocusPoint, radarOOI);
					//Log.v(TAG,radarOOI.mIcon.mName+" boiToFocusPoint="+poiToFocusPoint+" finishedGroup="+radarOOI.FinishedGroupScroll());
				}
			}
		}	
		
		UpdateFocusOOI();
	}
	
	public boolean HasBuddies(){
		return (mBOIs.size() > 0);
	}

	public void SetRadarMode(boolean radarMode)
	{
		if(!radarMode && mIsRadarMode) {  // Entring Map moide
			mOOIGrouper.SetBuddies(mBOIs, false);
		}
		mIsRadarMode = radarMode;
	}
	
	public void drawGL(GL10 gl)
	{
		if(mIsRadarMode)
		{
			DrawRadarOOIs(gl);
		}
		else
		{
			DrawMapOOIs(gl);
		}
	}
	
	protected boolean DrawOOI(RadarOOI radarOOI) {
		if(radarOOI == null) return false;
		
		if(!mBuddiesEnabled	&& radarOOI.mType == POI.POI_TYPE_BUDDY) 		{ return false; }
		if(!mRestEnabled	&& radarOOI.mType == POI.POI_TYPE_RESTAURANT) 	{ return false; }
		if(!mLiftsEnabled	&& radarOOI.mType == POI.POI_TYPE_CHAIRLIFTING) { return false; }
		
		return true;
	}

	public void DrawMapOOIs(GL10 gl)
	{
		if(mBundledBOIs.size() == 0 && mBundledPOIs.size() == 0){ return; }
		
		mSortedOOIs.clear();

		UpdateFocusedOOIs();
		
		for (int i = 0; i < mBundledPOIs.size(); i++)
		{
			RadarOOI radarOOI = GetOOI(mBundledPOIs, i);
			if(!DrawOOI(radarOOI)) { continue;}
			
			mSortedOOIs.put(mMaxDistanceToDraw-DistanceFromCamLocation(radarOOI), radarOOI);
		}

		for (int i = 0; i < mBundledBOIs.size(); i++)
		{
			RadarOOI radarOOI = GetOOI(mBundledBOIs, i);
			if(!DrawOOI(radarOOI)) { continue;}
			
			// Only if it is a single buddy - than change to not fresh icon
			if(!radarOOI.IsGroup()) {
				radarOOI.SetDrawParams(((RadarBuddy)radarOOI.mRadarOOIs.get(0)).mSecondsSinceLastUpdate < CommonRender.BUDDY_NOT_FRESH_SECONDS);
			}
			
			mSortedOOIs.put(mMaxDistanceToDraw-DistanceFromCamLocation(radarOOI), radarOOI);
		}
	
		float distanceBetweenCamToUser = DistanceBetweenCamToUser();
		float solidPOIArea = CommonRender.ALWAYS_SOLID_POI_AREA*mMaxDistanceToDraw; 
		float transparentPOIArea = mMaxDistanceToDraw-solidPOIArea;
		
		Iterator ooisIterator =mSortedOOIs.entrySet().iterator();
		while(ooisIterator.hasNext())
		{
			Map.Entry mapOOI =(Map.Entry)ooisIterator.next();

			float distanceFromCam = mMaxDistanceToDraw-(Float)mapOOI.getKey();
			RadarOOI radarOOI = ((RadarOOI)mapOOI.getValue());
			
			//Log.v(radarOOI.mIcon.mName,"distanceFromCam="+distanceFromCam+" mMaxDistanceToDraw="+mMaxDistanceToDraw);
			if(distanceFromCam <= mMaxDistanceToDraw)
			{
				float scale = 1.0f-((distanceFromCam/mMaxDistanceToDraw)*(1.f-CommonRender.MIN_POI_SCALE));
				if(mFocusOOI == radarOOI) {
					radarOOI.DrawFocused(gl, mCamOrientation, mCamOffset, mUserOffset, mUserWorldLoc, mAlpha, scale, false, false);
				}
				else {
					radarOOI.mIcon.setGLPos(mCamOrientation, mCamOffset, mUserOffset);
					radarOOI.mIcon.SetScale(scale);
					if(mFocusOOI == null) {  // When nothing is focused - we can reset all OOI
						radarOOI.ResetScroll();
					}
					if(distanceFromCam < distanceBetweenCamToUser)
						radarOOI.mIcon.DrawUnfocused(gl, CommonRender.TOO_CLOSE_POI_ALPHA, !radarOOI.IsGroup());
					else  {
						if(distanceFromCam < solidPOIArea) {
							radarOOI.mIcon.DrawUnfocused(gl, 1.0f, !radarOOI.IsGroup());
						} else {
							radarOOI.mIcon.DrawUnfocused(gl, 1.0f-(((distanceFromCam-solidPOIArea)/transparentPOIArea)*(1.f-CommonRender.MIN_POI_ALPHA)), !radarOOI.IsGroup());
						}
					}
				}
			}
		}
	}
	
	public void DrawRadarOOIs(GL10 gl)
	{
		Vector3 mDrawAreaPos = mCircularDrawArea.getPosition();
		
		pTemp.set(mDrawAreaPos.x, mDrawAreaPos.y, mDrawAreaPos.z);
		mTemp.setZero();		
		mTemp.rotZ(-Util.degreesToRadians(mCamOrientation.x));
		mTemp.transform(pTemp);
		
		mDrawAreaOrigin.set(pTemp.x, pTemp.y, pTemp.z).sub(mUserOffset);
		
		mSortedOOIs.clear();

		float radius = mCircularDrawArea.getRadius();
		for (int x = 0; x < mBOIs.size(); x++)
		{
			RadarOOI radarOOI = GetOOI(mBOIs, x);
			if(!DrawOOI(radarOOI)) { continue;} 
			
			float currentDist = Util.GetLocalDistanceBetweenPositions(radarOOI.GetPosition(), mDrawAreaOrigin);
			
			if(currentDist < radius)
			{	
				radarOOI.mIcon.setGLPos(mCamOrientation, mCamOffset, mUserOffset);				
			}
			else
			{
				radarOOI.mIcon.setNormalizedGLPos(mCamOrientation, mCamOffset, mUserOffset, mDrawAreaOrigin, mCircularDrawArea.getRadius());
			}			
			
			//Log.v(TAG,radarOOI.mIcon.mName+"  "+ "  Pos:"+radarOOI.mIcon.mGLPosition.x+","+radarOOI.mIcon.mGLPosition.y+" - " +radarOOI.mPosition.x + ","+radarOOI.mPosition.y);
		}

		mOOIGrouper.SetBuddies(mBOIs, true);
		mOOIGrouper.SetBOIGroups(mCamLocation, mBundledBOIs, mIsRadarMode);
		
		mFocusOOI = null;
		mFocusedOOIs.clear();
		
		float xDist = 0.0f; float yDist = 0.0f;
		for (int x = 0; x < mBundledBOIs.size(); x++)
		{
			RadarOOI radarOOI = GetOOI(mBundledBOIs, x);
			if(!DrawOOI(radarOOI)) { continue;}

			if(radarOOI.IsGroup()) {
				xDist = radarOOI.mIcon.mGLPosition.x-CIRCLE_FTARGET_X;
				yDist = radarOOI.mIcon.mGLPosition.y-CIRCLE_FTARGET_Y;
			} else {
				xDist = radarOOI.mRadarOOIs.get(0).mIcon.mGLPosition.x-CIRCLE_FTARGET_X;
				yDist = radarOOI.mRadarOOIs.get(0).mIcon.mGLPosition.y-CIRCLE_FTARGET_Y;				
			}
			
			//Log.v(TAG,radarOOI.mIcon.mName+"  "+ " Group="+radarOOI.IsGroup() + " Pos:"+radarOOI.mIcon.mGLPosition.x+","+radarOOI.mIcon.mGLPosition.y+" - " +radarOOI.mPosition.x + ","+radarOOI.mPosition.y);
		
			mSortedOOIs.put(-1.0f*radarOOI.mPosition.y, radarOOI);
			
			//Log.v(TAG,radarOOI.mIcon.mName+" - "+xDist+","+yDist+", "+radarOOI.IsNormalized());
			
			if(xDist < CIRCLE_FTARGET_MAX_DIST) {
				//Log.v(TAG,radarOOI.mIcon.mName+" - "+xDist+","+yDist+", smallestDist="+smallestDist);
				if(yDist < CIRCLE_FTARGET_MAX_DIST) {				
					float zDist = (float)Math.sqrt(xDist*xDist + yDist*yDist);
					//Log.v(TAG,radarOOI.mIcon.mName+" - "+xDist+","+yDist+","+zDist+" smallestDist="+smallestDist);
					if(zDist < CIRCLE_FTARGET_MAX_DIST) {
						mFocusedOOIs.put(zDist, radarOOI);
						//Log.v(TAG,radarOOI.mIcon.mName);
					}
				}
			}			
		} 
		
		UpdateFocusOOI();

		Iterator ooisIterator = mSortedOOIs.entrySet().iterator();
		while(ooisIterator.hasNext())
		{
			Map.Entry mapOOI =(Map.Entry)ooisIterator.next();

			RadarOOI radarOOI = ((RadarOOI)mapOOI.getValue());
		
			if(radarOOI != mFocusOOI)
			{
				radarOOI.DrawFocused(gl, mCamOrientation, mCamOffset, mUserOffset, mUserWorldLoc, mAlpha, 0.5f, true, true);
			}
		}
		if(mFocusOOI != null)
		{
			mFocusOOI.DrawFocused(gl, mCamOrientation, mCamOffset, mUserOffset, mUserWorldLoc, mAlpha, 0.5f, false, true);
		}
	}
		
	public boolean IsDrawingWithinCircularDrawArea(){ return mIsRadarMode; }
	
	// Returns a buddy based on the passed in id
	public RadarBuddy GetBuddy(int id)
	{
		for(RadarOOI radarOOI : mBOIs)
		{
			if(radarOOI == null)
				continue;
			
			if(RadarBuddy.IsMyInstance(radarOOI)) {
				if (((RadarBuddy)radarOOI).mId == id)
				{
					return ((RadarBuddy)radarOOI);
				}
			}
		}
		
		return null;
	}
	
	// Add and Update Buddy objects
	private void AddBuddy(BuddyInfo buddyInfo, float localX, float localY, float dist, Context context)
	{		
		Vector3 posVector = new Vector3((float)localX, (float)localY, 0); // no Z value yet

		GLPoi newRI  = new GLPoi(POI.POI_TYPE_BUDDY, buddyInfo.name, posVector, context);		
		
		RadarBuddy buddy = new RadarBuddy(buddyInfo.location, posVector, newRI, POI.POI_TYPE_BUDDY);
		buddy.mIcon.setGLDrawObjects(mGLDynamicText, mGLDynamicIcon);
		buddy.SetBuddy(buddyInfo.localId, buddyInfo.name, dist);

		mBOIs.add(buddy);
	}	
	
	public void PrintAvailMem(){
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);
		Log.i(TAG, " memoryInfo.availMem " + memoryInfo.availMem + "\n" );
	}
	
	class BuddyRefreshThread extends Thread 
	{
		BuddyRefreshThread(){  }

        public void run() 
        {
        	while(true)
        	{	                		
    			synchronized (mBOIs)
	    		{
	        		for(int x = 0; x < mBOIs.size();)
	        		{
        				try
        				{ 
        					//Log.v(TAG,mOOIs.size()+" ");
        					RadarOOI radarOOI = GetOOI(mBOIs, x);
        					RadarBuddy buddy = ((RadarBuddy)radarOOI);
		        			buddy.IncrementLastUpdateSeconds();
		        			if(buddy.mSecondsSinceLastUpdate > CommonRender.BUDDY_OFFLINE_SECONDS)
		        			{
		        				Log.i(TAG, "buddy[" + buddy.mName + "] has gone offline!");
		        				mBOIs.remove(buddy);
		        				continue;
		        			}
        					
        					x++;  // Will not increment if continued/removed
        				}
        				catch(Exception e){Log.e(TAG, "BuddyRefreshThread: failed to update buddy update counter."); }
    	    		}
        		}
    			
    			//PrintAvailMem();
    			
	        	try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e){ e.printStackTrace(); }
        	}
        }
    }	
}
