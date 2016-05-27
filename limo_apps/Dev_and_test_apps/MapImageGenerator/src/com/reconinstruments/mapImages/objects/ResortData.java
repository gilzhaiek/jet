package com.reconinstruments.mapImages.objects;

import java.util.ArrayList;

public class ResortData {
	public ResortInfo		mResortInfo = null;
	public ArrayList<POI>	POIs = null;
	public ArrayList<Trail>	Trails = null;
	public ArrayList<Area>	Areas = null;
	
	public ResortData(){
		POIs = new ArrayList<POI>();
		Trails = new ArrayList<Trail>();
		Areas = new ArrayList<Area>();
	}
	
	public void Release(){
		for(int i = 0; i < POIs.size(); i++ ) {
			POIs.get(i).Release();
		}

		for(int i = 0; i < Trails.size(); i++ ) {
			Trails.get(i).Release();
		}
		
		for(int i = 0; i < Areas.size(); i++ ) {
			Areas.get(i).Release();
		}		

		POIs.clear();
		Trails.clear();
		Areas.clear();
		
		mResortInfo = null;
	}
	
	public void AddPOI(POI poi) {
		POIs.add(poi);
	}
	
	public void AddTrail(Trail trail) {
		Trails.add(trail);
	}
	
	public void AddArea(Area area) {
		Areas.add(area);
	}		
	
	public static boolean IsMyInstance(Object object) {
		return object instanceof ResortData; 
	}
}
