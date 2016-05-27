/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading DBF(XBASE FILE) file
 *It was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 * Instances of the DbfError class are thrown from the DBF library classes
 * on encountering errors.
 */

package com.reconinstruments.mapImages.dbf;


public class DbfError extends java.lang.Error
{
	/**
	 *just for fixing the warning
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Defines the identifier value of an undefined error.  
	 */	
	public static final int ERROR_UNDEFINED		= 0;
	
	/**
	 * Defines the identifier value of a 'out of bounds' error, which is thrown
	 * when an invalid item index is passed.
	 */	
	public static final int ERROR_OUTOFBOUNDS	= 1;
	
	public int errorId = ERROR_UNDEFINED;
	
	public DbfError(String msg)
	{
		super(msg);
	}
}