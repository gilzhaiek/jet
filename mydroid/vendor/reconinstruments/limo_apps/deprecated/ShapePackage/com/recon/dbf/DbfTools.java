/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading DBF(XBASE FILE) file
 *It was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 * The DbfTools class bundles a utility functions used by the remainder of
 * the DBF library.
 */

package com.recon.dbf;

import java.nio.ByteBuffer;


public class DbfTools
{
	/**
	 * Read a zero terminated ANSI string from a ByteArray.
	 * @param src ByteBuffer instance to read from.
	 * @return 
	 * 
	 */	
	public static String readZeroTermANSIString(ByteBuffer src) 
	{
		byte[] b;
	
		int startIdx = src.position();
		int endIdx = startIdx;
		while( src.get(endIdx) != 0 ) 
		{
			++endIdx;
		}
		
		b = new byte[endIdx-startIdx];
		src.get( b );
		
		//skip the ending zero byte of the string
		src.get( );
		
		String str = null;
		try
		{
			 str = new String( b, "UTF-8" );
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
		
		return str;
		
	}
	
	/**
	 * Read a fixed length ANSI string from a ByteArray.
	 * @param src ByteBuffer instance to read from.
	 * @param length Number of character to read.
	 * @return 
	 * 
	 */	
	public static String readANSIString( ByteBuffer src, int length )
	{
		byte[] b = new byte[length];
		src.get( b );
		String str = null;
		try
		{
			str = new String( b, "UTF-8" );			
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
		
		return str;
	}
	
	/**
	 * Read a DBF record from a DBF file.
	 * @param src ByteBuffer instance to read from.
	 * @param header DbfHeader instance previously read from the ByteArray.
	 * @param index Index of the record to read.
	 * @return 
	 * @see DbfHeader
	 * 
	 */	
	public static DbfRecord getRecord(ByteBuffer src, DbfHeader header, int index)
	{
		if (index > header.recordCount) 
		{
			DbfError e = new DbfError("");
			e.errorId = DbfError.ERROR_OUTOFBOUNDS;
			throw e;
		}
					
		src.position( header.recordsOffset() + index * header.recordSize );
		return new DbfRecord(src, header);
	}
	
	/**
	 * Reads all available dbf records from the specified ByteBuffer.
	 * Reading starts at the ByteBuffer current offset.
	 * 
	 * @param src ByteBuffer to read dbf records from.
	 * @return An instance of DbfContent
	 * @see DbfRecord/DbfHeader/DbfContent 
	 */
	public static DbfContent readRecords( ByteBuffer src ) 
	{
		DbfContent dbfContent = new DbfContent();
		dbfContent.dbfHeader = new DbfHeader(src);
		dbfContent.dbfRecords = new DbfRecord[dbfContent.dbfHeader.recordCount ];
		
		// Assign attribute dictionaries to dbfRecords.
		for ( int i = 0; i < dbfContent.dbfHeader.recordCount; i++ ) 
		{
			dbfContent.dbfRecords[i] =  DbfTools.getRecord(src,dbfContent.dbfHeader,i);
		}
		
		return dbfContent;
	}
}