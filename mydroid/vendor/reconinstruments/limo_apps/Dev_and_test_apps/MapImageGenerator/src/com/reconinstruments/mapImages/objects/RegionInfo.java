package com.reconinstruments.mapImages.objects;

public class RegionInfo extends ResortHolder{
	public RegionInfo(int id, String name, ResortHolder parentHolder) {
		super(id, name, parentHolder);
	}
	
	public static boolean IsMyInstance(ResortHolder resortHolder) {
		return resortHolder instanceof RegionInfo; 
	}			
}
