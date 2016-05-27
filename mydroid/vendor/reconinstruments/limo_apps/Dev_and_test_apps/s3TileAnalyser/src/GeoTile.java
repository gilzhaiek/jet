

import java.util.ArrayList;
import java.util.TreeMap;

public class GeoTile {
// constants
	private final static String TAG = "GeoTile";
	private final static float  EQUITORIALCIRCUMFRENCE = 40075017.0f;	// taken from wikipedia/Earth
	private final static float  MERIDIONALCIRCUMFRENCE = 40007860.0f;	// taken from wikipedia/Earth
	private final static double TILE_HEIGHT_IN_DEGREES = 0.025;	// in degree latitude
	private final static double TILE_HEIGHT_IN_METERS = 2778.3;  // calculated from TILE_HEIGHT_IN_DEGREES/360. * MERIDIONALCIRCUMFRENCE - defined as a const to avoid computational differences with Tile construction code 
	private final static double TILE_WIDTH_IN_METERS = 2778.3;	// in meters as tiles are square in distance
	private final static double TILE_WIDTH_IN_DEGREES_AT_EQUATOR = 0.024958105;  // calculated from TILE_WIDTH_IN_METERS/EQUITORIALCIRCUMFRENCE * 360.;	// in degree longitude
	private final static int    NUMBER_TILES_PER_HEMISPHERE = (int) (90.0/TILE_HEIGHT_IN_DEGREES + 1e-10);  

// members
	public int						mTileIndex = -1;
	public double					mGPSQuantizationFactor = TILE_HEIGHT_IN_DEGREES;  // used for ref
	public boolean					mNoDataPlaceholder = false;
	
// constructors
	public GeoTile(int tileIndex) {
		mTileIndex = tileIndex;
	}


	
// methods
    
	public static GeoRegion GetGeoRegionFromTileIndex(long tileIndex) {
		int yIndex = (int)(tileIndex / 100000);
		double bottom = (yIndex - NUMBER_TILES_PER_HEMISPHERE) * TILE_HEIGHT_IN_DEGREES;
		double top = bottom + TILE_HEIGHT_IN_DEGREES;

		int xIndex = (int)(tileIndex % 100000);
//		Log.e(TAG, "xy = " + xIndex + ", " + yIndex);
		double dlong = TILE_WIDTH_IN_DEGREES_AT_EQUATOR / Math.cos(Math.toRadians(bottom)) ;
		double left = xIndex*dlong ;
		if(left > 180) left = left - 360.f;
		double right = (xIndex+1)*dlong;
		if(right > 180) right = right - 360.f;
//		Log.e(TAG, "xy = " + left + ", " + right + " | = " + top + ", " + bottom);
		return new GeoRegion().MakeUsingBoundingBox((float)left, (float)top, (float)right, (float)bottom);   // NOTE: rect BB makes it easier to find world objects but does represent a slightly trapazoid area 
	}
	
	public static int FloorWithJavaFix(double value) {	// *sometimes* JAVA does not compute division results properly and changes values like 5.0 to 4.999999999 (annoying)
		return (int) Math.floor(value + 1e-10);				// this dramatically effects boundary calculation, so this function was written to address that
	}


