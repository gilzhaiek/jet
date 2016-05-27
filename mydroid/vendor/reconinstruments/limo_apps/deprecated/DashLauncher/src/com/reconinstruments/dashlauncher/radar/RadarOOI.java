package com.reconinstruments.dashlauncher.radar;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.location.Location;
import android.util.Log;

import com.reconinstruments.dashlauncher.radar.maps.objects.POI;
import com.reconinstruments.dashlauncher.radar.prim.Vector3;
import com.reconinstruments.dashlauncher.radar.render.CommonRender;
import com.reconinstruments.dashlauncher.radar.render.GLPoi;

public class RadarOOI
{
	protected static final String TAG = "RadarOOI";
	
	public int					mType;
	public Location				mWorldLoc;
	public Vector3				mPosition;
	public GLPoi				mIcon;
	public ArrayList<RadarOOI>	mRadarOOIs = null;
	
	public RadarOOI(Location location, Vector3 position, GLPoi icon, int type)
	{
		mWorldLoc = location;
		mPosition = position;
		mIcon = icon;
		mType = type;
		mRadarOOIs = new ArrayList<RadarOOI>();
		mRadarOOIs.add(this);
	}

	public RadarOOI(ArrayList<RadarOOI> radarOOIs, Location location, Vector3 position, GLPoi icon, int type)
	{
		mRadarOOIs = radarOOIs;
		mWorldLoc = new Location("");
		mWorldLoc.setLongitude(location.getLongitude());
		mWorldLoc.setLatitude(location.getLatitude());
		mPosition = new Vector3(position);
		mIcon = icon;
		mType = type;
		if(mRadarOOIs.size() == 0) {
			mRadarOOIs.add(this);
		}
			
	}
	
	public boolean IsNormalized(){
		return mRadarOOIs.get(0).mIcon.mIsNormalized; 
	}

	public void SetDrawParams(boolean iconIsFresh) {
		mIcon.SetDrawParams(iconIsFresh);
	}
	
	public boolean IsGroup() {
		return (mRadarOOIs.size() > 1);
	}
	
	public boolean FinishedGroupScroll() {
		if(IsGroup()) {
			for(int selectedIcon = 0; selectedIcon < mRadarOOIs.size(); selectedIcon++) {
				if(!mRadarOOIs.get(selectedIcon).mIcon.FullScrollComplete())
					return false;
			}
			
			return true;
		} else {
			return mRadarOOIs.get(0).mIcon.FullScrollComplete();
		}
	}
	
	public void ResetScroll() {
		for(int i = 0; i < mRadarOOIs.size(); i++) {
			mRadarOOIs.get(i).mIcon.ResetScroll();
		}
	}
	
	public void DrawFocused(GL10 gl, Vector3 orientation, Vector3 camOffset, Vector3 userOffset, Location userLocation, float alpha, float scale, boolean firstNameOnly, boolean radarMode) {
		if(IsGroup()) {
			int selectedIcon = 0;

			for(selectedIcon = 0; selectedIcon < mRadarOOIs.size(); selectedIcon++) {
				if(!mRadarOOIs.get(selectedIcon).mIcon.FullScrollComplete())
					break;
			}
			
			if(selectedIcon == mRadarOOIs.size()) {
				selectedIcon = mRadarOOIs.size()-1;
			}

			// Log.v(TAG,mIcon.mName+":::"+mRadarOOIs.get(selectedIcon).mIcon.mName+"  "+selectedIcon+" - "+mRadarOOIs.size());
			
			if(mType == POI.POI_TYPE_BUDDY) {
				mRadarOOIs.get(selectedIcon).SetDrawParams(((RadarBuddy)mRadarOOIs.get(selectedIcon)).mSecondsSinceLastUpdate < CommonRender.BUDDY_NOT_FRESH_SECONDS);
			}
			mRadarOOIs.get(selectedIcon).mIcon.SetScale(scale);
			if(!radarMode) {
				mIcon.setGLPos(orientation, camOffset, userOffset);
			}
			mRadarOOIs.get(selectedIcon).mIcon.DrawFocused(gl, userLocation.distanceTo(mRadarOOIs.get(selectedIcon).mWorldLoc), alpha, false, mIcon.mGLPosition, firstNameOnly, radarMode);
			//Log.v(TAG,mIcon.mName+":::"+mRadarOOIs.get(selectedIcon).mIcon.mName+"  "+mIcon.mGLPosition.x+","+mIcon.mGLPosition.y+","+mIcon.mGLPosition.z);
		} else {
			if(mType == POI.POI_TYPE_BUDDY) {
				mRadarOOIs.get(0).SetDrawParams(((RadarBuddy)mRadarOOIs.get(0)).mSecondsSinceLastUpdate < CommonRender.BUDDY_NOT_FRESH_SECONDS);
			}
			
			mRadarOOIs.get(0).mIcon.SetScale(scale);
			if(!radarMode) {
				mRadarOOIs.get(0).mIcon.setGLPos(orientation, camOffset, userOffset);
			}
			//Log.v(TAG,mIcon.mName+":+:"+mRadarOOIs.get(selectedIcon).mIcon.mName+"  "+mIcon.mGLPosition.x+","+mIcon.mGLPosition.y);
			mRadarOOIs.get(0).mIcon.DrawFocused(gl, userLocation.distanceTo(mRadarOOIs.get(0).mWorldLoc), alpha, true, null, firstNameOnly, radarMode);
		}
	}
	
