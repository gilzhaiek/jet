/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 * The ShpPoint class parses an ESRI Shapefile Record Header from a ByteBuffer
 * as well as its associated Shape Object. The parsed object is stored as a 
 * ShpObject that can be cast to a specialized ShpObject deriving class using 
 * the found shapeType value.
 */


package com.recon.shp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ShpRecord
{
	/**
	 * Record number 
	 */	
	public int number;

	/**
	 * Content length in 16-bit words 
	 */
	public int contentLength;
	
	/**
	 * Content length in bytes 
	 */
	public int contentLengthBytes;
	
	/**
	 * Type of the Shape Object associated with this Record Header.
	 * Should match one of the constant values defined in the ShpType class.
	 * @see ShpType
	 */	
	public int shapeType;
	
	/**
	 * Parsed Shape Object. Cast to the specialized ShpObject deriving class
	 * indicated by the shapeType property to obtain Shape type specific
	 * data. 
	 */	
	public ShpObject shape;
	
	/**
	 * Constructor.
	 * @param src
	 * @return 
	 * @throws ShpError Not a valid header
	 * @throws Shape type is currently unsupported by this library
	 * @throws Encountered unknown shape type
	 * 
	 */	
	public  ShpRecord( ByteBuffer src ) 
	{
		int availableBytes = src.remaining();
		
		if (availableBytes == 0) 
		{
			ShpError e = new ShpError("");
			e.errorId = ShpError.ERROR_NODATA;
			throw e;
		}
			
		if (availableBytes < 8)
			throw(new ShpError("Not a valid record header (too small)"));
	
		src.order( ByteOrder.BIG_ENDIAN );

		number = src.getInt();
		contentLength = src.getInt();
		contentLengthBytes = contentLength*2 - 4;			
		src.order( ByteOrder.LITTLE_ENDIAN );
		shapeType = src.getInt();
				
		switch(shapeType) 
		{
			case ShpType.SHAPE_POINT:
				shape = new ShpPoint(src,contentLengthBytes);
				break;
			case ShpType.SHAPE_POINTZ:
				shape = new ShpPointZ(src,contentLengthBytes);
				break;
			case ShpType.SHAPE_POLYGON:
				shape = new ShpPolygon(src, contentLengthBytes);
				break;
			case ShpType.SHAPE_POLYLINE:
				shape = new ShpPolyline(src, contentLengthBytes);
				break;
			case ShpType.SHAPE_MULTIPATCH:
			case ShpType.SHAPE_MULTIPOINT:
			case ShpType.SHAPE_MULTIPOINTM:
			case ShpType.SHAPE_MULTIPOINTZ:
			case ShpType.SHAPE_POINTM:
			case ShpType.SHAPE_POLYGONM:
			case ShpType.SHAPE_POLYGONZ:
			case ShpType.SHAPE_POLYLINEZ:
			case ShpType.SHAPE_POLYLINEM:
				throw(new ShpError(shapeType+" Shape type is currently unsupported by this library"));
			default:	
				throw(new ShpError("Encountered unknown shape type ("+shapeType+")"));
		}
					
	}
}