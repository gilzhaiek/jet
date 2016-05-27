package com.reconinstruments.navigation.navigation;	

import java.nio.ByteBuffer;
import java.util.ArrayList;

import junit.framework.Assert;
import android.graphics.PointF;
import android.util.Log;

import com.recon.dbf.DbfContent;
import com.recon.dbf.DbfRecord;
import com.recon.dbf.DbfTools;
import com.recon.prim.PointD;
import com.reconinstruments.navigation.routing.ReManager;
import com.recon.shp.ShpContent;
import com.recon.shp.ShpPoint;
import com.recon.shp.ShpPolygon;
import com.recon.shp.ShpPolyline;
import com.recon.shp.ShpRecord;
import com.recon.shp.ShpTools;
import com.recon.shp.ShpType;


public class ShpMapLoader
{

	//filters for filering certain shp elemente from rendering
	private ShpMapFilters mShpFilters = null ;
	
	//the map style the loader will be load
	private int mMapStyle  = ShpMap.MAP_STYLE_NA;
	
	static final String DBF_FIELD_NAME = "NAME";
	static final String DBF_FIELD_ONEWAY = "ONE_WAY";
	static final String DBF_FIELD_SPDLIMIT = "SPD_LIMIT";
	static final String LOG_TAG = "ShapeMap Loader";
	
	//if true, remove the un-necessary patterns such  "(GREEN), (BLUE), (BLACK)"
	//etc from the trail names
	static final boolean CLEAN_TRAIL_NAME = true;
	
			
	/**
	 * Constructor
	 */
	public ShpMapLoader( )
	{
		//tested shpFilters
		mShpFilters = new ShpMapFilters();
		createFilters( mMapStyle );			
	}

	public void setMapStyle( int style )
	{
		mMapStyle = style;
		createFilters( mMapStyle );
	}

	
	/**
	 * Load in a shpMap from two streams
	 * Created trails/areas/point of interests, and added
	 * those to shpMap
	 * @param shpStream: contains the geometry records
	 * @param dbfStream: contains the database records
	 * @param shpMap: the ShpMap that will contains all the renderable elements 
	 */
	public void loadMap( ByteBuffer shpStream, ByteBuffer dbfStream, ShpMap shpMap, ReManager reManager)
	{
	
		//make sure that both shpStream and dbfStream are not null
		Assert.assertNotNull( "Empty stream of shape content is passed in", shpStream );
		Assert.assertNotNull("Empty stream of dbf content is passed in", dbfStream );
				
		//load in shape records
		ShpContent shpContent = ShpTools.readRecords( shpStream );
		Assert.assertNotNull( "Shape content is not loaded correctly", shpContent );
		
		//initialize the shpMap transform parameter 
		Util.initShpContentMap( shpContent );
		
		//check for the mapStyle using one of the ShpRecord
		//and the correspondent ShpFilters will bet set for the dbfRecords
		checkMapStyle( shpContent.shpRecords.get( 0 ) );
		
		//load in database records
		DbfContent dbfContent = DbfTools.readRecords( dbfStream );
		Assert.assertNotNull( "DBF content is not loaded correctly", shpContent );
		
		int idx = 0;
		for( ShpRecord shpRecord : shpContent.shpRecords )
		{
			DbfRecord dbfRecord = dbfContent.dbfRecords[idx];
			++idx;

			//skip not qualified record
			if( mShpFilters.isDbfRecordFiltered( dbfRecord, shpRecord.shapeType ) == true )
			{
				continue;
			}
			
			createMapElements( shpRecord, dbfRecord, shpMap, reManager );
		}
	
		
		//now we are done with all the dbf and shp records, release the 
		//reference to force GC to recycle used memory
		shpContent.shpHeader = null;
		shpContent.shpRecords = null;
		shpContent = null;
		
		dbfContent.dbfHeader = null;
		dbfContent.dbfRecords = null;
		dbfContent = null;
	}
						
