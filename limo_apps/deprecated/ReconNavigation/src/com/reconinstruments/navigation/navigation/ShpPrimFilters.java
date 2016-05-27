/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation;

import java.util.HashMap;
import java.util.Map;

import com.recon.dbf.DbfRecord;

public class ShpPrimFilters
{
	//one of the predefined ShpType
	public  int mPrimType;
	
	//the attribute name used to filter out the prims
	private String mAttributeName;
	
	private Map<String, Object>mFilters;
	
	public  ShpPrimFilters( int pType, String attrName )
	{
		mPrimType = pType;
		mAttributeName = attrName;
		mFilters = new HashMap<String,Object>();
	}
	
	
	public void addFilter( String filter, Object value  )
	{
		mFilters.put(filter, value);
	}
	
	public Boolean isFiltered( String filter )
	{
		return mFilters.containsKey(filter)==false;
	}
	
	public Boolean isDbfRecordFiltered( DbfRecord dbfRecord )
	{
		if( dbfRecord.values.containsKey(mAttributeName) == false )
		{
			return true;
		}
		else
		{
			String filterStr = dbfRecord.values.get(mAttributeName);
			filterStr = filterStr.trim();			
			return isFiltered( filterStr );
		}
	}
	
	public Object getDbfRecordFilterValue( DbfRecord dbfRecord   )
	{
		String filterStr = dbfRecord.values.get(mAttributeName);			
		filterStr = filterStr.trim();
		
		return getFilterValue( filterStr );
	}
	
	public Object getFilterValue( String filter  )
	{
		return mFilters.get(filter);
	}
	
}