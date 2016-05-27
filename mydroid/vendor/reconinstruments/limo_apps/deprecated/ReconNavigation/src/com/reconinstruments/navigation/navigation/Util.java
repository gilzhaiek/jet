/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.location.Location;
import android.util.Log;

import com.recon.prim.PointD;
import com.recon.prim.RectangleD;
import com.recon.shp.ShpContent;
import com.reconinstruments.reconsettings.ReconSettingsUtil;

public class Util
{
	static final double EARTH_RADIUS = 6371;
	
	static public boolean SHPCONTENT_INIT = false;
	static double SHPCONTENT_CENTERX = 0;
	static double SHPCONTENT_CENTERY = 0;
	static double SHPCONTENT_SCALE_FACTOR = 1;
	static final double DISTANCE_PER_PIXEL = 0.8;		//the distance in meter that a pixel will be mapped to initially
	static final boolean FLIP_Y = true;
	static final double GPS_LOCATION_THRESHOLD = 2;		//the threshold for setting the gps location
	static public final int HILITE_LABLE_BG_COLOR = 0x99000000;
	static public final int NAME_LABEL_BG_COLOR = 0xffbebebe;//909090;
	static Typeface MENU_TYPE_FONT = null;
	static final float METER_TO_FEET_FACTOR = 3.2808399f;
	static public final int BLACK_COLOR = 0xff000000; 
	static public final int WHITE_COLOR = 0xffffffff;
	static public final int YELLOW_COLOR = 0xffffff00;
	static public final int BLUE_MENU_COLOR = 0xff1480e8; 
	
	static public int mUnits = ReconSettingsUtil.RECON_UINTS_METRIC;
	
