package com.reconinstruments.mapsdk.mapview.WO_drawings;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;

import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.Terrain;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WO_Polyline;

public class TerrainDrawing extends PolylineDrawing{
	private static final long serialVersionUID = 1L;
	private final static String TAG = "TerrainDrawing";

	public TerrainDrawing(WorldObjectDrawing.WorldObjectDrawingTypes type, Terrain dataObject, World2DrawingTransformer world2DrawingTransformer) {
		super(type, (WO_Polyline)dataObject, false, world2DrawingTransformer);
		RenderSchemeManager.ObjectTypes renderType = RenderSchemeManager.ObjectTypes.AREAS;
		int	renderTypeVariantIndex = 0;

		switch(type) {
			case TERRAIN_LAND: {
				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.AREA_LAND.ordinal();
				break;
			}
			case TERRAIN_OCEAN: {
				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.AREA_OCEAN.ordinal();
				break;
			}
//			case TERRAIN_CITYTOWN: {
//				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.AREA_CITYTOWN.ordinal();
//				break;
//			}
			case TERRAIN_WOODS: {
				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.AREA_WOODS.ordinal();
				break;
			}
			case TERRAIN_PARK: {
				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.AREA_PARK.ordinal();
				break;
			}
			case TERRAIN_WATER: {
				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.AREA_WATER.ordinal();
				break;
			}
			case TERRAIN_SHRUB: {
				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.AREA_SCRUB.ordinal();
				break;
			}
			case TERRAIN_TUNDRA: {
				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.AREA_TUNDRA.ordinal();
				break;
			}
			case TERRAIN_SKIRESORT: {
				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.AREA_SKIRESORT.ordinal();
				break;
			}
			case NO_DATA_ZONE: {
				renderTypeVariantIndex = RenderSchemeManager.AreaTypes.NO_DATA_ZONE.ordinal();
				break;
			}
		}
		super.setRendering(renderType, renderTypeVariantIndex);
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