package com.reconinstruments.dashlauncher.radar.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.graphics.PointF;
import android.location.Location;
import android.util.Log;

import com.reconinstruments.dashlauncher.radar.RadarOOI;
import com.reconinstruments.dashlauncher.radar.maps.objects.POI;
import com.reconinstruments.dashlauncher.radar.prim.Vector3;

public class OOIGrouper {
	protected static final String TAG = "OOIGrouper";
	
	protected Context mContext = null;
	
	protected GLGlobalTextRenderer mGLDynamicText = null;
	protected GLGlobalIconRenderer mGLDynamicIcon = null;
	
	protected Vector3 mZeroVector = new Vector3();

	protected static final ReentrantLock mLock = new ReentrantLock();
	
	protected class RadarOOIMatrixRow {
		public RadarOOI mRadarOOIRoot = null;
		public RadarOOIMatrixRow(RadarOOI radarOOIRoot) {
			mRadarOOIRoot = radarOOIRoot;
		}
		
		public SortedMap<Float, RadarOOI> mRadarOOICells = new TreeMap<Float, RadarOOI>();
	}
	
	protected ArrayList<RadarOOIMatrixRow> mRadarOOIRestMatrix 			= new ArrayList<RadarOOIMatrixRow>();
	protected ArrayList<RadarOOIMatrixRow> mRadarOOILiftsMatrix			= new ArrayList<RadarOOIMatrixRow>();
	protected ArrayList<RadarOOIMatrixRow> mRadarOOIBuddiesMatrix		= new ArrayList<RadarOOIMatrixRow>();
	
	public OOIGrouper(Context context, GLGlobalTextRenderer glDynamicText, GLGlobalIconRenderer glDynamicIcon) {
		mContext = context;
		mGLDynamicText = glDynamicText;
		mGLDynamicIcon = glDynamicIcon;
	}
	
	protected void ClearMatrix(ArrayList<RadarOOIMatrixRow> radarOOIMatrix) {
		for(int i = 0; i < radarOOIMatrix.size(); i++) {
			radarOOIMatrix.get(i).mRadarOOICells.clear();
		}
		radarOOIMatrix.clear();		
	}
	
	protected void AddToMatrix(int type, RadarOOIMatrixRow radarOOIMatrixRow){
		switch (type) {
		case POI.POI_TYPE_RESTAURANT:
			mRadarOOIRestMatrix.add(radarOOIMatrixRow);
			break;
		case POI.POI_TYPE_CHAIRLIFTING:
			mRadarOOILiftsMatrix.add(radarOOIMatrixRow);
			break;
		case POI.POI_TYPE_BUDDY:
			mRadarOOIBuddiesMatrix.add(radarOOIMatrixRow);
			break;
		}
	}
	
	public float GetGLDistance(RadarOOI radarOOIA, RadarOOI radarOOIB) {
		try {
			return radarOOIA.mIcon.mGLPosition.distance(radarOOIB.mIcon.mGLPosition);
		} catch (Exception e) {
			return Float.MAX_VALUE;
		}
	}
	
	public float GetDistance(RadarOOI radarOOIA, RadarOOI radarOOIB) {
		try {
			return radarOOIA.mPosition.distance(radarOOIB.mPosition);
		} catch (Exception e) {
			return Float.MAX_VALUE;
		}
	}
	
	public void AddOOIsToMatrix(ArrayList<RadarOOI> radarOOIs, boolean useGLPos){
		for(int i = 0; i < radarOOIs.size(); i++) {
			RadarOOI radarOOIA = radarOOIs.get(i);
			
			RadarOOIMatrixRow radarOOIMatrixRow = new RadarOOIMatrixRow(radarOOIA);
			for(int j = (i+1); j < radarOOIs.size(); j++) {
				RadarOOI radarOOIB = radarOOIs.get(j);
				if(radarOOIA.mType == radarOOIB.mType) {
					radarOOIMatrixRow.mRadarOOICells.put(useGLPos ? GetGLDistance(radarOOIA,radarOOIB) : GetDistance(radarOOIA,radarOOIB), radarOOIB);
				}
			}
			AddToMatrix(radarOOIA.mType, radarOOIMatrixRow);
		}		
	}
	