	//return the distance( in Km ) between two (Longitude, Latitude) points
	//calculation is based on haversine algorithms.
	//please refer to the link: http://www.movable-type.co.uk/scripts/latlong.html
	static double distanceFromLngLats( double lng1, double lat1, double lng2, double lat2 )
	{
		double dLat = Math.toRadians(lat1 - lat2);
		double dLon = Math.toRadians(lng1- lng2);
		double a = Math.sin(dLat/2)*Math.sin(dLat/2)+
        		   Math.cos( Math.toRadians(lat1) )* Math.cos(Math.toRadians(lat2)) * 
        		   Math.sin(dLon/2) * Math.sin(dLon/2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		double d = EARTH_RADIUS * c;
		
		return d;
		
	}
	
	static double distanceFrom2PointD( PointD lngLat1, PointD lngLat2 )
	{
		return distanceFromLngLats( lngLat1.x, lngLat1.y, lngLat2.x, lngLat2.y );
	}
	

	//given a shpContent, calculate the mapping matrix for transforming
	//the shpContent to better manageable space
	//First, translate to the center of the content
	//second, scale by a factor, which is boundbox.width_in_kilometer*1000/boundbox.width_in_longitude
	static Matrix calShpContentMapping( ShpContent shpContent )
	{
		RectangleD bbox = shpContent.shpHeader.boundsXY;
		double width = bbox.bottomRight.x - bbox.topLeft.x;
		double centerX = (bbox.bottomRight.x + bbox.topLeft.x)/2.f;
		double centerY = (bbox.bottomRight.y + bbox.topLeft.y)/2.f;
		
		double width_in_km = distanceFromLngLats( bbox.topLeft.x, bbox.topLeft.y, bbox.bottomRight.x, bbox.topLeft.y );
		double scale = width_in_km*1000/(width*DISTANCE_PER_PIXEL);
		
		Matrix matrix = new Matrix( );
		matrix.reset();
		//matrix.preTranslate((float)-centerX, (float)-centerY);
		//matrix.preScale((float)scale, (float)scale);
		matrix.setScale((float)scale, (float)scale, (float)centerX, (float)centerY);
		return matrix;
	}
	
	static boolean IsShpContentInit () 
	{
		return SHPCONTENT_INIT;
	}
	
	static void initShpContentMap( ShpContent shpContent )
	{
		if( SHPCONTENT_INIT == false )
		{
			RectangleD bbox = shpContent.shpHeader.boundsXY;
			double width = bbox.bottomRight.x - bbox.topLeft.x;
			SHPCONTENT_CENTERX = (bbox.bottomRight.x + bbox.topLeft.x)/2.f;
			SHPCONTENT_CENTERY = (bbox.bottomRight.y + bbox.topLeft.y)/2.f;
			
			double width_in_km = distanceFromLngLats( bbox.topLeft.x, bbox.topLeft.y, bbox.bottomRight.x, bbox.topLeft.y );
			SHPCONTENT_SCALE_FACTOR  = width_in_km*1000/(width*DISTANCE_PER_PIXEL);
			
			SHPCONTENT_INIT= true;
		}
	}
	
	//convert from lnglat to local rendering space point
	static void mapShpContentPoint( PointD point )
	{
		if( SHPCONTENT_INIT == false )
		{
			Log.e("UTIL", "(1) ShpMap is not initialize!" );
		}
		else
		{
			point.x = (point.x - SHPCONTENT_CENTERX)*SHPCONTENT_SCALE_FACTOR;
			point.y = (point.y - SHPCONTENT_CENTERY)*SHPCONTENT_SCALE_FACTOR;
			point.y *= FLIP_Y ? -1 : 1;
		}
	}
	
	//covert a point that is in Map local space 
	//to Latitude and Longitude
	//x: longitude; y: latitude
	static public void mapLocalToLatLng( PointF local )
	{
		local.y *= FLIP_Y?-1 : 1;
		local.x = (float)(local.x/SHPCONTENT_SCALE_FACTOR + SHPCONTENT_CENTERX);
		local.y = (float)(local.y/SHPCONTENT_SCALE_FACTOR + SHPCONTENT_CENTERY);
	}
	
	static PointF mapLatLngToLocal( double lat, double lng )
	{
		PointF point = new PointF(0.f, 0.f);
		if( SHPCONTENT_INIT == false )
		{
			//Log.e("UTIL", "(2) ShpMap is not initialize!" );
			
		}
		else
		{
			point.x = (float)((lng - SHPCONTENT_CENTERX)*SHPCONTENT_SCALE_FACTOR);
			point.y = (float)((lat - SHPCONTENT_CENTERY)*SHPCONTENT_SCALE_FACTOR);
			point.y *= FLIP_Y ? -1 : 1;
		}
		return point;
		
	}
	
	//covert a point that is in global space: Lat, Lng 
	//to mapLocal space point
	//x: longitude; y: latitude
	static void mapLatLngToLocal( PointF latLng )
	{		
		if( SHPCONTENT_INIT == false )
		{
			Log.e("UTIL", "(3) ShpMap is not initialize!" );
			
		}
		else
		{
			float x = (float)((latLng.x - SHPCONTENT_CENTERX)*SHPCONTENT_SCALE_FACTOR);
			float y = (float)((latLng.y - SHPCONTENT_CENTERY)*SHPCONTENT_SCALE_FACTOR);
			y *= FLIP_Y ? -1 : 1;
			
			latLng.x = x;
			latLng.y = y;
		}				
	}

	//utility function for calculating the map style based on its bounding box
	static public int calcMapStyle( PointD position )
	{
		
		if( position.x > -10 )
			return ShpMap.MAP_STYLE_EUROPE;
		else
			return ShpMap.MAP_STYLE_NA;
		
	}
	
	static Path makeArrowPath( float arrowWidth ) 
    {

        Path p = new Path();      
        /*p.moveTo(arrowWidth/2, 0);
        p.lineTo(0, -arrowWidth/2);
        p.lineTo(arrowWidth, -arrowWidth/2);
        p.lineTo(arrowWidth*3/2, 0);
        p.lineTo(arrowWidth, arrowWidth/2);
        p.lineTo(0, arrowWidth/2);*/

        p.moveTo(0, arrowWidth/2);
        p.lineTo(0, -arrowWidth/2);
        p.lineTo(arrowWidth*2, 0);

        return p;
    }
	
	static String latLngToDegree( double latLng )
	{
		String loc = Location.convert(latLng, Location.FORMAT_MINUTES );
		
		loc = loc.replace(':', ((char) 0x00b0));
		// The following line is flase and hence commented out
		//loc = loc.replace('.', '\'');
		loc += "\'";
		
		return loc;
		
	}
	
	static public Typeface getMenuFont( Context context )
	{
		if( MENU_TYPE_FONT == null )
		{			
			MENU_TYPE_FONT = Typeface.createFromAsset(context.getAssets(), "fonts/Eurostib.ttf" );
		}
		
		return MENU_TYPE_FONT;
	}
	
	static public float meterToFeet( float meters )
	{
		return meters*METER_TO_FEET_FACTOR;
	}
	
}
