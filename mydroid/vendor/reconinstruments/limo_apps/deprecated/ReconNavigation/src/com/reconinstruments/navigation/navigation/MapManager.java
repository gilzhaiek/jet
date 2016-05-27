/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;

import com.reconinstruments.navigation.navigation.CustomPoiManager.CustomPoi;
import com.reconinstruments.navigation.navigation.dal.MapManagerDAL;
import com.reconinstruments.navigation.navigation.dal.ShpMapDAL;
import com.reconinstruments.navigation.navigation.datamanagement.ResortInfo;
import com.reconinstruments.navigation.navigation.datamanagement.ResortInfoProvider;
import com.reconinstruments.navigation.routing.ReManager;
import com.reconinstruments.navigation.routing.ReVerifyInfoProvider;

/**
 * A simple wrapper class for holding some of the map related instance
 */
public class MapManager implements IResortLoader,
		GPSListener.IGPSLocationUpdater {
	protected ResortInfo mActiveResort = null;
	public MapView mMapView = null;
	public ShpMap mMap = null;
	public CustomPoiManager mCPoiManager = null;
	public Context mContext;
	public PointF mOwnerLocation = null; // where the current owner located on
	private ReManager mReManager = null;
	private int mRecordedMapFeature = 0;
	private boolean mRecordedMapCenterUserDefined = false;
	private PointF mTempPoint = new PointF(); // temporary point
	protected PoInterest mHilitedPoi = null;
	protected IOverlayManager mOverlayManager = null;
	protected ShpMapDAL mShpMapDAL = null;
	protected String mLastResortName = "";
	protected int mLastResortID = -1;
	protected int mLastResortCountryID = -1;

	// temporary flag for disabling/enabling gps signal to reset the map
	// location
	public boolean mDiscardGPSSignal = false;

	public MapManager(Context context, MapView mapView,
			IOverlayManager overlayManager) {
		mContext = context;
		mMapView = mapView;
		mMap = new ShpMap();
		mCPoiManager = new CustomPoiManager(mContext, mMap, mMapView);
		mReManager = new ReManager();
		mOverlayManager = overlayManager;
		mShpMapDAL = new ShpMapDAL();
	}

	public MapLoadTask.IMapLoadCallback mMapLoadCallback = new MapLoadTask.IMapLoadCallback() {

		@Override
		public void onPreExecute() {

		}

		@Override
		public void onPostExecute() {

			// force to reset the position of the owner poi on the map
			if (mOwnerLocation != null) {
				mCPoiManager.updatePoiLocation(
						CustomPoiManager.DEVICE_OWNER_POI_ID,
						PoInterest.POI_TYPE_OWNER, mOwnerLocation.x,
						mOwnerLocation.y);
			}

			// load the map's user preferences and points
			if (mActiveResort != null) {
				mShpMapDAL.Load(mContext, mActiveResort.mID, mMap);
				mCPoiManager.loadCDPs(mContext, mActiveResort.mID);
				mMapView.invalidate();
			}

			// set the map center to one of the POI at the site
			// the search order is ski-center --> information center --> ski
			// lift --> the first POI on the list
			if (mMap != null) {

				ArrayList<PoInterest> poiSets = mMap.mPoInterests.get(PoInterest.POI_TYPE_SKICENTER);
				if (poiSets.size() == 0) {
					// if there is no SKICENTER, we try information center
					poiSets = mMap.mPoInterests.get(PoInterest.POI_TYPE_INFORMATION);

					if (poiSets.size() == 0) {
						poiSets = mMap.mPoInterests.get(PoInterest.POI_TYPE_CHAIRLIFTING);

						if (poiSets.size() == 0) {
							// looking for the first available poiSets
							for (int i = 0; i < PoInterest.NUM_POI_TYPE; ++i) {
								poiSets = mMap.mPoInterests.get(i);
								if (poiSets.size() > 0) {
									break;
								}
							}
						}
					}
				}

				if (poiSets.size() > 0) {
					PointF initPos = poiSets.get(0).mPosition;
					mMapView.setCenter(initPos.x, initPos.y, false);

				}
			}
		}
	};

	public void FocusOnMe() {
		if(mOwnerLocation == null)
		{
			return;
		}
			
		mMapView.ClearUserDefinedCenter();
		onLocationChanged((double) mOwnerLocation.x, (double) mOwnerLocation.y);
	}

	public void loadOwnerClosestLocation()
	{
		if (mOwnerLocation == null) {
			return;
		}
		// otherwise, we do not have any resort active.
		// then search for a resort that contains the GPS read
		ResortInfo resortInfo = ResortInfoProvider.lookForResort(mOwnerLocation.x, mOwnerLocation.y);

		if (resortInfo != null && mActiveResort != resortInfo) {
			if (mOverlayManager != null) {
				// clear any overlay on top of the MapView
				// before staring load
				mOverlayManager.setOverlayView(null);
			}

			loadResort(resortInfo);
		}	
	}
	
	/**
	 * Implemented the interface of GPSListener.IGPSUpdater
	 * 
	 */
	public void onLocationChanged(double latitude, double longitude) {
		float lat = (float)latitude;
		float lng = (float)longitude;
		
		if (mOwnerLocation == null) {
			// create the owner location and the owner POI for the first time
			mOwnerLocation = new PointF(lat, lng);

			CustomPoiManager.CustomPoi ownerPoi = mCPoiManager.new CustomPoi(0,
					0, "", CustomPoiManager.DEVICE_OWNER_POI_ID,
					PoInterest.POI_TYPE_OWNER);
			mCPoiManager.addPoi(ownerPoi);
		} else {
			mOwnerLocation.set(lat, lng);
		}

		if (mActiveResort != null) {
			if (mDiscardGPSSignal == false) {
				// if there is a resort map currently being rendered
				// and the GPS read is within the range of the map
				// lets update the MapView to center around the GPS read
				if (mActiveResort.contains(lat, lng)) {
					mMapView.setCenterLatLng(lat, lng, true);

					/*if (!mMapView.IsCenterUserDefined()) {
						// hilite the closest poi on the map
						PointF local = Util.mapLatLngToLocal(lat, lng);
						hiliteClosestPoi(local.x, local.y);
					}*/
				}
			}

			// otherwise, leave the MapView along without changing its centre
		} else {
			loadOwnerClosestLocation();
		}

		mCPoiManager.updatePoiLocation(CustomPoiManager.DEVICE_OWNER_POI_ID, PoInterest.POI_TYPE_OWNER, lat, lng);
	}

	/**
	 * Implemented the interface of GPSListener.IGPSUpdater
	 * 
	 */
	public void onBearingChanged(float newBearing) {
		if (mActiveResort != null) {
			if (mDiscardGPSSignal == false && mOwnerLocation != null) {
				// if there is a resort map currently being rendered
				// and the GPS read is within the range of the map
				// lets update the MapView to center around the GPS read
				if (mActiveResort.contains(mOwnerLocation.x, mOwnerLocation.y)) {
					// mMapView.setRotation( newBearing );
				}
			}

		}
	}

	public void loadResort(ResortInfo resortInfo) {
		setActiveResort(resortInfo);
		loadActiveResort();
		MapManagerDAL.Save(mContext, this);  // Shouldn't save the BO - should save the Object.  Right now the Manager (BO) is also the object...
	}

	protected void loadActiveResort() {
		if (mActiveResort != null) {
			loadMap(mActiveResort.mAssetBaseName + ".shp",
					mActiveResort.mAssetBaseName + ".dbf", mActiveResort.mName);
		}
	}

	// implements IResortLoader interface
	public void loadMap(String shpfile, String dbffile, String siteName) {
		if (mMap == null) {
			mMap = new ShpMap();
		}

		if (mReManager == null) {
			mReManager = new ReManager();
		}

		if (mMapView != null) {
			mMapView.reset();
		}

		mCPoiManager.reset();
		mMap.reset();
		mReManager.clear();
		PoiInfoProvider.reset();
		ReVerifyInfoProvider.reset();

		MapLoadTask task = new MapLoadTask(mContext, mMapView, siteName, mMap,
				mReManager, mMapLoadCallback);
		task.execute(shpfile, dbffile);
	}

	public String GetAvailablePinName(String baseName) {
		return mCPoiManager.GetAvailablePinName(baseName);
	}

	/**
	 * Drop a pin, i.e custom-defined point-of-interest at the current center
	 * location of the map-view.
	 */
	public void dropPin(String pinName) {
		// make sure there is actually a map
		// that has been loaded before we
		// can drop a custom pin on the map
		if (mActiveResort != null) {
			PointF localPos = mMapView.getCenter();
			Util.mapLocalToLatLng(localPos);

			long id = CustomPoiManager.generateID();
			CustomPoiManager.CustomPoi customPoi = mCPoiManager.new CustomPoi(
					localPos.y, localPos.x, pinName, id,
					PoInterest.POI_TYPE_CDP, new Date());
			mCPoiManager.addPoi(customPoi);
			mCPoiManager.saveCDPs(mContext, mActiveResort.mID);
			// customPoi.mPoi.setStatus(PoInterest.POI_STATUS_TRACKED );
			// add the CDP to the map
			mMap.addPOI(customPoi.mPoi);
			mMapView.invalidate();
		}
	}

	public void removePin(PoInterest pin) {
		if (mActiveResort != null) {
			mCPoiManager.removePoi(pin);
			mCPoiManager.saveCDPs(mContext, mActiveResort.mID);
			mMap.removePOI(pin);
			mMapView.invalidate();
		}
	}

	public void recordMapFeature() {
		mRecordedMapFeature = mMapView.getFeature();
		mRecordedMapCenterUserDefined = mMapView.IsCenterUserDefined();
	}

	public void restoreMapFeature(boolean restoreRecorderCenter) {
		mMapView.clearFeatures();
		mMapView.setFeature(mRecordedMapFeature, true);
		
		if(restoreRecorderCenter && !mRecordedMapCenterUserDefined)
		{
			mMapView.ClearUserDefinedCenter();
		}
	}

	/**
	 * Given a location (lat, lng) find the closest poi located on the active
	 * shpmap If active shpmap is empty, return null
	 */
	public PoInterest findClosestPoiLatLng(float lat, float lng) {
		if (mMap == null || mMap.isEmpty()) {
			return null;
		} else {
			mTempPoint.set(lng, lat);
			Util.mapLatLngToLocal(mTempPoint);
			return findClosestPoi(mTempPoint.x, mTempPoint.y);
		}
	}

	/**
	 * Given a location at local space (x, y) find the closest poi located on
	 * the active shpmap If active shpmap is empty, return null
	 */
	public PoInterest findClosestPoi(float x, float y) {
		if (mMap == null || mMap.isEmpty()) {
			return null;
		} else {
			mTempPoint.set(x, y);
			float minDist = Float.MAX_VALUE;
			PoInterest poi = null;

			for (int i = 0; i < PoInterest.NUM_POI_TYPE; ++i) {
				if (i != PoInterest.POI_TYPE_OWNER) {
					ArrayList<PoInterest> pois = mMap.mPoInterests.get(i);
					for (PoInterest test : pois) {
						float l = PointF.length(
								mTempPoint.x - test.mPosition.x, mTempPoint.y
										- test.mPosition.y);
						if (l < minDist) {
							minDist = l;
							poi = test;
						}
					}
				}
			}

			return poi;
		}
	}

	public PoInterest hiliteClosestPoi(float localX, float localY) {
		PoInterest poi = findClosestPoi(localX, localY);
		if (poi == null) {
			clearHilitedPoi();
			return null;
		}

		if (mHilitedPoi != poi) {
			clearHilitedPoi();

			mHilitedPoi = poi;
			mHilitedPoi.setHilited(true);
		}

		return mHilitedPoi;
	}

	public void clearHilitedPoi() {
		if (mHilitedPoi != null) {
			mHilitedPoi.setHilited(false);
			mHilitedPoi = null;
		}
	}

	public PoInterest getOwner() {
		if (mMap != null
				&& mMap.mPoInterests.get(PoInterest.POI_TYPE_OWNER).size() > 0) {
			return mMap.mPoInterests.get(PoInterest.POI_TYPE_OWNER).get(0);

		} else {
			return null;
		}
	}

	public void Save() {
		if (mActiveResort != null) {
			mShpMapDAL.Save(mContext, mActiveResort.mID, mMap);
		}
	}

	public void setActiveResort(ResortInfo resortInfo) {
		mActiveResort = resortInfo;
		setLastResortName(mActiveResort.mName);
		setLastResortID(mActiveResort.mID);
		setLastResortCountryID(mActiveResort.mCountryID);
	}

	public ResortInfo getActiveResort() {
		return mActiveResort;
	}

	public void setLastResortName(String value) {
		this.mLastResortName = value;
	}

	public String getLastResortName() {
		return this.mLastResortName;
	}

	public void setLastResortID(int value) {
		this.mLastResortID = value;
	}

	public int getLastResortID() {
		return this.mLastResortID;
	}

	public void setLastResortCountryID(int value) {
		this.mLastResortCountryID = value;
	}

	public int getLastResortCountryID() {
		return this.mLastResortCountryID;
	}

}
