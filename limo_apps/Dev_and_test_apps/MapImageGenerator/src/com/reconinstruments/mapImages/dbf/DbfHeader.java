/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading DBF(XBASE FILE) file
 *It was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 *The DbfHeader class parses a DBF file loaded to a ByteBuffer
 */

package com.reconinstruments.mapImages.dbf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DbfHeader
{
	/**
	 * File length
	 */	
	public int fileLength;

	/**
	 * File version
	 */
	public int version;
	
	/**
	 * Date of last update, Year.
	 */
	public int updateYear;
	
	/**
	 * Date of last update, Month. 
	 */	
	public int updateMonth;
	
	/**
	 * Data of last update, Day. 
	 */	
	public int updateDay;
	
	/**
	 * Number of records on file. 
	 */	
	public int recordCount;
	
	/**
	 * Header structure size. 
	 */	
	public int headerSize;
	
	/**
	 * Size of each record.
	 */	
	public int recordSize;
	
	/**
	 * Incomplete transaction flag 
	 */	
	public int incompleteTransaction;
	
	/**
	 * Encrypted flag.
	 */	
	public int encrypted;
	
	/**
	 * DBase IV MDX flag. 
	 */	
	public int mdx;
	/**
	 * Language driver.
	 */	
	public int language;
	
	/**
	 * Array of DbfFields describing the fields found
	 * in each record. 
	 */	
	public ArrayList<DbfField>fields;
		
	private  int _recordsOffset;
				
	/**
	 * Constructor
	 * @param src
	 * @return 
	 * 
	 */	
	public DbfHeader(ByteBuffer src) 
	{
		// endian:
		src.order(ByteOrder.LITTLE_ENDIAN);	
		
		version = (int)src.get();
		updateYear = 1900+(int)src.get();
		updateMonth = src.get();
		updateDay = src.get();
		recordCount = src.getInt();
		headerSize = src.getShort();
		recordSize = src.getShort();
		
		//skip 2:
		src.getChar();
		
		incompleteTransaction = src.get();
		encrypted = src.get();
		
		// skip 12:
		src.position( src.position() + 12 );
		
		mdx = src.get();
		language = src.get();
		
		// skip 2;
		src.getChar();
		
		// iterate field descriptors:
		fields = new ArrayList<DbfField>(32);
		while (src.get() != 0X0D)
		{
			src.position( src.position() - 1 );
			fields.add(new DbfField(src));
		}
		
		_recordsOffset = headerSize+1;					
	}
	
	protected int recordsOffset()
	{
		return _recordsOffset;
	}	
	
	public void Release(){
		if(fields != null) {
			fields.clear();
		}
	}
}			
