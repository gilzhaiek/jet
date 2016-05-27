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

package com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.dbf;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DbfRecord
{
	public boolean HasGRMNType = false;
	
	public String GRMNTypeFieldString = "";
	protected static final String DBF_FIELD_NAME		= "NAME";
	protected static final String DBF_FIELD_ONEWAY		= "ONE_WAY";
	protected static final String DBF_FIELD_SPDLIMIT	= "SPD_LIMIT";
	protected static final int NO_SPEED_LIMIT			= -1;
	
	/**
	 * Record field values. Use values.get("fieldName") to get a value(which is a string) 
	 */	
	public Map<String, String> values;
	
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

				if(!HasGRMNType && (field.name.equals("GRMN_TYPE"))) {
					HasGRMNType = true;
					GRMNTypeFieldString = fieldStr;
				}
			}
			catch( UnsupportedEncodingException e )
			{
				e.printStackTrace(System.out);
			}
							
		}		
	}
	
	public void Release(){
		if(values == null)
			return;
		
		values.clear();
	}
	
	public String GetName(){
		return values.get( DBF_FIELD_NAME ).trim();
	}
	
	public int GetSpeedLimit() {
		int spdLimit = NO_SPEED_LIMIT;
		try {
			String spdLimitStr = values.get( DBF_FIELD_SPDLIMIT );
			spdLimit = new Integer(spdLimitStr);
		}
		catch( Exception e ) {
			return NO_SPEED_LIMIT;
		}
		
		return spdLimit;
	}
	
	public boolean IsOneWay() {
		int oneWay = 0;
		try {
			String oneWayStr = values.get( DBF_FIELD_ONEWAY );
			oneWay = new Integer(oneWayStr);
		}
		catch( Exception e ) {
			return false;
		}
		
		return (oneWay > 0);
	}
}