	/**
	 * Creates the appropriate type of ShpMap elements given by a ShpRecord and DbfRecord
	 * @param shpRecord: the shape record.
	 * @param dbfRecord: the database record
	 * @return the bounding box of this record
	 * 
	 */
	private void createMapElements( ShpRecord shpRecord, DbfRecord dbfRecord, ShpMap shpMap, ReManager reManager )
	{	
		switch( shpRecord.shapeType ) {
			
			case ShpType.SHAPE_POINT:
			{	
	           	ShpPoint shape = (ShpPoint)shpRecord.shape;	        	
	           	int poiType  = (Integer)mShpFilters.getDbfRecordFilterValue( dbfRecord, ShpType.SHAPE_POINT );	        
									
				if( poiType != PoInterest.POI_TYPE_UNDEFINED )
				{
					Log.d(DebugUtil.LOG_TAG_MAPCONTENT, "Point-of-Interest - Position(" + shape.x + "," + shape.y +"); POI Type:" + poiType );
					PointD mappedPoint = new PointD( shape.x, shape.y );
					Util.mapShpContentPoint( mappedPoint );
					
	        		String poiName = dbfRecord.values.get( DBF_FIELD_NAME );	        	
	        		poiName = poiName.trim();
	        		
	        		//do not try to clean the POI name by removing the (*)* content
	        		//since it contains useful information of the POI
/*	        		
	        		if( ShpMapLoader.CLEAN_TRAIL_NAME )
	        		{
	        			poiName = cleanTrailName( poiName );
	        		}
*/	        		
	        		PoInterest poi = new PoInterest( mappedPoint, poiName, poiType );
	        		shpMap.addPOI(poi);	        	
				}
			}		
			break;
			
			case ShpType.SHAPE_POLYLINE:
			{
	           	ShpPolyline shape = (ShpPolyline)shpRecord.shape;	        	
	           	int trailType  = (Integer)mShpFilters.getDbfRecordFilterValue( dbfRecord, ShpType.SHAPE_POLYLINE );
	           	PointD mappedPoint = new PointD();
	           	
	        	for( ArrayList<ShpPoint> points : shape.rings )
	        	{
	        		ArrayList<PointF> pointDs = new ArrayList<PointF>( points.size() );
	        		for( ShpPoint point : points )
	        		{
	        			mappedPoint.x = point.x;
	        			mappedPoint.y = point.y;
	        			Util.mapShpContentPoint(mappedPoint);
	        			PointF localPoint = new PointF( (float)mappedPoint.x, (float)mappedPoint.y );
	        			
	        			pointDs.add( localPoint );
	        		}
	        		
	        		//get the trail name from DBF records
	        		String trailName = dbfRecord.values.get( DBF_FIELD_NAME );
	        		trailName = trailName.trim();
	        		if( ShpMapLoader.CLEAN_TRAIL_NAME )
	        		{
	        			trailName = cleanTrailName( trailName );
	        		}
	        		
	        		
	        		//get oneWay and speed-limit from DBF records
	        		String oneWayStr = dbfRecord.values.get( DBF_FIELD_ONEWAY );
	        		String spdLimitStr = dbfRecord.values.get( DBF_FIELD_SPDLIMIT );
	        		int oneWay = 0;
	        		
	        		try
	        		{
	        			oneWay = oneWayStr == null ? 0 : new Integer(oneWayStr);
	        		}
	        		catch( Exception e )
	        		{
	        			//oneWayStr can not be parsed as valid int, which is Null or "*"
	        			oneWay = 0;
	        		}
	        		
	        		int spdLimit = 0;
	        		try
	        		{
	        			spdLimit = spdLimitStr == null ? 0 : new Integer(spdLimitStr);
	        		}
	        		catch( Exception e )
	        		{
	        			//the spdLimitStr == "**" which is empty
	        			spdLimit = -1;				//-1 no speed limit
	        		}

	        		
	        		Trail trail = new Trail( pointDs, trailName, trailType, oneWay > 0 );
	        		shpMap.addTrail( trail );
	        		
	        		reManager.addEdge(pointDs, trailType, spdLimit, oneWay > 0, trailName  );
	        		
	        	}
			}
			break;
			
			case ShpType.SHAPE_POLYGON:
			{
	           	ShpPolygon shape = (ShpPolygon)shpRecord.shape;
	           	int areaType  = (Integer)mShpFilters.getDbfRecordFilterValue( dbfRecord, ShpType.SHAPE_POLYGON );
	        	
	        	for( ArrayList<ShpPoint> points : shape.rings )
	        	{
	        		ArrayList<PointD> pointDs = new ArrayList<PointD>( points.size() );
	        		for( ShpPoint point : points )
	        		{
	        			PointD mappedPoint = new PointD( point.x, point.y );
	        			Util.mapShpContentPoint(mappedPoint);
	        			pointDs.add( mappedPoint );
	        		}
	        		
	        		String areaName = dbfRecord.values.get( DBF_FIELD_NAME );
	        		areaName = areaName.trim( );
	        		Area area = new Area( pointDs, areaName, areaType );
	        		shpMap.addArea( area );
	        		
	        	}
	        }	
			break;
		}
	}
		
