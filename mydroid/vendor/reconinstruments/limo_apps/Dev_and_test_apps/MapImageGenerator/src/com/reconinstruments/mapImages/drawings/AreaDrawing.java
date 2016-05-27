package com.reconinstruments.mapImages.drawings;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.reconinstruments.mapImages.common.CommonDrawings;
import com.reconinstruments.mapImages.objects.Area;
import com.reconinstruments.mapImages.prim.PointD;

public class AreaDrawing extends PolylineDrawing{
	public Area		mArea;
	private Paint	mPaint;
	
	public AreaDrawing(ArrayList<PointD> pathPoints, Area area) {
		super(pathPoints, false);
		mArea = area;
	}
	
	public void Release(){
		super.Release();
		mArea = null;
	}
	
	public void InitPaint(int type){  
		int areaColor;
		int areaAlpha;

		switch(type) {
		case Area.AREA_WOODS:
			areaColor = CommonDrawings.AREA_WOODS_COLOR;//0xff00ff00;
			areaAlpha = 255;
			break;
		case Area.AREA_SCRUB:
			areaColor = CommonDrawings.AREA_SCRUB_COLOR;//0xff0000ff
			areaAlpha = 255;
			break;
		case Area.AREA_TUNDRA:
			areaColor = CommonDrawings.AREA_TUNDRA_COLOR;
			areaAlpha = 255;
			break;
		case Area.AREA_PARK:
			areaColor = CommonDrawings.AREA_PARK_COLOR;
			areaAlpha = 255;
			break;
		case Area.AREA_INVALID:
		default:
			areaColor = CommonDrawings.AREA_TUNDRA_COLOR;
			areaAlpha = 255;
			break;
		}

		mPaint = new Paint( );
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setColor(areaColor);
		mPaint.setAlpha(areaAlpha);
	}
	
	public void Draw(Canvas canvas, RenderSchemeManager rsm, Matrix transformMatrix, double viewScale) {
//		if(mPaint == null)
//			InitPaint(mArea.Type);
				
		super.Draw(canvas, 0.0, rsm.GetAreaPaint(mArea.Type), transformMatrix);
	}
}