/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading DBF(XBASE FILE) file
 *It was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 * The DbfFilter class is a utility class that allows for collecting records
 * that match on one of the given values for a field.
 */

package com.reconinstruments.dashlauncher.radar.maps.dbf;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DbfFilter
{
	/**
	 * Array containing DbfRecord typed values that match on one of the given 
	 * values for a field. 
	 */	
	public ArrayList<DbfRecord> matches;
	
	/**
	 * Constructor.
	 * @param src ByteArray containing the DBF file to filter.
	 * @param header DbfHeader instance previously read from the ByteArray.
	 * @param field Field to filter on.
	 * @param values Array of values to match field against.
	 * @param append If specified, the found records will be added to the specified Array instead of to the instance's matches array.
	 * @return 
	 * @see DbfHeader
	 * 
	 */	
	public DbfFilter(ByteBuffer src, DbfHeader header, String field, ArrayList<Object> values, ArrayList<DbfRecord> append) 
	{
		if( append == null )
		{
			matches = new ArrayList<DbfRecord>( header.recordCount );
		}
		else
		{
			matches = append;
		}
		
		src.position( header.recordsOffset() );
	
		DbfRecord record;
		int i, j;
		
		for (i= 0; i<header.recordCount; i++)
		{
			record = DbfTools.GetRecord(src,header,i);
			for (j=0; j<values.size(); j++) 
			{
				if( record.values.get( field ).equals( values.get(j) ) )
				{
					matches.add( record );				
					break;
				}
			}			
		}				
	}
	
}