	public static ArrayList<Integer> GetTileListForGeoRegion(GeoRegion geoRegion, ArrayList<Integer> ignoreTheseTiles) {
		ArrayList<Integer> tileList = new ArrayList<Integer>();
		double startLat = (double) geoRegion.mBoundingBox.top;
		double startLong = (double) geoRegion.mBoundingBox.left;
		double endLat = startLat;
		double endLong = (double) geoRegion.mBoundingBox.right;
		double bottomOfTileLatitude=100.;

		TreeMap<Integer, Integer> doNotInclude = new TreeMap<Integer, Integer>();
		if(ignoreTheseTiles != null) {
			for(Integer tileIndex : ignoreTheseTiles) {
				doNotInclude.put(tileIndex,  tileIndex);	// for faster search.. better structure??
			}
		}
		
		TreeMap<Float, Integer> tileDistance = new TreeMap<Float, Integer>();	// for sorting
		
		Integer centerTileIndex = 	GetTileIndex(geoRegion.mCenterPoint.x, geoRegion.mCenterPoint.y);
		GeoRegion centerTileGeoRegion = GetGeoRegionFromTileIndex(centerTileIndex);
//		Log.d(TAG, "tile index of center " + centerTileIndex) ;
		
		double dlongAtCenter = TILE_WIDTH_IN_DEGREES_AT_EQUATOR / Math.cos(Math.toRadians(geoRegion.mCenterPoint.y)) ;
		
		do {
			Integer startPointTileIndex = GetTileIndex(startLong, startLat);
			GeoRegion startTileGeoRegion = GetGeoRegionFromTileIndex(startPointTileIndex);
			Integer endPointTileIndex = GetTileIndex(endLong, endLat);
//			GeoRegion endTileGeoRegion = GetGeoRegionFromTileIndex(startPointTileIndex);
			bottomOfTileLatitude = (FloorWithJavaFix(startLat/TILE_HEIGHT_IN_DEGREES) * TILE_HEIGHT_IN_DEGREES);
			double curLong = startTileGeoRegion.mBoundingBox.left;
			double dlong = TILE_WIDTH_IN_DEGREES_AT_EQUATOR / Math.cos(Math.toRadians(startTileGeoRegion.mBoundingBox.bottom)) ;
			if(endPointTileIndex >= startPointTileIndex) {  // normal case
//				Log.e(TAG, "startTileGeoRegion: " + startPointTileIndex +"endPointTileIndex: " + endPointTileIndex) ;
				for(Integer index=startPointTileIndex; index<=endPointTileIndex; index++) {
//					Log.d(TAG, "index: " + index ) ;
//					Log.d(TAG, "tile index  " + index) ;
					if(doNotInclude.get(index) == null) {
//						tileDistance.put(DistanceBetweenPoints(curLong, startTileGeoRegion.mBoundingBox.bottom, centerTileGeoRegion.mBoundingBox.left, centerTileGeoRegion.mBoundingBox.bottom, dlongAtCenter), index);
						tileList.add(index);
					}
					curLong += dlong;
				}
			}
			else { // special case of crossing 0 boundary
				Integer maxTileIndex = CombineTileSubIndices(FloorWithJavaFix(startLat/TILE_HEIGHT_IN_DEGREES) + NUMBER_TILES_PER_HEMISPHERE, FloorWithJavaFix((EQUITORIALCIRCUMFRENCE * Math.cos(Math.toRadians((double)startLat))) / TILE_WIDTH_IN_METERS) );
//				Log.d(TAG, "0 boundary crossing  " + startPointTileIndex + " end:" + endPointTileIndex+ " max:" + maxTileIndex) ;
				for(Integer index=startPointTileIndex; index<=maxTileIndex; index++) {
					if(doNotInclude.get(index) == null) {
//						tileDistance.put(DistanceBetweenPoints(curLong, startTileGeoRegion.mBoundingBox.bottom, centerTileGeoRegion.mBoundingBox.left, centerTileGeoRegion.mBoundingBox.bottom, dlongAtCenter), index);
						tileList.add(index);
					}
					curLong += dlong;
				}
				curLong = 0;
				Integer sIndex =((int)(endPointTileIndex/100000)) * 100000;
				for(Integer index=sIndex; index<=endPointTileIndex; index++) {
//					Log.d(TAG, "far side of crossing  " + index) ;
					if(doNotInclude.get(index) == null) {
//						tileDistance.put(DistanceBetweenPoints(curLong, startTileGeoRegion.mBoundingBox.bottom, centerTileGeoRegion.mBoundingBox.left, centerTileGeoRegion.mBoundingBox.bottom, dlongAtCenter), index);
						tileList.add(index);
					}
					curLong += dlong;
				}
			}
			startLat -= TILE_HEIGHT_IN_DEGREES;	// move south by tile height to next tile layer
			endLat -= TILE_HEIGHT_IN_DEGREES;
		}
		while(bottomOfTileLatitude > geoRegion.mBoundingBox.bottom);

//		for (TreeMap.Entry<Float, Integer> entry : tileDistance.entrySet())  {
////			Log.d(TAG, "tile distance " + entry.getKey() + " for tile #" + entry.getValue()) ;
//			tileList.add(entry.getValue());
//		}		
		return tileList;
	}

