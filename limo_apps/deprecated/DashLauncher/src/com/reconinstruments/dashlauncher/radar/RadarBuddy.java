package com.reconinstruments.dashlauncher.radar;

import android.location.Location;

import com.reconinstruments.dashlauncher.radar.maps.objects.ResortData;
import com.reconinstruments.dashlauncher.radar.prim.Vector3;
import com.reconinstruments.dashlauncher.radar.render.GLPoi;

public class RadarBuddy extends RadarOOI
{
	public int		mId;
	public String	mName;
	public float	mDistance;
	public long		mSecondsSinceLastUpdate = -1;

	public RadarBuddy(Location location, Vector3 position, GLPoi poi, int type)
	{
		super(location, position, poi, type);
	}

	public void SetBuddy(int id, String name, float distance)
	{
		mId = id;
		mName = name;
		mDistance = distance;
		mSecondsSinceLastUpdate = 0;
	}
	
	public void Update(Location location, float distance, float xPos, float yPos, float zPos)
	{
		SetPosition(location, xPos, yPos, zPos);
		mDistance = distance;
		mSecondsSinceLastUpdate = 0;
	}

	public void IncrementLastUpdateSeconds()
	{
		mSecondsSinceLastUpdate++;
	}
	
	public long GetSecondsSinceLastUpdate() {
		return mSecondsSinceLastUpdate;
	}
	
	public static boolean IsMyInstance(Object object) {
		return object instanceof RadarBuddy; 
	}
}
