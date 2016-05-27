package com.reconinstruments.mapImages.objects;

import java.util.ArrayList;

import com.reconinstruments.mapImages.prim.PointD;

public class Area {
	public static final int AREA_INVALID	= -1;
	public static final int AREA_WOODS		= 0;
	public static final int AREA_SCRUB		= 1;
	public static final int AREA_TUNDRA		= 2;
	public static final int AREA_PARK		= 3;
	public static final int NUM_AREA_TYPES	= 4;
	
	public int					Type;
	public String				Name;
	public ArrayList<PointD>	AreaPoints;
	
	public Area() {
	}
	
	public Area(int type, String name, ArrayList<PointD> areaPoints) {
		Type		= type;
		Name		= name;
		AreaPoints	= areaPoints;
	}
	
	public void Release(){
		AreaPoints.clear();		
	}
}
