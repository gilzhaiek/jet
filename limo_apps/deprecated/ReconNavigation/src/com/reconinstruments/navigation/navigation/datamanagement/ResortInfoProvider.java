/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation.datamanagement;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.os.Environment;
import android.util.Log;

/**
 *This is a utility class for managing
 *all resort information so that the
 *resort can be searched or listed out
 *for user to select
 */
public class ResortInfoProvider
{	
	//a hash map from the country/region ID to the country/region name
	private static Map<Integer, String> sCountryID2NameMap = new HashMap<Integer, String>( 400 );

	//the list of the countries that have ski resorts with map available
	public static ArrayList<CountryInfo> sCountries = new ArrayList<CountryInfo>( 400 );
	
	public static ResortAssetEnumerator sAssetEnumerator = null;
	
	public static void reset( )
	{
		sCountryID2NameMap.clear();
		sCountries.clear();
		if( sAssetEnumerator == null )
		{
			sAssetEnumerator = new  ResortAssetEnumerator( getAssetFolder() );
		}
		
		sAssetEnumerator.reset( );
		
	}
	
	public static String getAssetFolder( )
	{
		File externalStorage = Environment.getExternalStorageDirectory();
		
		return externalStorage.getPath() + "/ReconApps/MapData/";
	}
	
	public static String getNavigationFolder( )
	{
		File externalStorage = Environment.getExternalStorageDirectory();
		
		return externalStorage.getPath() + "/ReconApps/Navigation/";
	}
	
	//add a mapping entry from ID to country/regionName
	public static void addMapping( int countryID, String name )
	{
		String prev = sCountryID2NameMap.put(countryID, name);
		if( prev != null )
		{
			Log.e("Data Management", "Country/Region ID: " + countryID + " is duplicated");
		}
	}
	
	//given an id of country/region, return the name 
	public static String getCountryRegionName( int id )
	{
		return sCountryID2NameMap.get(id);
	}
	
	public static void addCountry( int countryID, String name )
	{
		CountryInfo country = new CountryInfo( countryID, name );
		
		//sort the sCountries by country name
		if( sCountries.size() == 0 )
		{
			sCountries.add( country );
		}
		else
		{
			int idx = 0;
			for( CountryInfo existed : sCountries )
			{
				if( existed.mName.compareTo(name) >= 0 )
				{					
					break;
				}
				++idx;
			}
			sCountries.add( idx, country );
		}
		
	}
	
	//given a country/region ID, search for a countryInfo. If
	//none existed yet, return null;
	public static CountryInfo getCountryInfo( int countryID )
	{
		for( CountryInfo countryInfo : sCountries )
		{
			if( countryInfo.isMyID(countryID) )
				return countryInfo;
		}
		
		return null;
	}
	
	public static CountryInfo getCountryInfo( String countryName )
	{
		for( CountryInfo countryInfo : sCountries )
		{
			if( countryInfo.mName.equals(countryName) )
				return countryInfo;
		}
		
		return null;
	}
	
	//remove country and region entries what have not resort attached
	public static void compactList( )
	{
		for( int i = sCountries.size() - 1; i >= 0; --i )
		{
			CountryInfo countryInfo = sCountries.get(i);
			
			if( countryInfo.mID != CountryInfo.COUNTRY_ID_UNDEFINED && countryInfo.mResorts == null )
			{
				sCountries.remove(i);
			}
			else if( countryInfo.mID == CountryInfo.COUNTRY_ID_UNDEFINED )
			{
				for( int j = countryInfo.mRegions.size() - 1; j >= 0; --j )
				{
					RegionInfo region = countryInfo.mRegions.get(j);
					if(region.mResorts == null)
					{
						countryInfo.mRegions.remove(j);
					}
				}
			}
		}
	}
	
	/**
	 * Get a list of countries that has ski resort maps available on device
	 */
	public static ArrayList<CountryInfo> getAvailableCountries()
	{
		//quickly find out how many countries having available resorts
		int num = 0;		
		for( CountryInfo info : sCountries )
		{
			if( info.mNumAvailableResorts > 0 )
			{
				++num;
			}
		}
		
		if( num > 0 )
		{
			ArrayList<CountryInfo> list = new ArrayList<CountryInfo>( num );
			for( CountryInfo info : sCountries )
			{
				if( info.mNumAvailableResorts > 0 )
				{
					list.add(info);
				}
			}
			
			return list;
		}
		else
		{
			return null;
		}
		
	}
	
	/**
	 * Check if there are any resort maps available on the device 
	 */
	public static boolean hasResortMapsAvailable( )
	{
		for( CountryInfo info : sCountries )
		{
			if( info.mNumAvailableResorts > 0 )
				return true;
		}
		
		return false;
	}
	
	static public ResortInfo getResort( int countryID, int resortID )
	{
		ResortInfo resortInfo = null;
		for( CountryInfo info : sCountries )
		{
			if(info.mID == countryID || info.mID == CountryInfo.COUNTRY_ID_UNDEFINED)
			{
				resortInfo = info.getResort(resortID);
				
				if(resortInfo != null)
				{
					return resortInfo;
				}
			}
		}
		
		return resortInfo;	
	}	
	
	/**
	 *Given a location( latitude, longitude ),
	 *search for a resort that contains this location
	 *if multiple resorts contain this location
	 *return the one that is closest to this location
	 *by testing the location to the five position of the resort
	 */
	static public ResortInfo lookForResort( float latitude, float longitude )
	{
		//test against all regions
		ResortInfo resort = null;
		float minDist = Float.MAX_VALUE;
		for( CountryInfo info : sCountries )
		{
			ResortInfo result = info.lookForResort(latitude, longitude);
			
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
 