package com.reconinstruments.dashlauncher.radar.maps.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ResortHolder {
	public int			ID;
	public String		Name;
	public ResortHolder	ParentHolder = null;
	
	public static final int NOID = -1;
	
	protected	Map<String, ResortHolder>	mHoldersMap	= new HashMap<String, ResortHolder>();
	protected	ArrayList<ResortHolder>		mHolders 	= new ArrayList<ResortHolder>();
	public		ArrayList<ResortInfo>		mResorts 	= new ArrayList<ResortInfo>();
	
	public ResortHolder()
	{
		ID = -1;
		Name = null;
	}

	public ResortHolder(int id, String name)
	{
		ID = id;
		Name = name;
	}	
	
	public ResortHolder(int id, String name, ResortHolder parentHolder)
	{
		ID = id;
		Name = name;
		ParentHolder = parentHolder;
	}	
	
	public void AddResort(ResortInfo resortInfo) {
		int index = 0;
		
		if( mResorts.size() != 0 )
		{			
			for(ResortInfo tmpResortInfo : mResorts )
			{
				if( tmpResortInfo.Name.compareTo(resortInfo.Name) >= 0 )
				{			
					break;
				}
				
				index++;
			}
		}
		
		mResorts.add(index, resortInfo );
	}

	protected void AddSortHolder(ResortHolder resortHolder) {
		int index = 0;
		
		if( mHolders.size() != 0 )
		{			
			for(ResortHolder tmpResortHolder : mHolders )
			{
				if( tmpResortHolder.Name.compareTo(resortHolder.Name) >= 0 )
				{			
					break;
				}
				
				index++;
			}
		}
		
		mHolders.add(index, resortHolder );
		mHoldersMap.put(resortHolder.Name, resortHolder);
	}
	
	protected ResortHolder GetResortHolder( String name ) {
		return mHoldersMap.get(name);
	}
		
}
