package com.reconinstruments.dashlauncher.radar.maps.drawings;

import java.util.ArrayList;

import com.reconinstruments.dashlauncher.radar.maps.objects.Area;
import com.reconinstruments.dashlauncher.radar.prim.PointD;

public class AreaDrawing extends PathDrawing{
	public Area		mArea;
	
	public AreaDrawing(ArrayList<PointD> pathPoints, Area area) {
		super(pathPoints);
		mArea = area;
	}
	
	public void Release(){
		super.Release();
		mArea = null;
	}
}
