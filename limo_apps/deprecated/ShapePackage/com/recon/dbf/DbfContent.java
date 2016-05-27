/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *DbfContent contains all the parsed information of an ERSI dbf file
 *It has an instance of DbfHeader and an array-list of DbfRecord
 *Author: Hongzhi Wang at 2011
 */
package com.recon.dbf;


public final class DbfContent
{
	public DbfHeader dbfHeader = null;
	//since we know the number of records after reading in DbfHeader
	//we using the array here for performance gain
	public DbfRecord[] dbfRecords = null;
}