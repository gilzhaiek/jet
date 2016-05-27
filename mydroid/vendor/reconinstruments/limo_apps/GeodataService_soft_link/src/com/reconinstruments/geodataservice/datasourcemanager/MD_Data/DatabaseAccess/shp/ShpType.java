/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
/**
 *This file is ported from the ActionScript package for loading ESRI shape file
 *which was originally composed by Edwin van Rijkom.
 *Author: Hongzhi Wang at 2011
 * The ShpType class is a place holder for the ESRI Shapefile defined
 * shape types.
 */

package com.reconinstruments.geodataservice.datasourcemanager.MD_Data.DatabaseAccess.shp;

public class ShpType
{	
	/**
	 * Unknow Shape Type (for internal use) 
	 */
	public static final int SHAPE_UNKNOWN = -1;
	
	/**
	 * ESRI Shapefile Null Shape shape type.
	 */	
	public static final int SHAPE_NULL  = 0;
	
	
	/**
	 * ESRI Shapefile Point Shape shape type.
	 */
	public static final int SHAPE_POINT	= 1;
	
	/**
	 * ESRI Shapefile PolyLine Shape shape type.
	 */
	public static final int SHAPE_POLYLINE = 3;
	
	/**
	 * ESRI Shapefile Polygon Shape shape type.
	 */
	public static final int SHAPE_POLYGON = 5;
	
	/**
	 * ESRI Shapefile Multipoint Shape shape type
	 * (currently unsupported).
	 */
	public static final int SHAPE_MULTIPOINT = 8;
	
	/**
	 * ESRI Shapefile PointZ Shape shape type.
	 */
	public static final int SHAPE_POINTZ = 11;
	
	/**
	 * ESRI Shapefile PolylineZ Shape shape type
	 * (currently unsupported).
	 */
	public static final int SHAPE_POLYLINEZ = 13;
	
	/**
	 * ESRI Shapefile PolygonZ Shape shape type
	 * (currently unsupported).
	 */
	public static final int SHAPE_POLYGONZ = 15;
	
	/**
	 * ESRI Shapefile MultipointZ Shape shape type
	 * (currently unsupported).
	 */
	public static final int SHAPE_MULTIPOINTZ = 18;
	
	/**
	 * ESRI Shapefile PointM Shape shape type
	 */
	public static final int  SHAPE_POINTM = 21;
	
	/**
	 * ESRI Shapefile PolyLineM Shape shape type
	 * (currently unsupported).
	 */
	public static final int SHAPE_POLYLINEM	= 23;
	
	/**
	 * ESRI Shapefile PolygonM Shape shape type
	 * (currently unsupported).
	 */
	public static final int SHAPE_POLYGONM	= 25;
	
	/**
	 * ESRI Shapefile MultiPointM Shape shape type
	 * (currently unsupported).
	 */
	public static final int SHAPE_MULTIPOINTM = 28;
	
	/**
	 * ESRI Shapefile MultiPatch Shape shape type
	 * (currently unsupported).
	 */
	public static final int SHAPE_MULTIPATCH = 31;
	
}