/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation;

import java.util.ArrayList;

import com.reconinstruments.reconsettings.ReconSettingsUtil;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PathDashPathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

public class ShpMap
{
	static final int MAP_STYLE_EUROPE = 1;				//europe ski resort map
	static final int MAP_STYLE_NA = 0;					//north american ski resort map
	static final int POINTER_DIRECTION_VECTOR_WIDTH = 20; 

	static final int VIRTUAL_BORDER_WIDTH = 30;
	static final int VIRTUAL_BORDER_WIDTH_NO_DIR = 20;
	static final boolean DRAW_DIRECTION = true;
	
	private static RectF TEST_BOX = new RectF();
	
	public ArrayList<ArrayList<Trail>> mTrails;
	public ArrayList<ArrayList<Area>> mAreas;
	public ArrayList<ArrayList<PoInterest>> mPoInterests;
	//public 
	public RectF mBBox = null;
	public boolean mDrawTrailNames = true;				//do not draw trails names if false
	public boolean mDrawPOIs = true;
	public boolean mDrawPOIsNames = true;
	public boolean mDrawTrackedPOIs = true;
	public boolean mDrawTrackedNames = true;
	public boolean mForceDrawPOIs = false;
	private float mScaleFactor = 1.f;
	public String mResortName="Unknown";				//the Resort name of current map
	private Paint[] mLinePaints = null;
	private PathDashPathEffect mArrowEffect  = null;
	private ArrayList<PoInterest> mHilitedPois = null;
	private ArrayList<float[]> mFarDrawnPoints = null;
	public float mDistancePerPixel = 0.5f;
	public float mScale = 1.0f;
	private Paint mPaint = null;
	
