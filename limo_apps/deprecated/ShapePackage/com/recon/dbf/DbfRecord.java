/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading DBF(XBASE FILE) file
 *It was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 * The DbfRecord class parses a record from a DBF file loaded to a ByteBuffer.
 * To do so it requires a DbfHeader instance previously read from the 
 * ByteBuffer.
 */

package com.recon.dbf;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DbfRecord
{
	/**
	 * Record field values. Use values.get("fieldName") to get a value(which is a string) 
	 */	
	public Map<String, String> values;
	
	//never referred by local code
	//private int offset;
	
	public DbfRecord(ByteBuffer src, DbfHeader header) 
	{
		values = new HashMap<String,String>( header.fields.size() );
		for ( DbfField field : header.fields ) 
		{
			byte[] fieldValue = new byte[field.length];
			src.get( fieldValue, 0, field.length );
			try
			{
				//construct a UTF-8 string from the byte array read from the src byteBuffer
				String fieldStr = new String( fieldValue, "UTF-8");
				//remove the leading and appending white space from the fieldStr
				fieldStr = fieldStr.trim();
				values.put( field.name, fieldStr );	
			}
			catch( UnsupportedEncodingException e )
			{
				e.printStackTrace(System.out);
			}
							
		}		
	}
}