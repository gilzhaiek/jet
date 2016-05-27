/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 *Instances of the ShpError class are thrown from the SHP library classes
 *on encountering errors.* 
 */

package com.recon.shp;

public class ShpError extends java.lang.Error
{
	/**
	 * for fixing the warning
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Defines the identifier value of an undefined error.  
	 */	
	public static final int ERROR_UNDEFINED		= 0;
	/**
	 * Defines the identifier value of a 'no data' error, which is thrown
	 * when a ByteArray runs out of data.
	 */	
	public static final int ERROR_NODATA	= 1;
	
	public int errorId = ERROR_UNDEFINED;
	
	/**
	 * Constructor.
	 * @param message
	 * @return 
	 */	
	public ShpError( String message )
	{
		super( message );
	}
}
