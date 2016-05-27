/**
 * 
 */
package com.reconinstruments.osmimages;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.PointXY;
import com.reconinstruments.mapsdk.geodataservice.clientinterface.worldobjects.RectXY;

/**
 * @author simonwang
 *
 */
public class OsmBoundingBox {

	/**
	 * 
	 */
	private static final String TAG = "MapImages";
	public RectXY		mBoundingBox;
	public PointXY	    mCenterPoint;
	protected PointXY	mSize;


	private final static double TILE_HEIGHT_IN_DEGREES = 0.025;	// in degree latitude
	private final static double TILE_WIDTH_IN_DEGREES_AT_EQUATOR = 0.024958105;  // calculated from TILE_WIDTH_IN_METERS/EQUITORIALCIRCUMFRENCE * 360.;	// in degree longitude
	private final static int    NUMBER_TILES_PER_HEMISPHERE = (int) (90.0/TILE_HEIGHT_IN_DEGREES + 1e-10);  
	
	
	
	public OsmBoundingBox(float left, float top, float right, float bottom) {
		mCenterPoint = new PointXY((right+left)/2.f, (top+bottom)/2.f);
		mBoundingBox = new RectXY(left, top, right, bottom);
		mSize = new PointXY(right-left, top-bottom); 
		
	}
	
	
	public static OsmBoundingBox GetBoundingBoxFromTileId(Long tileId) {
		long id = OsmTile.GetTileIdlong(tileId);
		return GetBoundingBoxFromTileId(id);

	}
	
	public static OsmBoundingBox GetBoundingBoxFromTileId(long tileId) {
		int yIndex = (int)(tileId / 10000);
		double bottom = (yIndex - NUMBER_TILES_PER_HEMISPHERE) * TILE_HEIGHT_IN_DEGREES;
		double top = bottom + TILE_HEIGHT_IN_DEGREES;

		int xIndex = (int)(tileId % 10000);
		double dlong = TILE_WIDTH_IN_DEGREES_AT_EQUATOR / Math.cos(Math.toRadians(bottom)) ;
		double left = xIndex*dlong ;
		if(left > 180) left = left - 360.f;
		double right = (xIndex+1)*dlong;
		if(right > 180) right = right - 360.f;
		return new OsmBoundingBox((float)left, (float)top, (float)right, (float)bottom);   
		 
	}
	

	

}
