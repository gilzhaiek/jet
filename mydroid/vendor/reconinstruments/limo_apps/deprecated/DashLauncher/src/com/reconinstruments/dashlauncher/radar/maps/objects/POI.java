package com.reconinstruments.dashlauncher.radar.maps.objects;

import com.reconinstruments.dashlauncher.radar.prim.PointD;

public class POI {
	public static final int POI_TYPE_UNDEFINED		= -1;
	public static final int POI_TYPE_SKICENTER		= 0;		//resort POI: GRMN_TYPE=SKI_CENTER, MDTYPE1=Resort
	public static final int POI_TYPE_RESTAURANT		= 1;		//ski lodge:  GRMN_TYPE=RESTAURANT, or GRMN_TYPE=RESTAURANT_AMERICAN
	public static final int POI_TYPE_BAR			= 2;		//bar:  GRMN_TYPE=BAR
	public static final int POI_TYPE_PARK			= 3;		//terrain park main entrance: GRMN_TYPE=PARK
	public static final int POI_TYPE_CARPARKING		= 4;		//Car parking: GRMN_TYPE=PARKING, MDTYPE1=Parking
	public static final int POI_TYPE_RESTROOM		= 5;		//Toilet(Standalone, not part of the lodge): GRMN_TYPE=RESTROOM
	public static final int POI_TYPE_CHAIRLIFTING	= 6;		//chair-lifting loading: GRMN_TYPE=SKI_CENTER, MDTYPE1=LiftLoading
	public static final int POI_TYPE_SKIERDROPOFF_PARKING = 7;	//Parking for dropping off skiers: GRMN_TYPE=PARKING, MDTYPE1=Skier Dropoff
	public static final int POI_TYPE_INFORMATION	= 8;		//Ticket window: GMRN_TYPE=INFORMATION
	public static final int POI_TYPE_HOTEL			= 9;		//Hotel: GMRN_TYPE=HOTEL
	public static final int POI_TYPE_BANK			= 10;		//Bank: GRMN_TYPE=BANK
	public static final int POI_TYPE_SKISCHOOL		= 11;		//Skischool: GRMN_TYPE=SCHOOL
	public static final int POI_TYPE_BUDDY			= 12;		//Buddy around the site
	public static final int NUM_POI_TYPE			= 13;	

	
	public int		Type;
	public String	Name;
	public PointD	Location;
	
	public POI(){
		Location = null;
		Name = "";
		Type = POI_TYPE_UNDEFINED;		
	}
	
	public POI(int type, String name, PointD location){
		Location = location;
		Name = name;
		Type = type; 
	}
	
	public void Release(){
		Location = null;
	}
}
