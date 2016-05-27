/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 *The ShpTools class contains static tool methods for working with
 *ESRI Shapefiles.
 */

package com.reconinstruments.mapImages.shp;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ShpTools 
{
	/**
	 * Reads all available ESRI Shape records from the specified ByteArray.
	 * Reading starts at the ByteArrays current offset.
	 * 
	 * @param src ByteArray to read ESRI Shape records from.
	 * @return An Array containing zoomero or more ShpRecord typed values.
	 * @see ShpRecord 
	 */
	public static ShpContent ReadRecords( ByteBuffer src )
	{
		ShpContent shp = new ShpContent( );
		shp.shpHeader = new ShpHeader(src);
		shp.shpRecords = new ArrayList<ShpRecord>( 1024 );
		
		ShpRecord record;
	
		while (true)
		{
			try 
			{
				record = new ShpRecord(src);
				shp.shpRecords.add(record);
			} 
			catch (ShpError e) 
			{
				if( e.errorId == ShpError.ERROR_NODATA )
				{
					//reach the end of the file, just break the loop
					break;
				}
				else
				{
					System.out.println("Un-recoverable error while reading in shape file. Force to exit");
					System.out.println( e.getMessage() );	
					e.printStackTrace(System.out);
					throw e;									
				}
			}
		}
		return shp;
	}
}
