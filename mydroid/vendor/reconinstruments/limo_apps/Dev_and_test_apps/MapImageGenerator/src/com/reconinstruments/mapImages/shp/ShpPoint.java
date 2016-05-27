/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 *The ShpPoint class parses an ESRI Shapefile Point record from a ByteBuffer.
 */

package com.reconinstruments.mapImages.shp;

import java.nio.ByteBuffer;

public class ShpPoint extends ShpObject
{
	/**
	 * Constructor
	 * @throws ShpError Not a Point record 
	 */
	public double x; 
	public double y;
	
	public ShpPoint( ByteBuffer src, int size ) 
	{
		type = ShpType.SHAPE_POINT;
		if (src != null) 
		{
			if (src.remaining() < size) 
			{
				throw new ShpError("Not a Point record (to small)");
			}

			x = (size > 0) ? src.getDouble() : Double.NaN;
			y = (size > 8) ? src.getDouble() : Double.NaN;
			
		}
	}
}