	public static Float DistanceBetweenPoints(double longObs, double latObs, double longRef, double latRef, double dlongAtRefLat) {	// rough distance measured in tile height/width
		double latdiff = (latObs - latRef)/TILE_HEIGHT_IN_DEGREES;	// in tile heights
		double longdiff = (longObs - longRef)/dlongAtRefLat;		// in tile widths ... rough approximation... good enough for sorting purposes
		double dist = Math.sqrt(longdiff*longdiff + latdiff*latdiff);
//		Log.d(TAG, "tile distance calc " + longObs + " - " + longRef+ " - " + longdiff+ " | " + latObs+ " - " + latRef+ " - " + latdiff + " = dist " + dist) ;
//		Log.d(TAG, "tile distance calc " + longdiff+ ", " + latdiff + " = dist " + dist) ;
		return (float)  dist;

	}

	public static Integer GetTileIndex(double longitude, double latitude) { // x=long, y=lat
		int longIndex;
		int latIndex;
		double dlat = TILE_HEIGHT_IN_DEGREES;

//		if(latitude == 90.0)  latitude -= 0.00001;	// 
		double latRatio = latitude/dlat;		// can have JAVA rounding issue with Flooring
//		Log.d(TAG, "latitude:" + latitude + ", " + "dlat: " + dlat + " -> LatRatio: " + latRatio);
		int baseLatIndex = FloorWithJavaFix(latRatio);
		double dlong = TILE_WIDTH_IN_DEGREES_AT_EQUATOR / Math.cos(Math.toRadians(baseLatIndex*dlat)) ;
		latIndex = baseLatIndex + NUMBER_TILES_PER_HEMISPHERE;		// make all indices positive
		
//		Log.d(TAG, "tile width at baseLat:" + baseLatIndex + ", " + "baseLat: " + (baseLatIndex*dlat) + ", " + dlong);
		double positiveLongitude = (longitude+360.f) % 360.f;
		longIndex = FloorWithJavaFix(positiveLongitude/dlong);

//		Log.d(TAG, "tile Lat Ind:" + latIndex + ", " + "Long Ind: " + longIndex);
		Integer result = CombineTileSubIndices(latIndex, longIndex );
//		Log.d(TAG, "tile Lat Ind:" + latIndex + ", " + "Long Ind: " + longIndex + " | " + result);
		return result;
	}
//	public static Integer GetTileIndexF(PointXY gpsCoordinate) { // x=long, y=lat
//		int longIndex;
//		int latIndex;
////		double dlat = TILE_HEIGHT_IN_DEGREES;
//		double dlat = 0.05;
//
////		if(gpsCoordinate.y == 90.0f)  gpsCoordinate.y -= 0.00001;	// 
//		double latRatio = gpsCoordinate.y/dlat;
//		Log.d(TAG, "latitude:" + gpsCoordinate.y + ", " + "dlat: " + dlat + " -> LatRatio: " + latRatio);
//		int baseLatIndex = (int) Math.floor((double)((gpsCoordinate.y/dlat)));
//		double dlong = (double) (dlat / Math.cos(Math.toRadians((double)(baseLatIndex*dlat)) )) ;
//		latIndex = baseLatIndex + NUMBER_TILES_PER_HEMISPHERE;
//		
//		Log.d(TAG, "tile width at baseLat:" + baseLatIndex + ", " + "baseLat: " + (baseLatIndex*dlat) + ", " + dlong);
//		double positiveLongitude = (gpsCoordinate.x+360.f) % 360.f;
//		longIndex = (int)(positiveLongitude/dlong);
//
//		return CombineTileSubIndices(latIndex, longIndex );
//	}
	
	public static Integer CombineTileSubIndices(int latIndex, int longIndex) {
		return latIndex * 100000 + longIndex;
	}

//	public float TransformLatitude(float longitude, float latitude) {
//		return (float)(-((double)latitude-mOriginY)/360.0 * MERIDIONALCIRCUMFRENCE / DISTANCE_PER_PIXEL) ;  // == pixel coordinate  (-ve because Android drawing/graphics Y is opposite real-world (GPS) Y direction);
//	}
//
//	public float TransformLongitude(float longitude, float latitude) { 
//		return (float)((((double)longitude-mOriginX)/360.0) * EQUITORIALCIRCUMFRENCE*Math.cos(Math.toRadians((double)latitude)) / DISTANCE_PER_PIXEL); // == pixel coordinate
//	}
}
