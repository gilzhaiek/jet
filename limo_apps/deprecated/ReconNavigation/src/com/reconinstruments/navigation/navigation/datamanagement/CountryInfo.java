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
public class CountryInfo
{
	static final int COUNTRY_ID_UNDEFINED = -1;
	public int mID = COUNTRY_ID_UNDEFINED;				//the countryRegion ID, the "id" field of the countryRegion table in the resortInfo.DB
														//zero if the country has sub regions.
	public String mName = null;							//the country name, Must NOT be NULL;
	public int mRegionIDLow = COUNTRY_ID_UNDEFINED;		//the low bound ID of the region;
	public int mRegionIDHigh = COUNTRY_ID_UNDEFINED;	//the high bound ID of the region
	public ArrayList<RegionInfo> mRegions = null;		//can be NULL;
	public ArrayList<ResortInfo> mResorts = null;		//the resorts located on this country\
	public RectF mBBox;									//the bounding box in (lng, lat) for the country
	public int mNumAvailableResorts = 0;				//the number of resorts within this country that have shape map data available(purchased) 
	
	public CountryInfo( int id, String name )
	{
		mID = id;
		mName = name;
		mBBox = new RectF();
		mBBox.setEmpty();
	}
	
	/**
	 * Add a new region for given id, and name
	 */
	public void addRegion( int id, String regionName )
	{
		RegionInfo info = new RegionInfo( id, mName, regionName );
		mRegionIDLow = mRegionIDLow == COUNTRY_ID_UNDEFINED ? id : mRegionIDLow > id ? id : mRegionIDLow;
		mRegionIDHigh = mRegionIDHigh == COUNTRY_ID_UNDEFINED ? id : mRegionIDHigh < id ? id : mRegionIDHigh;
		if( mRegions == null )
		{
			mRegions = new ArrayList<RegionInfo>( 64 );
		}
		
		//insert the region to the list sorting by the name ascendingly
		int idx = 0;
		for( RegionInfo curr : mRegions )
		{
			if( info.mName.compareTo(curr.mName) < 0 )
			{
				//find the position to insert
				break;
			}
			
			//otherwise, increase the position
			++idx;
		}
		mRegions.add(idx, info);
	}
	
	/**
	 * 
	 * Given an region ID, check if there is
	 * a sub-region of this country that has ID
	 * match it.
	 * 
	 */
	public boolean isMyID( int id )
	{
		return (mID == id) || (id >= mRegionIDLow && id <= mRegionIDHigh);
	}
	
	/**
	 * Given an region ID, find the region
	 */
	public RegionInfo getRegion( int regionId )
	{
		if( regionId < mRegionIDLow || regionId > mRegionIDHigh )
		{
			return null;
		}
		else
		{
			for( RegionInfo region : mRegions )
			{
				if( region.mID == regionId )
					return region;
			}
			return null;
		}
	}
	
	/**
	 * 
	 * Added a resortInfo to this country
	 * If the country has sub-regions,
	 * added the resort that region
	 * 
	 */
	public void addResort( ResortInfo resort )
	{
		mBBox.union(resort.mBBox.left, resort.mBBox.top, resort.mBBox.right, resort.mBBox.bottom);
		if( mRegions == null )
		{
			//this country has no sub-region, so just add the resort
			//to the resort list of this country
			if( mResorts == null )
			{
				mResorts = new ArrayList<ResortInfo>();
			}			
			

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
		}
		else
		{
			//otherwise search for the region that contains this resort,
			//and add the resort to that region
			for( RegionInfo region : mRegions )
			{
				if( region.mID == resort.mCountryID )
				{
					region.addResort(resort);
					break;
				}
			}
		}
		if( resort.mIsAvailable )
		{
			++mNumAvailableResorts;
		}
	}
	
	/**
	 * Override the toString( ) to provide the string 
	 * Requested by ArrayAdapter to feed the correct content for rendering
	 * in a listView 
	 */
	@Override
	public String toString()
	{
		//return a string that has the country and the number of resorts in this country
		int numResort = 0;
		if( mRegions == null )
		{
			numResort = mResorts.size();
		}
		else
		{
			for( RegionInfo region : mRegions )
			{
				numResort += region.mResorts.size();
			}
		}
		
		return mName + "(" + numResort + ")";
	}
	
	/**
	 * 
	 * Given a region name, search for the
	 * regionInfo that matches the name
	 */
	public RegionInfo getRegion( String regionName )
	{
		if( mRegions == null)
		{
			return null;
		}
		
		//otherwise
		for( RegionInfo region : mRegions )
		{
			if( region.mName.equals(regionName))
			{
				return region;
			}
		}
		
		return null;
	}
	
	/**
	 * Given a resort name, search for the
	 * resortInfo that match the given name
	 * in the resortList of the country.  
	 */
	public ResortInfo getResort( String resortName )
	{
		if( mResorts != null )
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
		else
		{
			for( RegionInfo region : mRegions )
			{
				ResortInfo resort = region.getResort( resortName );
				
				if( resort != null)
				{
					return resort;
				}
			}
			
			return null;
		}
		
		
	}
	

	public ArrayList<RegionInfo> getAvailableRegions()
	{
		if( mRegions != null )
		{
			int num = 0;
			for( RegionInfo info : mRegions )
			{
				if( info.mNumAvailableResorts > 0 )
				{
					++num;
				}
			}
			
			if( num > 0 )
			{
				ArrayList<RegionInfo> list = new ArrayList<RegionInfo>( num );
				for( RegionInfo info : mRegions )
				{
					if( info.mNumAvailableResorts > 0 )
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

	public ResortInfo getResort( int resortID )
	{
		if( mRegions == null && mResorts == null)
		{
			return null;
		}
		else
		{				
			if( mResorts != null )
			{
				for( ResortInfo info : mResorts )
				{
					if(info.mID == resortID)
					{
						return info;
					}
				}
			}
			else
			{
				for( RegionInfo region : mRegions )
				{
					ResortInfo info = region.getResort(resortID);
					
					if( info != null )
					{
						return info;
					}					
				}				
			}
			
			return null;
		}
	}
	
	/**
	 *Given a location( latitude, longitude ),
	 *search for a resort that contains this location
	 *if multiple resorts contain this location
	 *return the one that is closest to this location
	 *by testing the location to the five position of the resort
	 */
	public ResortInfo lookForResort( float latitude, float longitude )
	{
		if( (mRegions == null && mResorts == null) || mBBox.contains(longitude, latitude)==false )
		{
			return null;
		}
		else
		{
			if( mResorts != null )
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
			else
			{
				//test against all regions
				ResortInfo resort = null;
				float minDist = Float.MAX_VALUE;

				for( RegionInfo region : mRegions )
				{
					ResortInfo result = region.lookForResort(latitude, longitude);
					
					if( result != null )
					{
						float l = result.minDistance(latitude, longitude);
						if( resort == null || l < minDist )
						{
							resort = result;
							minDist = l;						
						}
					}
					
				}
				
				return resort;
			}
		}
	}
}
 
