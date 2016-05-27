/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.recon.prim.PointD;

public class Area 
{	
	//enum for different type of areas
	static final int AREA_INVALID = -1;
	static final int AREA_WOODS = 0;
	static final int AREA_SCRUB = 1;
	static final int AREA_TUNDRA = 2;
	static final int AREA_PARK = 3;
	static final int NUM_AREA_TYPES = 4;

	//paint style related constants
	static final int[] AREA_COLORS = new int[] {
		0xff336600,						//color for woods area
		0xff996600,						//color for scrub area
		0xff00ffff,						//color for tundra area
		0xffff9905						//color for urban park
	};
	
	static final int[] AREA_ALPHAS = new int[]{
		128,			//woods area
		128,			//scrub area
		128,			//tundra area
		128				//urban area
	};
			
	//hold different painting style for different type of areas
	private static Paint[] sAreaPaints = null;
	
	//the name of the area, get from the dbf file
	public String mName = null;
	
	//the bounding box of the area. Already transformed the map-content-space
	public RectF mBBox = null;
	
	//the Android path submitted for rendering
	private Path mPath = null;
	
	//the paint style for rendering the area
	private Paint mPaint = null;
	
	//one of the pre-defined area type
	private int mType;

	static public void InitPaints( Context context )
	{
		//double check to make sure it is  initialized ONCE
		if( sAreaPaints != null )
		{
			return;
		}
		
		sAreaPaints = new Paint[ Area.NUM_AREA_TYPES ];
		
		for( int idx = 0; idx < Area.NUM_AREA_TYPES; ++idx )
		{
			Paint paint = new Paint( );
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(AREA_COLORS[idx]);
			paint.setAlpha(AREA_ALPHAS[idx]);
			sAreaPaints[idx] = paint;
		}
		
	}
	
	public Area( ArrayList<PointD> line, String name, int areaType )
	{	
		mName = name;
		mPath = new Path( );
		mType = areaType;
		
		//hint the mPath the number of points to be added for more efficiently storage allocation
		mPath.incReserve( line.size() + 1 );
		
		int idx = 0;
		for( PointD p : line)
		{
			if( idx == 0 )
			{				
				mPath.moveTo( (float)p.x, (float)p.y );
			}
			else
			{
				mPath.lineTo((float)p.x, (float)p.y);
			}
			++idx;
			
		}
		mPath.close();
				
		mPaint = sAreaPaints[mType];
			
		mBBox = new RectF( );
		mPath.computeBounds(mBBox, true);	
	}
	
	
	public int getType( )
	{
		return mType;
	}
	
	public void setRenderColor( int color )
	{
		mPaint.setColor(color);
	}
		
	
	public void draw( Canvas canvas, Matrix transform )
	{
		canvas.drawPath(mPath, mPaint);
	}
}