	public void CalcGroupLocation(boolean useGLPos) {
		if(!IsGroup()) return;
			
		float longitude = 0;
		float latitude = 0;
		float xPos = 0;
		float yPos = 0;
		
		float xGLPos = 0;
		float yGLPos = 0;
		float zGLPos = 0;

		try {
			for(int i = 0; i < mRadarOOIs.size(); i++) {
				RadarOOI tmpRadarOOI = mRadarOOIs.get(i);
				longitude += tmpRadarOOI.mWorldLoc.getLongitude();
				latitude += tmpRadarOOI.mWorldLoc.getLatitude();
				if(useGLPos) {
					xGLPos += tmpRadarOOI.mIcon.mGLPosition.x;
					yGLPos += tmpRadarOOI.mIcon.mGLPosition.y;
					zGLPos += tmpRadarOOI.mIcon.mGLPosition.z;
				}
				xPos += tmpRadarOOI.mPosition.x;
				yPos += tmpRadarOOI.mPosition.y;
				//zPos += tmpRadarOOI.mPosition.z;
				
				//Log.v(TAG,tmpRadarOOI.mIcon.mName +"+"+tmpRadarOOI.mPosition.x+","+tmpRadarOOI.mPosition.y+"-"+tmpRadarOOI.mIcon.mPos.x+","+tmpRadarOOI.mIcon.mPos.y+" -> "+xPos+","+yPos+","+zPos);
			}
			
			longitude /= mRadarOOIs.size();
			latitude /= mRadarOOIs.size();
			if(useGLPos) {
				xGLPos /= mRadarOOIs.size();
				yGLPos /= mRadarOOIs.size();
				zGLPos /= mRadarOOIs.size();
			}
			xPos /= mRadarOOIs.size();
			yPos /= mRadarOOIs.size();
			
			//Log.v(TAG,mIcon.mName +"="+xPos+","+yPos+","+zPos);
			
			mWorldLoc.setLatitude(latitude);
			mWorldLoc.setLongitude(longitude);
			mPosition.set(xPos, yPos, 0.0f);
			mIcon.SetPosition(mPosition);
			if(useGLPos) {
				mIcon.mGLPosition.set(xGLPos, yGLPos, zGLPos);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void SetPosition(Location location, float xPos, float yPos, float zPos)
	{
		mWorldLoc = location;
		mPosition.set(xPos, yPos, zPos);
		mIcon.SetPosition(xPos, yPos, zPos);
	}
	
	public Vector3 GetPosition()
	{
		return mPosition;
	}
	
	public void Release() {
		if(mRadarOOIs != null)
			mRadarOOIs.clear();
	}
	
	public void DebugPrint() {
		String debugStr = "";
		if(IsGroup()) {
			debugStr = "Group [" + mPosition.x+","+mPosition.y+"] - ";
			for(int i = 0; i < mRadarOOIs.size(); i++) {
				debugStr += mRadarOOIs.get(i).mIcon.mName + ", ";
			}
		} else {
			debugStr = "Item [" + mPosition.x+","+mPosition.y+"] - " + mIcon.mName;
		}
		
		Log.v(TAG, debugStr);
	}
}
