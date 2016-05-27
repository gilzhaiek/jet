package com.reconinstruments.mapImages.objects;

public class CountryInfo  extends ResortHolder{
	public CountryInfo(int id, String name) {
		super(id, name);
	}
	
	public void AddRegion(RegionInfo regionInfo) {
		AddSortHolder((ResortHolder)regionInfo);
	}
	
	public RegionInfo GetRegion( String regionName ) {
		return (RegionInfo)GetResortHolder(regionName );
	}

	public static boolean IsMyInstance(ResortHolder resortHolder) {
		return resortHolder instanceof CountryInfo; 
	}	
}
