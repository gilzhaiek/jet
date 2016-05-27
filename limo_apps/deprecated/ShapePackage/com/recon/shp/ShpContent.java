/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *ShpContent contains all the parsed information of an ERSI shape file
 *It has an instance of ShpHeader and an array-list of ShpRecord
 */

package com.recon.shp;

import java.util.ArrayList;

public final class ShpContent
{
	public ShpHeader shpHeader = null;
	public ArrayList<ShpRecord> shpRecords = null;
}