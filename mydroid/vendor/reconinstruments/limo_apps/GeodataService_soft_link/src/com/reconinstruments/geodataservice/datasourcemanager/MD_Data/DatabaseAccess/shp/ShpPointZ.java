/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 *The ShpPointZ class parses an ESRI Shapefile PointZ record from a ByteBuffer.
 */

package com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp;

import java.nio.ByteBuffer;

public class ShpPointZ extends ShpPoint
{
	/**
	 * Z value
	 */
	public double z;
	
	/**
	 * M value (measure)
	 */ 
	public double m; // Measure;
	
	/**
	 * Constructor
	 * @param src
	 * @param size
	 * @return	 
	 * 
	 */	
	public ShpPointZ( ByteBuffer src, int size ) 
	{
		super( src, size );
		type = ShpType.SHAPE_POINTZ;
		if ( src!=null ) 
		{			
			z = (size > 16) ? src.getDouble() : Double.NaN;			
			m = (size > 24) ? src.getDouble() : Double.NaN;
		}		
	}
}