	public void SetPOIs(ArrayList<RadarOOI> radarOOIs, boolean useGLPos) {
		ClearMatrix(mRadarOOIRestMatrix);
		ClearMatrix(mRadarOOILiftsMatrix);
		
		AddOOIsToMatrix(radarOOIs, useGLPos);
	}
	
	public void SetBuddies(ArrayList<RadarOOI> radarOOIs, boolean useGLPos) {
		mLock.lock();
		try{
			ClearMatrix(mRadarOOIBuddiesMatrix);
			
			AddOOIsToMatrix(radarOOIs, useGLPos);
		} catch (Exception e) {
		} 
		mLock.unlock();
	}
	
	protected void AddGroup(Vector3 camLocation, ArrayList<RadarOOIMatrixRow> radarOOIMatrix, ArrayList<RadarOOI> radarOOIs, boolean radarMode) {
		Map<RadarOOI, ArrayList<RadarOOI>> mappedOOIToGroups = new HashMap<RadarOOI, ArrayList<RadarOOI>>();
		
		int startingSize = radarOOIs.size();
		
		for(int i = 0; i < radarOOIMatrix.size(); i++) {  // Create Groups
			RadarOOI radarOOIA = radarOOIMatrix.get(i).mRadarOOIRoot;
			if(mappedOOIToGroups.containsKey(radarOOIA)) { // If we already put the item in a group - ignore it
				continue;
			}
			
			Vector3 radarOOIAPos = (radarMode) ? radarOOIA.mIcon.mGLPosition : radarOOIA.mPosition;
			
			ArrayList<RadarOOI> newGroup = new ArrayList<RadarOOI>();
			newGroup.add(radarOOIA); // Make a group where the first element is the root item
			
			float maxDistanceToMakeAGroup = 0.0f;
			if(radarMode) {
				maxDistanceToMakeAGroup = mZeroVector.distance(radarOOIAPos)*CommonRender.POI_RADAR_GROUP_RATIO;
			} else {
				maxDistanceToMakeAGroup = camLocation.distance(radarOOIAPos)*CommonRender.POI_GROUP_RATIO;
			}
			
			//Log.v(TAG, radarOOIA.mIcon.mName + " (" +camLocation.x+","+camLocation.y+","+camLocation.z+")::("+radarOOIA.mPosition.x+","+radarOOIA.mPosition.y+","+radarOOIA.mPosition.z+") maxDistanceToMakeAGroup=" + maxDistanceToMakeAGroup);

			Iterator ooisIterator = radarOOIMatrix.get(i).mRadarOOICells.entrySet().iterator();
			while(ooisIterator.hasNext()) // Iterate on each of the root items
			{
				Map.Entry mapCell =(Map.Entry)ooisIterator.next();

				float distanceToRadarOOI = (Float)mapCell.getKey();
				RadarOOI radarOOIB = (RadarOOI)mapCell.getValue();
				//Log.v(TAG,radarOOIB.mIcon.mName + " distanceToRadarOOI=" +distanceToRadarOOI);
				
				if(distanceToRadarOOI > maxDistanceToMakeAGroup) {  // If the item is too far from the sorted root, we don't need to continue (the rest of the items are farther)
					//Log.v(TAG,radarOOIB.mIcon.mName + " distanceToRadarOOI > maxDistanceToMakeAGroup continue");
					continue;
				}
				
				ArrayList<RadarOOI> attachedGroup = mappedOOIToGroups.get(radarOOIB); 
				if(attachedGroup != null) {  // Found the item in a different group
					//Log.v(TAG,radarOOIB.mIcon.mName + " GetDistance(attachedGroup.get(0),radarOOIB)=" +GetDistance(attachedGroup.get(0),radarOOIB));
					if(distanceToRadarOOI < GetDistance(attachedGroup.get(0),radarOOIB)) {  // If the distance of our group is shorter than the one in the old group
						attachedGroup.remove(radarOOIB); // Remove it
						mappedOOIToGroups.remove(radarOOIB);
					} else {
						//Log.v(TAG,radarOOIB.mIcon.mName + " distanceToRadarOOI >= GetDistance(attachedGroup.get(0),radarOOIB)");
						//Log.v(TAG,radarOOIA.mIcon.mName+ " - [" + distanceToRadarOOI +">"+GetDistance(attachedGroup.get(0),radarOOIB)+"] " + radarOOIB.mIcon.mName);
						continue; // The distance is farther than the old group
					}
				}
				
				//Log.v(TAG,radarOOIA.mIcon.mName+ " + " + radarOOIB.mIcon.mName);
				newGroup.add(radarOOIB);  // Add the item to the new group
				//Log.v(TAG,	"newGroup.add("+radarOOIB.mIcon.mName + ") ("+radarOOIB.mPosition.x+","+radarOOIB.mPosition.y+","+radarOOIB.mPosition.z+")");
				mappedOOIToGroups.put(radarOOIB, newGroup); // Made it available for search
			}
			
			GLPoi glIcon = new GLPoi(radarOOIA.mType, radarOOIA.mIcon.mName, radarOOIAPos, mContext);
			glIcon.setGLDrawObjects(mGLDynamicText, mGLDynamicIcon);
			
			RadarOOI radarGroup = new RadarOOI(newGroup, radarOOIA.mWorldLoc, radarOOIAPos, glIcon, radarOOIA.mType);
			radarOOIs.add(radarGroup);
		} 
		
		// Calc new location
		for(int i = startingSize; i < radarOOIs.size(); i++) {
			radarOOIs.get(i).CalcGroupLocation(radarMode);
			//radarOOIs.get(i).DebugPrint();
		}
	}
	