	private void createFilters(  int mapStyle )
	{			

		mShpFilters.reset();
		
		ShpPrimFilters polylineFilter = new ShpPrimFilters( ShpType.SHAPE_POLYLINE, "GRMN_TYPE" );
		if( mMapStyle == ShpMap.MAP_STYLE_NA )
		{
			polylineFilter.addFilter( "GREEN_TRAIL", Trail.GREEN_TRAIL );
			polylineFilter.addFilter( "GREEN_TRUNK", Trail.GREEN_TRUNK );
			polylineFilter.addFilter( "BLUE_TRAIL", Trail.BLUE_TRAIL );
			polylineFilter.addFilter( "BLUE_TRUNK", Trail.BLUE_TRUNK );
			polylineFilter.addFilter( "BLACK_TRAIL", Trail.BLACK_TRAIL );
			polylineFilter.addFilter( "BLACK_TRUNK", Trail.BLACK_TRUNK);
			polylineFilter.addFilter( "DBLBLCK_TRAIL", Trail.DBLBLACK_TRAIL );
			polylineFilter.addFilter( "DBLBLCK_TRUNK", Trail.DBLBLACK_TRUNK);
			polylineFilter.addFilter( "SKI_LIFT", Trail.SKI_LIFT );
			polylineFilter.addFilter( "CHWY_RESID", Trail.CHWY_RESID_TRAIL );
			polylineFilter.addFilter( "TRAIL", Trail.WALKWAY_TRAIL );
	
		}
		else
		{
			polylineFilter.addFilter( "GREEN_TRAIL", Trail.GREEN_TRAIL );
			polylineFilter.addFilter( "GREEN_TRUNK", Trail.GREEN_TRUNK );
			polylineFilter.addFilter( "BLUE_TRAIL", Trail.BLUE_TRAIL );
			polylineFilter.addFilter( "BLUE_TRUNK", Trail.BLUE_TRUNK );
			polylineFilter.addFilter( "BLACK_TRAIL", Trail.BLACK_TRAIL );
			polylineFilter.addFilter( "BLACK_TRUNK", Trail.BLACK_TRUNK);
			polylineFilter.addFilter( "RED_TRAIL", Trail.RED_TRAIL );
			polylineFilter.addFilter( "RED_TRUNK", Trail.RED_TRUNK);
			polylineFilter.addFilter( "SKI_LIFT", Trail.SKI_LIFT );
			polylineFilter.addFilter( "CHWY_RESID", Trail.CHWY_RESID_TRAIL );
			polylineFilter.addFilter( "TRAIL", Trail.WALKWAY_TRAIL );
		}
		
		ShpPrimFilters  polygonFilter = new ShpPrimFilters( ShpType.SHAPE_POLYGON, "GRMN_TYPE" );
		polygonFilter.addFilter( "WOODS", Area.AREA_WOODS);
		polygonFilter.addFilter( "SCRUB", Area.AREA_SCRUB);
		polygonFilter.addFilter( "TUNDRA", Area.AREA_TUNDRA);
		polygonFilter.addFilter( "URBAN_PARK", Area.AREA_PARK);
		

		ShpPrimFilters pointsFilter = new ShpPrimFilters( ShpType.SHAPE_POINT, "GRMN_TYPE" );
		pointsFilter.addFilter( "SKI_CENTER", PoInterest.POI_TYPE_CHAIRLIFTING );
		pointsFilter.addFilter( "RESTAURANT_AMERICAN", PoInterest.POI_TYPE_RESTAURANT );
		pointsFilter.addFilter( "PARKING", PoInterest.POI_TYPE_CARPARKING );
		pointsFilter.addFilter( "SCHOOL", PoInterest.POI_TYPE_SKISCHOOL );
		pointsFilter.addFilter( "INFORMATION", PoInterest.POI_TYPE_INFORMATION );
		pointsFilter.addFilter( "BAR", PoInterest.POI_TYPE_BAR );
		pointsFilter.addFilter( "RESTROOM", PoInterest.POI_TYPE_RESTROOM );
		pointsFilter.addFilter( "HOTEL", PoInterest.POI_TYPE_HOTEL );
		pointsFilter.addFilter( "BANK", PoInterest.POI_TYPE_BANK );
			
		mShpFilters.addFilter( polylineFilter );
		mShpFilters.addFilter( polygonFilter );
		mShpFilters.addFilter( pointsFilter );

		
	}
	
	/**
	 * this is a utility function for double-checking the mapStyle
	 * at the loading time, because the map style might not
	 * be abled to passed in by external caller, which is the case
	 * for the online situation. Or the mapStyle(NA or Europe) could be
	 * passed in wrong. We double check the map style by one of the location
	 * containing in the passed-in record to see if it is in NA or Europe
	 *  
	 */
	private void checkMapStyle( ShpRecord record )
	{
	  
	    PointD vertex = new PointD( 0, 0 );
	    
	    switch( record.shapeType ) 
	    {                
	        case ShpType.SHAPE_POINT:
	        {
	            vertex.x = ((ShpPoint)record.shape).x;
	            vertex.y = ((ShpPoint)record.shape).y; 
	            break;
	        }
	           
	        case ShpType.SHAPE_POLYLINE:
	        {
	        	ShpPolyline polyLine = (ShpPolyline)record.shape;
	            vertex.x = polyLine.box.topLeft.x;
	            vertex.y = polyLine.box.topLeft.y;
	            break;
	        }
	         
	        case ShpType.SHAPE_POLYGON:
	        {
	        	ShpPolygon polygon = (ShpPolygon)record.shape;
	            vertex.x = polygon.box.topLeft.x;
	            vertex.y = polygon.box.topLeft.y;
	            break;
	        }
	    }
	    
	    //reset the mapStyle if the newly calculated
	    //is different from the default one
	    int style = Util.calcMapStyle( vertex );
	    if( style != mMapStyle )
	    {
	    	mMapStyle = style;
	        createFilters( mMapStyle );
	    }
	}
	
	//utility function for remove the "(*)" appending from the trail name
	private String cleanTrailName( String name )
	{
		int idx = name.indexOf('(');		
		if( idx == 0 )
		{
			return "";
		}
		else if( idx > 0 )
		{
			return name.substring(0, idx - 1);
		}
		else
		{
			return name;
		}
	}
}