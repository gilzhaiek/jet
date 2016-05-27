/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation;

import java.util.ArrayList;

import com.recon.dbf.DbfRecord;

public class ShpMapFilters
{
	//the natme of the attribute for filtering Polylines
	private ArrayList<ShpPrimFilters> mPrimFilters;
	
	public ShpMapFilters( )
	{
		//initialize with three type of filters: Line, Area and PoI
		mPrimFilters = new ArrayList<ShpPrimFilters>( 3 );
	}
	
	public void reset( )
	{
		mPrimFilters.clear();
	}	
		
	public void addFilter( ShpPrimFilters primFilter )
	{
		ShpPrimFilters filter = findFilter( primFilter.mPrimType );
		if( filter != null )
			throw new Error("prim filter already defined" ); 
		
		mPrimFilters.add( primFilter );
	}
	
	private ShpPrimFilters findFilter( int primType )
	{
		for( ShpPrimFilters primFilter : mPrimFilters )
		{
			if( primFilter.mPrimType == primType )
				return primFilter;
		}
		
		return null;
	}
	
	public Boolean isDbfRecordFiltered( DbfRecord dbfRecord, int primType )
	{
		ShpPrimFilters primFilter = findFilter( primType );
		
		return primFilter == null ? false : primFilter.isDbfRecordFiltered( dbfRecord );
	}
	
	public Boolean isFiltered( String filter, int primType )
	{
		ShpPrimFilters primFilter = findFilter( primType );
		
		if( primFilter == null )
			return false;
		else
		{
			return primFilter.isFiltered( filter );
		}
	}
	
	public Object getDbfRecordFilterValue( DbfRecord dbfRecord, int primType )
	{
		ShpPrimFilters primFilter = findFilter( primType );
		return primFilter.getDbfRecordFilterValue( dbfRecord );
	}
	
	public Object getFilterValue( String filter, int primType )
	{
		ShpPrimFilters primFilter = findFilter( primType );
		if( primFilter == null )
			return null;
		else
		{
			return primFilter.getFilterValue( filter );		
		}
	}
}
