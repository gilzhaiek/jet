/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

/**
 *This is a utility class for defining a 2D point with coordinator of double,
 *which is not defined by java.
 *Using this class to avoid of introducing the dependence on android.geom.PointF
 *so that the com.recon.shp and com.recon.dbf can be a pure java package independent
 *of andriod
 *Author: Hongzhi Wang at 2011 
 */

package com.reconinstruments.dashlauncher.radar.prim;

public class PointD
{
	//the coordinator of the 2D point
	public double x, y;
	
	public PointD( )
	{
		x = 0;
		y = 0;
	}
	
	public PointD( double x, double y )
	{
		this.x = x;
		this.y = y;
	}
	
	public PointD( PointD p )
	{
		this.x = p.x;
		this.y = p.y;
	}
	
	public double length( )
	{
		return Math.sqrt(x*x + y*y);
	}
	
	public void normalize( )
	{
		double len = this.length( );
		x /= len;
		y /= len;
	}
}

