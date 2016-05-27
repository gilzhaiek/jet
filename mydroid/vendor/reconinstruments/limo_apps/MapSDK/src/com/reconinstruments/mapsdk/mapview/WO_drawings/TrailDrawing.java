package com.reconinstruments.mapsdk.mapview.WO_drawings;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.WO_Polyline;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;
import com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.CollisionDetector;

public class TrailDrawing extends PolylineDrawing 
{
	private static final long serialVersionUID = 1L;
	private final static String TAG = "TrailDrawing";

	public TrailDrawing(WorldObjectDrawingTypes type, WO_Polyline dataObject, World2DrawingTransformer world2DrawingTransformer) {
		super(type, dataObject, true, world2DrawingTransformer);
		RenderSchemeManager.ObjectTypes renderType = RenderSchemeManager.ObjectTypes.NAMED_PATHS;
		int	renderTypeVariantIndex = 0;
		switch(type) {
			case WATERWAY: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.WATERWAY.ordinal();
				break;
			}
			case BORDER_NATIONAL: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.BORDER_NATIONAL.ordinal();
				break;
			}
			case DOWNHILLSKITRAIL_GREEN: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.SKIRUN_GREEN.ordinal();
				break;
			}
			case DOWNHILLSKITRAIL_BLUE: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.SKIRUN_BLUE.ordinal();
				break;
			}
			case DOWNHILLSKITRAIL_BLACK: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.SKIRUN_BLACK.ordinal();
				break;
			}
			case DOWNHILLSKITRAIL_DBLACK: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.SKIRUN_DOUBLEBLACK.ordinal();
				break;
			}
			case DOWNHILLSKITRAIL_RED: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.SKIRUN_RED.ordinal();
				break;
			}
			case CHAIRLIFT: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.CHAIRLIFT.ordinal();
				break;
			}
			case SKIRESORTSERVICE_WALKWAY: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.WALKWAY.ordinal();
				break;
			}
			case ROAD_FREEWAY: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.ROAD_FREEWAY.ordinal();
				break;
			}
			case ROAD_ARTERY_PRIMARY: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.ROAD_ARTERY_PRIMARY.ordinal();
				break;
			}
			case ROAD_ARTERY_SECONDARY: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.ROAD_ARTERY_SECONDARY.ordinal();
				break;
			}
			case ROAD_ARTERY_TERTIARY: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.ROAD_ARTERY_TERTIARY.ordinal();
				break;
			}
			case ROAD_RESIDENTIAL: {
				renderTypeVariantIndex = RenderSchemeManager.NamedPathTypes.ROAD_RESIDENTIAL.ordinal();
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

	public void Draw(Canvas canvas, double mapHeading, RenderSchemeManager rsm, Matrix transformMatrix, double viewScale) {
		super.Draw(canvas, mapHeading, (rsm.GetTrailPaint(mRenderTypeVariantIndex, mState.ordinal(), viewScale))[RenderSchemeManager.TrailPaint.LINE.ordinal()], transformMatrix);
	}
	
	public void DrawNames(Canvas canvas, RenderSchemeManager rsm, Matrix transformMatrix, double viewScale, CollisionDetector collisionDetector) {
		Paint[] paints = rsm.GetTrailPaint(mRenderTypeVariantIndex, mState.ordinal(), viewScale);
		if(!mDataObject.mName.equalsIgnoreCase("not defined")) {
			super.DrawNames(canvas, paints[RenderSchemeManager.TrailPaint.TEXTSIZE.ordinal()], paints[RenderSchemeManager.TrailPaint.TEXTOUTLINE.ordinal()], rsm.GetTrailLabelOffsetFactor(), transformMatrix, mDataObject.mName, collisionDetector);
		}
	}
}