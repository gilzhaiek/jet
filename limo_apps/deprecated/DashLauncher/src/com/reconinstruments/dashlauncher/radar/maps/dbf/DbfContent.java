/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *DbfContent contains all the parsed information of an ERSI dbf file
 *It has an instance of DbfHeader and an array-list of DbfRecord
 *Author: Hongzhi Wang at 2011
 */
package com.reconinstruments.dashlauncher.radar.maps.dbf;


public final class DbfContent
{
	public DbfHeader dbfHeader = null;
	//since we know the number of records after reading in DbfHeader
	//we using the array here for performance gain
	public DbfRecord[] dbfRecords = null;
	
	public void Release(){
		if(dbfHeader == null)
			return;

		if(dbfRecords != null) {
			try {
				for ( int i = 0; i < dbfHeader.recordCount; i++ ) 
				{
					if(dbfRecords[i] != null) {
						dbfRecords[i].Release();
						dbfRecords[i] = null;
					}
				}			
			}
			catch (Exception e) { }
			finally {
				dbfRecords = null;
			}
		}
		dbfHeader.Release();
		dbfHeader = null;
	}
}