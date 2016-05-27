/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 *The ShpPolygon class parses an ESRI Shapefile Polygon record from a ByteBuffer.
 */


package com.recon.shp;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import com.recon.prim.RectangleD;

public class ShpPolygon extends ShpObject
{
	/**
	 * Cartesian bounding box of all the rings found in this Polygon record.
	 */	
	public RectangleD box;

	/**
	 * Array containing zero or more Arrays containing zero or more ShpPoint
	 * typed values, constituting the rings found in this Polygon record.
	 * @see ShpPoint 
	 */	
	public ArrayList<ArrayList<ShpPoint>>rings = null;
	
	/**
	 * Constructor.
	 * @param src
	 * @param size
	 * @return 
	 * @throws ShpError Not a Polygon record
	 */	
	public ShpPolygon( ByteBuffer src, int size ) 
	{
		type = ShpType.SHAPE_POLYGON;
		
		if (src!=null) 
		{			
			if (src.remaining() < size)
				throw(new ShpError("Not a Polygon record (to small)"));
			
			src.order( ByteOrder.LITTLE_ENDIAN );
			
			box = new RectangleD
				( src.getDouble(), src.getDouble()
				, src.getDouble(), src.getDouble()
				);
				
			//count of ring
			int rc = src.getInt();
			//count of parts
			int pc = src.getInt();
			
			//reserve the capacity of the ring ArrayList with rc
			rings = new ArrayList<ArrayList<ShpPoint>>( rc );
			
			int[] ringOffsets = new int[rc];
			int idx = 0;
			for( idx=0; idx < rc; ++idx )
			{
				ringOffsets[idx] = src.getInt();
			}
			
			ShpPoint[] points = new ShpPoint[pc];
			for( idx = 0; idx < pc; ++idx )
			{
				points[idx] = new ShpPoint( src,16 );
			}

			//there are more than one rings
			if( rc > 1 )
			{
				int startIdx = 0;
				for( idx = 0; idx < ringOffsets.length; ++idx )
				{
					//preserve the capacity of the ArrayList as ringOffsets[idx] for performance
					//gain.
					ArrayList<ShpPoint> newRing = new ArrayList<ShpPoint>( ringOffsets[idx] );
					for( int pointIdx = startIdx; pointIdx < ringOffsets[idx]; ++pointIdx )
					{
						newRing.add( points[pointIdx] );
					}
					startIdx = ringOffsets[idx];
					rings.add( newRing );
				}					
				
			}
			else	//only one ring
			{
				//We know how many points are with the ring, so preserve the capacity of the ArrayList 
				//as ringOffsets[idx] for performance gain			
				ArrayList<ShpPoint> newRing = new ArrayList<ShpPoint>( pc );
				for( int pointIdx = 0; pointIdx < pc; ++pointIdx )
				{
					newRing.add( points[pointIdx] );
				}
				rings.add( newRing );	
				
			}
		}		
	}
}