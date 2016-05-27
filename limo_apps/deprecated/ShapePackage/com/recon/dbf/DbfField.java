/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading DBF(XBASE FILE) file
 *It was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 * The DbfField class parses a field definition from a DBF file loaded to a
 * ByteArray.
 */

package com.recon.dbf;
	
import java.nio.ByteBuffer;


public class DbfField
{
	/**
	 * Field name. 
	 */	
	public String name;
	
	/**
	 * Field type. 
	 */	
	public int type;
	
	/**
	 * Field address.
	 */	
	public int address;
	
	/**
	 * Field lenght. 
	 */	
	public int length;
	
	/**
	 * Field decimals.
	 */	
	public int decimals;
	
	/**
	 * Field id.
	 */	
	public int id;
	
	/**
	 * Field set flag. 
	 */	
	public int setFlag;
	
	/**
	 * Field index flag. 
	 */	
	public int indexFlag;
	
	/**
	 * Constructor.
	 * @param src
	 * @return 
	 * 
	 */			
	public DbfField(ByteBuffer src) 
	{
	
		name = DbfTools.readZeroTermANSIString(src);
		
		// fixed length: 10, so:
		src.position( src.position() + (10-name.length()));
	
		//remove the leading and appending white space from the name str
		name = name.trim();
		
		type = (int)src.get();
		
		//trace(name + ": " + type);
		address = src.getInt();
		length = (int)src.get();
		decimals = (int)src.get();
		
		// skip 2: (Java Char takes 2 bytes)
		src.getChar();
		
		id = (int)src.get();
		
		// skip 2:
		src.getChar();
		
		setFlag = (int)src.get();
		
		// skip 7:
		src.position( src.position() + 7 );
		
		indexFlag = (int)src.get();
		
		
	}
}
