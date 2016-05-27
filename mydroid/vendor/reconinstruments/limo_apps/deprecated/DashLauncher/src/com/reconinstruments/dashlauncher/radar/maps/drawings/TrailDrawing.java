package com.reconinstruments.dashlauncher.radar.maps.drawings;

import java.util.ArrayList;

import com.reconinstruments.dashlauncher.radar.maps.objects.Trail;
import com.reconinstruments.dashlauncher.radar.prim.PointD;

public class TrailDrawing extends PathDrawing{
	public Trail	mTrail;
	
	public TrailDrawing(ArrayList<PointD> pathPoints, Trail trail) {
		super(pathPoints);
		mTrail = trail;
	}
	
	public void Release(){
		super.Release();
		mTrail = null;
	}
}
