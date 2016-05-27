/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation.datamanagement;

import android.graphics.PointF;
import android.graphics.RectF;

/**
 * Cache resort related information: name, id,
 * hub, longitude, latitude etc
 */
public class ResortInfo
{
	public int 		mID;			// resort ID
	public String	mName;			// resort name
	public int  	mCountryID;		// the country-region ID, where the resort located on
	public PointF	mHubLocation;	// the hub location of this resort in (lng, lat)
	public RectF 	mBBox;			// the bounding box of the resort in (lng, lat)
	public String	mAssetBaseName;	// the asset base name, which is string of the ID
	public boolean	mIsAvailable;	// the resort is available for navigation on the device or not
	public int		mMapVersion;	// the version of map. For now, there is only one value 1 : mountain dynamic; 0: no map data
	
	public ResortInfo( int id, String name, int countryID, PointF hubLocation, int mapVersion, RectF bbox )
	{
		mID = id;
		mName = name;
		mCountryID = countryID;
		mHubLocation = hubLocation;
		mBBox = bbox;
		mIsAvailable = false;
		mMapVersion = mapVersion;
		mAssetBaseName = "" + id;
	}
	
	/**
	 * Override the default toString function to output the resort
	 * name as the string to feed to an ArrayAdapter to render on
	 * a listView
	 */
	@Override
	public String toString()
	{
		return mName;
	}
	
	/**
	 * given a location (latitiude, longitude), test if this location is within this resort
	 */	
	public boolean contains( float latitude, float longitude )
	{
		return mBBox.contains(longitude, latitude);
	}
	
	/**
	 * given a position( latitude, longitude ), calcualte the minimum distance from the location
	 * to one of the five points(four corner and the center)  
	 */
	public float minDistance( float latitude, float longitude )
	{
		PointF p = new PointF();
		p.set( mBBox.left, mBBox.top );
		p.offset(-longitude, -latitude);
		float l = p.length( );
		
		p.set( mBBox.left, mBBox.bottom );
		p.offset(-longitude, -latitude);
		l = l < p.length( ) ? l : p.length( );
		
		p.set( mBBox.right, mBBox.top );
		p.offset(-longitude, -latitude);
		l = l < p.length( ) ? l : p.length( );
		
		p.set( mBBox.right, mBBox.bottom );
		p.offset(-longitude, -latitude);
		l = l < p.length( ) ? l : p.length( );

		p.set( mBBox.centerX(), mBBox.centerY());
		p.offset(-longitude, -latitude);
		l = l < p.length( ) ? l : p.length( );
		
		return l;
	}
}