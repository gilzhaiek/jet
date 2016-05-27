/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 *The ShpObject class is the base class of all specialized Shapefile
 *record type parsers.
 */
package com.reconinstruments.dashlauncher.radar.maps.shp;
		
public class ShpObject
{
	/**
	 * Type of this Shape object. Should match one of the constant 
	 * values defined in the ShpType class.
	 * @see ShpType
	 */	
	public int type;	
}