	public ShpMap( )
	{	
		mLinePaints = new Paint[PoInterest.NUM_POI_TYPE];
		mTrails = new ArrayList<ArrayList<Trail>>(Trail.NUM_TRAIL_TYPES);
		mAreas = new ArrayList<ArrayList<Area>>(Area.NUM_AREA_TYPES);
		mPoInterests = new ArrayList<ArrayList<PoInterest>>(PoInterest.NUM_POI_TYPE);
		mHilitedPois = new ArrayList<PoInterest>( 16 );								//pre-reserved 16 slots for hilited Pois;
		mFarDrawnPoints = new ArrayList<float[]>(); 
		
		mPaint = new Paint();
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setAntiAlias(true);
		mPaint.setColor(0xff00ff00);
		
		
		int idx;
		for(idx = 0; idx < Trail.NUM_TRAIL_TYPES; ++idx)
		{
			//TODO: the reserved capacity might need to be tuned
			ArrayList<Trail> trails = new ArrayList<Trail>(128);
			mTrails.add(trails);
		}
		
		for( idx = 0; idx < Area.NUM_AREA_TYPES; ++idx)
		{
			//TODO: the reserved capacity might need to be tuned
			ArrayList<Area> areas = new ArrayList<Area>(64);
			mAreas.add(areas);
		}
		
		for( idx = 0; idx < PoInterest.NUM_POI_TYPE; ++idx )
		{
			//TODO: the reserved capacity might need to be tuned
			ArrayList<PoInterest> poi = new ArrayList<PoInterest>(64);
			mPoInterests.add( poi );
		}
		
		
		mBBox = new RectF();
		mBBox.setEmpty();
		
		//paint style for draw the buddy direction vector
		mArrowEffect  = new PathDashPathEffect(Util.makeArrowPath( POINTER_DIRECTION_VECTOR_WIDTH ), POINTER_DIRECTION_VECTOR_WIDTH*3, 0, PathDashPathEffect.Style.MORPH);
				
		for(int i = 0; i < mLinePaints.length; i++)
		{
			mLinePaints[i] = new Paint( );
			mLinePaints[i].setStyle(Paint.Style.STROKE);
			mLinePaints[i].setStrokeWidth(POINTER_DIRECTION_VECTOR_WIDTH);
			mLinePaints[i].setAntiAlias(true);
			mLinePaints[i].setPathEffect(mArrowEffect);
			mLinePaints[i].setShadowLayer(2, 2, 2, Util.BLACK_COLOR);
			
			switch (i)
			{
				case PoInterest.POI_TYPE_SKICENTER : mLinePaints[i].setColor(PoInterest.SKICENTER_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_RESTAURANT : mLinePaints[i].setColor(PoInterest.RESTAURANT_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_BAR : mLinePaints[i].setColor(PoInterest.BAR_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_PARK : mLinePaints[i].setColor(PoInterest.PARK_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_CARPARKING : mLinePaints[i].setColor(PoInterest.CARPARKING_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_RESTROOM : mLinePaints[i].setColor(PoInterest.RESTROOM_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_CHAIRLIFTING :  mLinePaints[i].setColor(PoInterest.CHAIRLIFTING_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_SKIERDROPOFF_PARKING : mLinePaints[i].setColor(PoInterest.SKIERDROPOFF_PARKING_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_INFORMATION : mLinePaints[i].setColor(PoInterest.INFORMATION_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_HOTEL : mLinePaints[i].setColor(PoInterest.HOTEL_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_BANK : mLinePaints[i].setColor(PoInterest.BANK_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_SKISCHOOL : mLinePaints[i].setColor(PoInterest.SKISCHOOL_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_BUDDY : mLinePaints[i].setColor(PoInterest.BUDDY_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_OWNER : mLinePaints[i].setColor(PoInterest.OWNER_POINTER_COLOR); break;
				case PoInterest.POI_TYPE_CDP : mLinePaints[i].setColor(PoInterest.CDP_POINTER_COLOR); break;				
			}		
		}
	}
	
	public void addTrail( Trail trail )
	{
		mTrails.get(trail.getType()).add(trail);
		
		if( mBBox.isEmpty() )
		{
			mBBox.set(trail.mBBox);
		}
		else
		{
			mBBox.union(trail.mBBox);
		}
	}

	public void addArea( Area area )
	{
		mAreas.get(area.getType()).add(area);
		
		if( mBBox.isEmpty() )
		{
			mBBox.set(area.mBBox);
		}
		else
		{
			mBBox.union(area.mBBox);
		}
	}
	
	public void addPOI( PoInterest poi )
	{
		mPoInterests.get( poi.getType() ).add( poi );
		
		if( mBBox.isEmpty() )
		{
			mBBox.set( poi.mPosition.x, poi.mPosition.y, poi.mPosition.x, poi.mPosition.y );
		}
		else
		{
			mBBox.union(poi.mPosition.x, poi.mPosition.y);
		}
	}
	
	
	public void removePOI( PoInterest poi )
	{
		mPoInterests.get( poi.getType() ).remove(poi);
	}
	
	/**
	 * 
	 * Draw a map's all areas based on the current transform
	 * We transform the bounding box of the area using the given matrix,
	 * then test it against the viewport for culling. The area will be
	 * exclude from rendering if it is culled out by current viewport
	 */
	public void drawAreas( Canvas canvas, RectF viewPortBBox,  Matrix transform )
	{
		//the current viewport of canvas,
		//please note that the width/height of the canvas
		//is actually the screen size of the device, instead of the MapView viewport
		//that is why we have to pass in the actually viewPortBBox;
		float clipRight = viewPortBBox.width();
		float clipBottom = viewPortBBox.height();
		float top = viewPortBBox.top;
		float left = viewPortBBox.left;
		
		
		int count = 0;
		int total = 0;
		for( ArrayList<Area> areas : mAreas )
		{			
			total += areas.size();
			for( Area area : areas )
			{
				//transform the bounding box of the trail
				transform.mapRect(TEST_BOX, area.mBBox);
				
				//render the trail only if its bounding box
				//is intersected with the viewport
				if( TEST_BOX.intersects(left, top, clipRight, clipBottom) )
				{			
					area.draw(canvas, transform);
					++count;
				}			
			}
		}
		
		Log.d(DebugUtil.LOG_TAG_RENDERING, count + " out of " + total + " Areas are rendering" );
	}

	/**
	 * 
	 * Draw a map's all trails based on the current transform
	 * We transform the map elements, then test against 
	 * the viewport, if the trail is culled by the current
	 * viewport, the trail will be excluded from rendering.
	 */
	public void drawTrails( Canvas canvas, RectF viewPortBBox, Matrix transform )
	{
		//the current viewport of canvas,
		//please note that the width/height of the canvas
		//is actually the screen size of the device, instead of the MapView viewport
		//that is why we have to pass in the actually viewPortBBox;
		float clipRight = viewPortBBox.width();
		float clipBottom = viewPortBBox.height();
		float top = viewPortBBox.top;
		float left = viewPortBBox.left;
		
		
		int count = 0;
		int total = 0;
		
		//render all trails
		for( ArrayList<Trail> trails: mTrails )
		{
			total += trails.size();
			for( Trail trail : trails )
			{
				//transform the bounding box of the trail
				transform.mapRect(TEST_BOX, trail.mBBox);

				//render the trail only if its bounding box
				//is intersected with the viewport
				if( TEST_BOX.intersects(left, top, clipRight, clipBottom) )
				{
					trail.drawTrail(canvas, transform);
					trail.mIsCulled = false;
					++count;
				}
				else				
				{
					trail.mIsCulled =  true;
				}			
			}
		}
		
		Log.d(DebugUtil.LOG_TAG_RENDERING, count + " out of " + total + " Trails are rendering" );
	

		if( mDrawTrailNames )
		{
			//render all trail names
			for( ArrayList<Trail> trails: mTrails )
			{
				for( Trail trail : trails )
				{
					//no need to do the culling test again, since it
					//has already been set while rendering the trail
					if( trail.mIsCulled == false )
					{
						trail.drawName(canvas, transform);
					}							
				}
			}
		}
	}
	
	protected void drawFarPOI(Canvas canvas, RectF viewPortBBox, PoInterest poi, float[] mappedPOIPoint, int poiType)
	{
		PointF intP = new PointF(0.f, 0.f);
		
		PointF direction = new PointF();
		PointF location = new PointF();
				
		float centerX = (mDistancePerPixel*(mappedPOIPoint[0]-viewPortBBox.width()/2.f))/mScale;
		float centerY = (mDistancePerPixel*(mappedPOIPoint[1]-viewPortBBox.height()/2.f))/mScale;		

		//calculate the distance from the POI to the owner
		//float distance = (float)Math.sqrt(( poi.mPosition.x - centerX )*( poi.mPosition.x - centerX ) + ( poi.mPosition.y - centerY )*( poi.mPosition.y - centerY ));
		float distance = (float)Math.sqrt(( centerX )*( centerX ) + ( centerY )*( centerY ));
		//distance *= mDistancePerPixel;
		
		//the POI located outside of the current viewport, and is currently being tracked 
		//find the location to draw the icon, and draw some hints as well		
		float ownerX = viewPortBBox.width()/2.f;
		float ownerY = viewPortBBox.height()/2.f;
		
		boolean bIntersected = intersect( viewPortBBox, mappedPOIPoint, intP );

		//find the intersection point around the boundary, let's draw the POI icon around that area
		if( bIntersected )
		{
			//normalized the directional vector from owner to the position
			direction.set( mappedPOIPoint[0] - ownerX, mappedPOIPoint[1] - ownerY );
			float len = direction.length();
			direction.set(direction.x/len, direction.y/len);
			
			if(DRAW_DIRECTION)
			{
				location.set( intP.x - VIRTUAL_BORDER_WIDTH*direction.x, intP.y - VIRTUAL_BORDER_WIDTH*direction.y );
				canvas.drawLine(location.x, location.y, intP.x, intP.y, mLinePaints[poiType]);
			}
			else
			{
				// The 3 is for some anti aliasing offset?
				location.set( intP.x - VIRTUAL_BORDER_WIDTH_NO_DIR*direction.x + 3, intP.y - VIRTUAL_BORDER_WIDTH_NO_DIR*direction.y + 3 );
			}
			
			String label = null;
			if(mDrawPOIsNames || (mDrawTrackedNames && poi.isTracked()))
			{
				label = poi.mName;
			}
			//String label = (int)centerX + ":" + (int)poi.mPosition.x + ":" + (int)ownerX;
			poi.drawFar(canvas, viewPortBBox, location, label, (int)distance);
		}
	}
	
	/**
	 * 
	 * Draw a map's all p-o-i's based on the current transform
	 * The position of the POI will be transform by given matrix, 
	 * then tested on the viewport. If the position is not inside
	 * the viewport, it is excluded from rendering
	 */
	public void drawPOIs( Canvas canvas, RectF viewPortBBox, Matrix transform)
	{		
		//the current viewport of canvas,
		//please note that the width/height of the canvas
		//is actually the screen size of the device, instead of the MapView viewport
		//that is why we have to pass in the actually viewPortBBox;
		float clipRight = viewPortBBox.width();
		float clipBottom = viewPortBBox.height();
		float top = viewPortBBox.top;
		float left = viewPortBBox.left;
		boolean drawPOI = false;
		
		mFarDrawnPoints.clear();
				
		float[] mappedPoint = new float[2];
		
		mHilitedPois.clear();
		
		TEST_BOX.set(left, top, clipRight, clipBottom );
		
		int poiType = -1;
		for( ArrayList<PoInterest> pois : mPoInterests )
		{
			++poiType;
			//skip buddy for the normal rendering pass
			//since it is handled differently;
			if( poiType == PoInterest.POI_TYPE_BUDDY  || poiType == PoInterest.POI_TYPE_OWNER )
			{				
				continue;
			}
			
			for( PoInterest poi : pois )
			{
				drawPOI  = (mDrawPOIs || (mDrawTrackedPOIs && poi.isTracked()) || (mDrawTrackedPOIs && poi.isHilited()));
				
				mappedPoint[0] = poi.mPosition.x;
				mappedPoint[1] = poi.mPosition.y;
				
				transform.mapPoints(mappedPoint);
				
				if( TEST_BOX.contains( mappedPoint[0], mappedPoint[1] ))  // In the view
				{
					if(drawPOI || mForceDrawPOIs)
					{
						if(poi.isHilited())
						{
							mHilitedPois.add( poi );
						}
						else if(poi.isTracked() && mForceDrawPOIs) // We Draw all tracked points and labels if they we force them
						{
							poi.draw(canvas, transform, true);
						}						
						else if(drawPOI) // We draw all the other POI
						{
							poi.draw(canvas, transform, mDrawPOIsNames || (mDrawTrackedNames && poi.isTracked()));
						}
						else
						{
							poi.drawCircle(canvas, transform);
						}
					}
					else
					{
						poi.drawCircle(canvas, transform);
					}
				}
				else
				{
					// CDP - PINs are always shown
					if( poi.isTracked() && (drawPOI || mForceDrawPOIs))
					{
						drawFarPOI(canvas, viewPortBBox, poi, mappedPoint,poiType);
					}
				}
			}
		}
		
		drawBuddies(canvas, viewPortBBox, transform);
		
		//lastly, we draw all hilited pois
		for( PoInterest hilited : mHilitedPois )
		{
			hilited.draw(canvas, transform, true);
		}
		
		//finally draw the owner
		drawOwner( canvas, viewPortBBox, transform );
		
		//Log.d(DebugUtil.LOG_TAG_RENDERING, count + " out of " + total + " Point-of-Interests are rendering" );
	}
	
	/**	  
	 * Scan for buddies that are currently being tracked, and draw them inside the view port
	 * any way
	 */
	protected void drawBuddies( Canvas canvas, RectF viewPortBBox, Matrix transform )
	{
		//the current viewport of canvas,
		//please note that the width/height of the canvas
		//is actually the screen size of the device, instead of the MapView viewport
		//that is why we have to pass in the actually viewPortBBox;
		float clipRight = viewPortBBox.width();
		float clipBottom = viewPortBBox.height();
		
		float[] mappedPoint = new float[2];
		
		TEST_BOX.set(viewPortBBox.left, viewPortBBox.top, clipRight, clipBottom );
		
		Matrix inverseMat = new Matrix( transform );
		transform.invert(inverseMat);
		
		//calcute the center of the viewport's coord in the Map local space
		mappedPoint[0] = clipRight/2.f;
		mappedPoint[1] = clipBottom/2.f;
		inverseMat.mapPoints(mappedPoint);
		
		for( PoInterest poi : mPoInterests.get( PoInterest.POI_TYPE_BUDDY ) )
		{
			mappedPoint[0] = poi.mPosition.x;
			mappedPoint[1] = poi.mPosition.y;
			
			transform.mapPoints(mappedPoint);
			
			if( TEST_BOX.contains( mappedPoint[0], mappedPoint[1] ))
			{
				poi.draw(canvas, transform, mDrawPOIsNames || (mDrawTrackedNames && poi.isTracked()) );
			}
			else
			{
				if( poi.isTracked() )
				{	
					drawFarPOI(canvas, viewPortBBox, poi, mappedPoint,PoInterest.POI_TYPE_BUDDY);
				}
			}
		}	
	}
	
	public void drawPoint(Canvas canvas, RectF viewPortBBox, Matrix transform, float cx, float cy, float radius)
	{		
		if( this.mBBox.contains( cx, cy ) == false )
			return;
				
		float[] mappedPoint = new float[2];
	
		mappedPoint[0] = -cx;
		mappedPoint[1] = -cy;
		
		transform.mapPoints(mappedPoint);
		
		if( TEST_BOX.contains( mappedPoint[0], mappedPoint[1] ))
		{
			mPaint.setColor(0xffff00ff);
			canvas.drawCircle(mappedPoint[0], mappedPoint[1], radius, mPaint);			
		}		
	}
	
	protected void drawOwner( Canvas canvas, RectF viewPortBBox, Matrix transform  )
	{
		//no owner added yet(i.e, the owner is not within current resort, just do nothing then 
		if( mPoInterests.get(PoInterest.POI_TYPE_OWNER).size() == 0 )
			return;
		
		PoInterest owner = mPoInterests.get(PoInterest.POI_TYPE_OWNER).get( 0 );
		
		//if the owner is not inside current map, dont draw it
		if( this.mBBox.contains( owner.mPosition.x, owner.mPosition.y ) == false )
			return;
		
		//the current viewport of canvas,
		//please note that the width/height of the canvas
		//is actually the screen size of the device, instead of the MapView viewport
		//that is why we have to pass in the actually viewPortBBox;
		float clipRight = viewPortBBox.width();
		float clipBottom = viewPortBBox.height();
		
		
		float[] mappedPoint = new float[2];
		
		TEST_BOX.set(viewPortBBox.left, viewPortBBox.top, clipRight, clipBottom );
		PointF direction = new PointF();
		PointF location = new PointF();
		
		Matrix inverseMat = new Matrix( transform );
		transform.invert(inverseMat);
		
		//calcute the center of the viewport's coord in the Map local space
		mappedPoint[0] = clipRight/2.f;
		mappedPoint[1] = clipBottom/2.f;
		inverseMat.mapPoints(mappedPoint);
		
		float centerX = mappedPoint[0];
		float centerY = mappedPoint[1];
			
	
		mappedPoint[0] = owner.mPosition.x;
		mappedPoint[1] = owner.mPosition.y;
		
		transform.mapPoints(mappedPoint);
		
		//owner is visible, do regular drawing
		if( TEST_BOX.contains( mappedPoint[0], mappedPoint[1] ))
		{
			owner.draw(canvas, transform, true);
		}
		else
		{
			drawFarPOI(canvas, viewPortBBox, owner, mappedPoint, PoInterest.POI_TYPE_OWNER);
		}
	
	}
	public void reset( )
	{
		for( ArrayList<Trail> trails : mTrails )
		{
			trails.clear();
		}
		
		for( ArrayList<Area> areas : mAreas )
		{
			areas.clear();
		}
		
		for( ArrayList<PoInterest> pois : mPoInterests )
		{
			pois.clear();
		}
		mBBox.setEmpty();
	}
	
	public void setScaleFactor( float scale )
	{
		//let's keep the trail name text size not affected by the scale factor
		mScaleFactor = scale;		
		
		Trail.sTextPaint.setTextSize( Trail.NAME_TEXT_SIZE/mScaleFactor );
				
	}
		
	public boolean isEmpty()
	{
		return mBBox.isEmpty();
	}
	
	//return the device owner's positin in the map local space
	//Note, it is NOT the (lat, lng) pair but the local coord in the map space
	protected PointF getOwnerPosition()
	{
		ArrayList<PoInterest> ownerList = mPoInterests.get( PoInterest.POI_TYPE_OWNER );
		
		if( ownerList.size() > 0 )
		{
			return new PointF( ownerList.get(0).mPosition.x,  ownerList.get(0).mPosition.y );
		}
		else
		{
			return null;
		}
	}	
	
	
	//utility function for calculating the intersection point of a line and a rectangle, the point is
	//fire from the center of the rectangle to another point
	private boolean intersect( RectF rect, float[] testP, PointF intP )
	{
		float clipRight = rect.width();
		float clipBottom = rect.height();

		//the buddy locates outside of the current viewport, and is currently being tracked 
		//find the location to draw the icon, and draw some hints as well		
		float ownerX = clipRight/2.f;
		float ownerY = clipBottom/2.f;
		
		float top = 0;
		float bottom = clipBottom;
		float left = 0;
		float right = clipRight;
		
		float t;
		boolean bIntersected = false;
		
		//test against the top line y =top;
		if( ( ownerY > top && testP[1] < top ) || ( ownerY < top && testP[1] > top ) )
		{
			t =(top -ownerY)/(testP[1] - ownerY);
			if( t >= 0.f && t <= 1.f )
			{
				float x = ownerX + (testP[0] - ownerX)*t;
				if( x >= left && x <= right )
				{
					intP.set( x, top );
					bIntersected = true;
				}
			}
		}
		
		//test against the bottom line y = bottom
		if( !bIntersected && ((ownerY > bottom  && testP[1] < bottom ) || ( ownerY < bottom  && testP[1] > bottom ) ))
		{
			t = (bottom-ownerY)/(testP[1] - ownerY);
			if( t >= 0.f && t <= 1.f )
			{
				float x = ownerX + (testP[0] - ownerX)*t;
				if( x >= left && x <= right )
				{
					intP.set( x, bottom );
					bIntersected = true;
				}
			}
			
		}
		
		//test against the left line x = left
		if( !bIntersected && ( ( ownerX > left && testP[0] < left ) || ( ownerY < left && testP[0] > left)))
		{
			t =(left -ownerX)/(testP[0] - ownerX);
			if( t >= 0.f && t <= 1.f )
			{
				float y = ownerY + (testP[1] - ownerY)*t;
				if( y >= top && y <= bottom )
				{
					intP.set( left, y );
					bIntersected = true;
				}
			}
		}

		//test against the left line x = right
		if( !bIntersected && (( ownerX > right && testP[0] < right ) || ( ownerY < right && testP[0] > right )))
		{ 
			t =(right -ownerX)/(testP[0] - ownerX);
			if( t >= 0.f && t <= 1.f )
			{
				float y = ownerY + (testP[1] - ownerY)*t;
				if( y >= top && y <= bottom )
				{
					intP.set( right, y );
					bIntersected = true;
				}
			}
		}

		return bIntersected;
	}

}
