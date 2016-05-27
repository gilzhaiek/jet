package com.reconinstruments.dashlauncher.radar.maps.objects;

import java.util.ArrayList;

import android.graphics.PointF;

public class Trail {
	public static final int TRAIL_INVALID		= -1;
	public static final int GREEN_TRAIL			= 0;
	public static final int BLUE_TRAIL			= 1;
	public static final int BLACK_TRAIL			= 2;
	public static final int DBLBLACK_TRAIL		= 3;
	public static final int RED_TRAIL			= 4;
	public static final int GREEN_TRUNK			= 5;
	public static final int BLUE_TRUNK			= 6;
	public static final int BLACK_TRUNK			= 7;
	public static final int DBLBLACK_TRUNK		= 8;
	public static final int RED_TRUNK			= 9;
	public static final int SKI_LIFT			= 10;
	public static final int CHWY_RESID_TRAIL	= 11;			//highway or residential trial
	public static final int WALKWAY_TRAIL		= 12;		//walkway trail
	public static final int NUM_TRAIL_TYPES		= 13;
	
	public static final int NO_SPEED_LIMIT		= -1;

	public int					Type;
	public String				Name;
	public ArrayList<PointF>	TrailPoints;
	public int					SpeedLimit;
	public boolean				IsOneWay;
	
	
	public Trail(){
		Type = TRAIL_INVALID;
		Name = "";
		TrailPoints = null;
		SpeedLimit = NO_SPEED_LIMIT;
		IsOneWay = false;
	}
	
	public Trail(int type, String name, ArrayList<PointF> trailPoints, int speedLimit, boolean isOneWay){
		Type = type;
		Name = name;
		TrailPoints = trailPoints;
		SpeedLimit = speedLimit;
		IsOneWay = isOneWay;		
	}
	
	public void Release(){
		if(TrailPoints != null) {
			TrailPoints.clear();
		}
	}
}
