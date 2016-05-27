/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 *The ShpPolyline class parses an ESRI Shapefile Polyline record from a ByteBuffer.
 */

package com.reconinstruments.mapImages.shp;

import java.nio.ByteBuffer;


public final class ShpPolyline extends ShpPolygon
{
	/**
	 * Constructor.
	 * @inherit
	 * @param src
	 * @param size
	 * @return 
	 * 
	 */	
	public ShpPolyline(ByteBuffer src, int size) 
	{
		super(src,size);
		type = ShpType.SHAPE_POLYLINE;		
	}
}