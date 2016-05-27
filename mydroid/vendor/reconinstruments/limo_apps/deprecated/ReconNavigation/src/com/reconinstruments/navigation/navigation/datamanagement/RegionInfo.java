/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation.datamanagement;

import java.util.ArrayList;

import android.graphics.RectF;

/**
 * This class defines a country-region information
 * loaded from the resort information DB
 */
public class RegionInfo
{
	public int mID;										//the countryRegion ID, the "id" field of the countryRegion table in the resortInfo.DB
	public String mCountryName;							//the country that region belong to
	public String mName = null;							//the region name, Must NOT be NULL;
	public ArrayList<ResortInfo> mResorts = null;
	public RectF mBBox;									//the bounding box of the region in (lng, lat)
	public int mNumAvailableResorts = 0;				//the number of resorts within this region that have shape map data available(purchased)
	
	public RegionInfo( int id, String countryName, String regionName )
	{
		mID = id;
		mCountryName = countryName;
		mName = regionName;
		mBBox = new RectF();
		mBBox.setEmpty();
	}
	
	public void addResort( ResortInfo resort )
	{
		if( mResorts == null )
		{
			mResorts = new ArrayList<ResortInfo>();
		}
		
		mBBox.union(resort.mBBox.left, resort.mBBox.top, resort.mBBox.right, resort.mBBox.bottom);		
		//insert the resort to the list sorting by the name ascendingly
		int idx = 0;
		for( ResortInfo curr : mResorts )
		{
			if( resort.mName.compareTo(curr.mName) < 0 )
			{
				//find the position to insert
				break;
			}
			
			//otherwise, increase the position
			++idx;
		}
		mResorts.add(idx, resort);
		if( resort.mIsAvailable )
		{
			++mNumAvailableResorts;
		}
	}
	
	/**
	 * Override the default toString( ) function 
	 * to provide a string to ArrayAdapter for rendering
	 * in a listView.
	 */
	@Override
	public String toString()
	{
		return mName + "(" + mResorts.size() + ")";
	}
	
	public ArrayList<ResortInfo> getAvailableResorts()
	{
		if( mResorts != null )
		{
			int num = 0;
			for( ResortInfo info : mResorts )
			{
				if( info.mIsAvailable )
				{
					++num;
				}
			}
			
			if( num > 0 )
			{
				ArrayList<ResortInfo> list = new ArrayList<ResortInfo>( num );
				for( ResortInfo info : mResorts )
				{
					if( info.mIsAvailable )
					{
						list.add( info );
					}
				}
				
				return list;
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Given a resort name, search the region
	 * for a resortInfo that match the name.
	 * Return null if none found
	 */
	public ResortInfo getResort( String resortName )
	{
		for( ResortInfo resort : mResorts )
		{
			if( resort.mName.equals(resortName) )
			{
				return resort;
			}			
		}
		
		return null;
	}
	
	public ResortInfo getResort( int resortID )
	{
		if( mResorts == null )
		{
			return null;
		}
		else
		{
			for( ResortInfo info : mResorts )
			{
				if(info.mID == resortID)
				{
					return info;
				}
			}
		}	
		
		return null;
	}
	
	/**
	 * Given a position(lat, lng), looking for a resort
	 * that contains this position. If there are multiple
	 * resorts contains this location. The one that is closest
	 * will be returned. The closest is defined as the minimum distance
	 * from this location to the corners and center
	 */
	public ResortInfo lookForResort( float latitude, float longitude )
	{
		if( mResorts == null || !mBBox.contains(longitude, latitude))
		{
			return null;
		}
		else
		{
			ResortInfo resort = null;
			float minDist = Float.MAX_VALUE;
			
			for( ResortInfo info : mResorts )
			{
				if( info.contains(latitude, longitude))
				{					
					
					float l = info.minDistance(latitude, longitude);
					if( resort == null || l < minDist )
					{
						resort = info;
						minDist = l;						
					}
				}
			}
			
			return resort;
		}
	}	
}
 