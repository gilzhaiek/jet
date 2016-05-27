package com.reconinstruments.mapsdk.mapview.renderinglayers.customlayer;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.GeoRegion;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.mapview.renderinglayers.World2DrawingTransformer;

public class CustomPOIDrawing extends CustomWorldObjectDrawing {
	private static final long serialVersionUID = 1L;
	private final static String TAG = "POIDrawing";

	public PointXY  mLocation = new PointXY(0.f, 0.f);
	public PointXY  mGPSLocation = new PointXY(0.f, 0.f);
	public int	mAlpha = 255;
	public Bitmap mBitMap = null;
	Bitmap mDrawingImage = null;
	float mDrawingScale = 0.f;
	
	public CustomPOIDrawing(String _poiID, PointXY _gpsLocation, Bitmap _bitmap, int _alpha, World2DrawingTransformer _world2DrawingTransformer) {
		super(_poiID, CustomObjectTypes.POI);
		
		mGPSLocation = new PointXY(_gpsLocation.x, _gpsLocation.y);
		mLocation = _world2DrawingTransformer.TransformGPSPointToDrawingPoint(mGPSLocation);
		mAlpha = _alpha;
		mBitMap = Bitmap.createBitmap(_bitmap);
				
		mDrawingImage = Bitmap.createBitmap(mBitMap);
	}
	public void HandleDrawingTransformerChange(World2DrawingTransformer _world2DrawingTransformer) {
		mLocation = _world2DrawingTransformer.TransformGPSPointToDrawingPoint(mGPSLocation);
	}

	public void Release(){
	}
	
	public void Draw(Canvas canvas, Matrix transformMatrix, float viewScale) {

		if(mEnabled) {
			float[] dl = new float[2];
			dl[0] = mLocation.x;
			dl[1] = mLocation.y;
			transformMatrix.mapPoints(dl);

			mDrawingImage = Bitmap.createScaledBitmap(mBitMap, (int)(mBitMap.getWidth()), (int)(mBitMap.getHeight()), false);
			if(mDrawingImage != null) {
				Paint paint = new Paint();
				paint.setAlpha(mAlpha);
				canvas.drawBitmap(mDrawingImage, dl[0] - mDrawingImage.getWidth()/2+1, dl[1] - mDrawingImage.getHeight()/2+1, paint);
			}
		}
	}
	
	public boolean InGeoRegion(GeoRegion geoRegion) {
		return geoRegion.mBoundingBox.Contains(mGPSLocation.x, mGPSLocation.y);
	}

}
