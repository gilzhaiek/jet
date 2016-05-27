/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 *The ShpHeader class parses an ESRI Shapefile Header from a ByteBuffer.
 */

package com.recon.shp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.recon.prim.PointD;
import com.recon.prim.RectangleD;

public class ShpHeader
{
	/**
	 * Size of the entire Shapefile as stored in the Shapefile, in bytes.
	 */	
	public int fileLength;
	/**
	 * Shapefile version. Expected value is 1000. 
	 */		
	public int version;
	/**
	 * Type of the Shape records contained in the remainder of the
	 * Shapefile. Should match one of the constant values defined
	 * in the ShpType class.
	 * @see ShpType
	 */	
	public int shapeType;
	/**
	 * The cartesian bounding box of all Shape records contained
	 * in this file.
	 */	
	public RectangleD boundsXY;
	/**
	 * The minimum (Point.x) and maximum Z (Point.y) value expected
	 * to be encountered in this file.
	 */	
	public PointD boundsZ;
	/**
	 * The minimum (Point.x) and maximum M (Point.y) value expected
	 * to be encountered in this file.
	 */	
	public PointD boundsM;
	
	/**
	 * Constructor.
	 * @param src
	 * @return
	 * @throws ShpError Not a valid shape file header
	 * @throws ShpError Not a valid signature
	 * 
	 */			
	public  ShpHeader( ByteBuffer src ) 
	{
		// set as big-endian for content reading
		src.order( ByteOrder.BIG_ENDIAN );
		
		// check length:
		if ( src.remaining()< 100 )
		{	
			throw (new ShpError("Not a valid shape file header (too small)"));
		}
		
		// check signature	
		if( src.getInt() != 9994 )
		{
			throw (new ShpError("Not a valid signature. Expected 9994"));
		}
		
		 // skip 5 integers;
		int oldPos = src.position();
		src.position( oldPos + 5*4 );
		
		// read file-length:
		fileLength = src.getInt( );
		
		// switch endian:
		src.order( ByteOrder.LITTLE_ENDIAN );
		
		// read version:
		version = src.getInt( );
				
		// read shape-type:
		shapeType = src.getInt();
		
		// read bounds:
		boundsXY = new RectangleD
			( src.getDouble(), src.getDouble()
			, src.getDouble(), src.getDouble()
			);
		
		boundsZ = new PointD( src.getDouble(), src.getDouble() );
		boundsM = new PointD( src.getDouble(), src.getDouble() );				
	}
}
