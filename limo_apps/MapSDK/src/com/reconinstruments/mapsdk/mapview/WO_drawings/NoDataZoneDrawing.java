package com.reconinstruments.mapsdk.mapview.WO_drawings;

import android.graphics.Canvas;
import android.graphics.Matrix;

import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.NoDataZone;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WO_Polyline;

public class NoDataZoneDrawing extends PolylineDrawing{
	private static final long serialVersionUID = 1L;
	private final static String TAG = "NoDataZoneDrawing";

	public NoDataZoneDrawing(NoDataZone dataObject, World2DrawingTransformer world2DrawingTransformer) {
		super(WorldObjectDrawingTypes.NO_DATA_ZONE, (WO_Polyline)dataObject, false, world2DrawingTransformer);
		RenderSchemeManager.ObjectTypes renderType = RenderSchemeManager.ObjectTypes.AREAS;
		super.setRendering(renderType, RenderSchemeManager.AreaTypes.NO_DATA_ZONE.ordinal());  
	}
	
	public void Release(){
		super.Release();
	}
	
//	public RectF GetBoundingBox(){			
//		return super.GetBoundingBox();		
//	}
	
	public void Draw(Canvas canvas, RenderSchemeManager rsm, Matrix transformMatrix, double viewScale) {
		super.Draw(canvas, 0.0, rsm.GetAreaPaint(mRenderTypeVariantIndex), transformMatrix);
	}
}