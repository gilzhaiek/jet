package com.reconinstruments.dashlauncher.radar.maps.bo;

import java.util.ArrayList;

import com.reconinstruments.dashlauncher.radar.maps.common.CommonDrawings;
import com.reconinstruments.dashlauncher.radar.maps.drawings.AreaDrawing;
import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;
import com.reconinstruments.dashlauncher.radar.maps.objects.Area;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortInfo;
import com.reconinstruments.dashlauncher.radar.prim.PointD;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class AreaDrawingBO {
	private final static String TAG = "AreaDrawingBO";
	
	public boolean	mInitialized = false;
	private int[] 	mAreaColors	= null; 
	private int[] 	mAreaAlphas	= null;
	private Paint[]	mAreaPaints = null;
	
	private LocationTransformer mLocationTransformer = null;
	
	public AreaDrawingBO(LocationTransformer locationTransformer) {
		mLocationTransformer = locationTransformer;
	}
	
	public void InitPaint(){  
		if(mInitialized)
			return;
		
		// TODO change this to resource
		mAreaColors = new int[Area.NUM_AREA_TYPES];
		mAreaColors[Area.AREA_WOODS]	= CommonDrawings.AREA_WOODS_COLOR;
		mAreaColors[Area.AREA_SCRUB]	= CommonDrawings.AREA_SCRUB_COLOR;
		mAreaColors[Area.AREA_TUNDRA]	= CommonDrawings.AREA_TUNDRA_COLOR;
		mAreaColors[Area.AREA_PARK]		= CommonDrawings.AREA_PARK_COLOR;
		
		mAreaAlphas = new int[Area.NUM_AREA_TYPES];
		mAreaAlphas[Area.AREA_WOODS]	= 255;
		mAreaAlphas[Area.AREA_SCRUB]	= 255;
		mAreaAlphas[Area.AREA_TUNDRA]	= 255;
		mAreaAlphas[Area.AREA_PARK]		= 255;
		
		mAreaPaints = new Paint[ Area.NUM_AREA_TYPES ];
		for( int idx = 0; idx < Area.NUM_AREA_TYPES; ++idx )
		{
			Paint paint = new Paint( );
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(mAreaColors[idx]);
			paint.setAlpha(mAreaAlphas[idx]);
			mAreaPaints[idx] = paint;
		}
		
		
		mInitialized = true;
	}
	
	public void Draw(Canvas canvas, AreaDrawing areaDrawings) {
		if(!mInitialized)
			InitPaint();
				
		canvas.drawPath(areaDrawings.GetPath(), mAreaPaints[areaDrawings.mArea.Type]);
	}
	
	public AreaDrawing CreateTransformedAreaDrawing(Area area, ResortInfo resortInfo) {
		ArrayList<PointD> pathPoints = new ArrayList<PointD>(area.AreaPoints.size());
		
		int idx = 0;
		for( PointD pointD : area.AreaPoints)
		{
			pathPoints.add(idx++, new PointD(
					mLocationTransformer.TransformLongitude(pointD.x),
					mLocationTransformer.TransformLatitude(pointD.y)));
		}
		
		AreaDrawing areaDrawing = new AreaDrawing(pathPoints, area);
		
		return areaDrawing;
	}
}