	public void SetPOIGroups(Vector3 camLocation, ArrayList<RadarOOI> radarOOIs) {
		for(int i = 0; i < radarOOIs.size(); i++) {
			radarOOIs.get(i).Release();
		}
		radarOOIs.clear();
		
		AddGroup(camLocation, mRadarOOIRestMatrix, radarOOIs, false);
		AddGroup(camLocation, mRadarOOILiftsMatrix, radarOOIs, false);
	}
	
	public void SetBOIGroups(Vector3 camLocation, ArrayList<RadarOOI> radarOOIs, boolean radarMode) {
		radarOOIs.clear();
		
		mLock.lock();
		try {
			AddGroup(camLocation, mRadarOOIBuddiesMatrix, radarOOIs, radarMode);
		} catch (Exception e) {}
		mLock.unlock();
	}
		
	protected void DebugPrintMatrix(ArrayList<RadarOOIMatrixRow> radarOOIMatrix) {
		for(int i = 0; i < radarOOIMatrix.size(); i++) {
			String rowStr = radarOOIMatrix.get(i).mRadarOOIRoot.mIcon.mName + "("+radarOOIMatrix.get(i).mRadarOOIRoot.mPosition.x+","+radarOOIMatrix.get(i).mRadarOOIRoot.mPosition.y+","+radarOOIMatrix.get(i).mRadarOOIRoot.mPosition.z+")| ";
			Iterator ooisIterator = radarOOIMatrix.get(i).mRadarOOICells.entrySet().iterator();
			while(ooisIterator.hasNext())
			{
				Map.Entry mapCell =(Map.Entry)ooisIterator.next();

				float distanceToRadarOOI = (Float)mapCell.getKey();
				RadarOOI radarOOIB = (RadarOOI)mapCell.getValue();
				
				rowStr += distanceToRadarOOI+":"+radarOOIB.mIcon.mName + "("+radarOOIB.mPosition.x+","+radarOOIB.mPosition.y+","+radarOOIB.mPosition.z+"), ";
			}
			
			Log.v(TAG,rowStr);
		}
	}
	
	public void DebugPrintMatrices() {
		Log.v(TAG,"---- Buddy Matrix ----");
		DebugPrintMatrix(mRadarOOIBuddiesMatrix);
		Log.v(TAG,"---- Resterants Matrix ----");
		DebugPrintMatrix(mRadarOOIRestMatrix);
		Log.v(TAG,"---- Lifts Matrix ----");
		DebugPrintMatrix(mRadarOOILiftsMatrix);
	}	
}
