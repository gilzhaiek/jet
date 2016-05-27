/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 *This class defined a point-of-interest item for rendering in a
 *resort map
 */
package com.reconinstruments.navigation.navigation;

import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.recon.prim.PointD;
import com.reconinstruments.navigation.R;
import com.reconinstruments.reconsettings.ReconSettingsUtil;

public class PoInterest
{

	static public final int POI_TYPE_UNDEFINED = -1;
	static public final int POI_TYPE_SKICENTER = 0;				//resort POI: GRMN_TYPE=SKI_CENTER, MDTYPE1=Resort
	static public final int POI_TYPE_RESTAURANT = 1;			//ski lodge:  GRMN_TYPE=RESTAURANT, or GRMN_TYPE=RESTAURANT_AMERICAN
	static public final int POI_TYPE_BAR = 2;					//bar:  GRMN_TYPE=BAR
	static public final int POI_TYPE_PARK=3;					//terrain park main entrance: GRMN_TYPE=PARK
	static public final int POI_TYPE_CARPARKING=4;				//Car parking: GRMN_TYPE=PARKING, MDTYPE1=Parking
	static public final int POI_TYPE_RESTROOM=5;				//Toilet(Standalone, not part of the lodge): GRMN_TYPE=RESTROOM
	static public final int POI_TYPE_CHAIRLIFTING=6;			//chair-lifting loading: GRMN_TYPE=SKI_CENTER, MDTYPE1=LiftLoading
	static public final int POI_TYPE_SKIERDROPOFF_PARKING=7;	//Parking for dropping off skiers: GRMN_TYPE=PARKING, MDTYPE1=Skier Dropoff
	static public final int POI_TYPE_INFORMATION=8;				//Ticket window: GMRN_TYPE=INFORMATION
	static public final int POI_TYPE_HOTEL=9;					//Hotel: GMRN_TYPE=HOTEL
	static public final int POI_TYPE_BANK=10;					//Bank: GRMN_TYPE=BANK
	static public final int POI_TYPE_SKISCHOOL=11;				//Skischool: GRMN_TYPE=SCHOOL
	static public final int POI_TYPE_BUDDY =12;					//Buddy around the site
	static public final int POI_TYPE_OWNER=13;					//the owner of this device;
	static public final int POI_TYPE_CDP=14;					//the customer defined POI(not supported yet)
	static public final int NUM_POI_TYPE = 15;
	
	static public final int SKICENTER_POINTER_COLOR = 0xff050708;
	static public final int RESTAURANT_POINTER_COLOR = 0xff944a96;//0xff6a317c;//8d50a0;
	static public final int BAR_POINTER_COLOR = 0xffdd5a9b;//0xffc1357f;//e05ea3;
	static public final int PARK_POINTER_COLOR = 0xffef7807;//0xffcb6514;//f57e20;
	static public final int CARPARKING_POINTER_COLOR = 0xff2494d2;//0xff0b77ac;//1c96d3;
	static public final int RESTROOM_POINTER_COLOR = 0xff33afa5;//0xff099487;//25b6a8;
	static public final int CHAIRLIFTING_POINTER_COLOR = 0xffe41b1b;//0xffca1516;//ed2426;
	static public final int SKIERDROPOFF_PARKING_POINTER_COLOR = 0xff2494d2;//0xff1275a7;//1c96d3;
	static public final int INFORMATION_POINTER_COLOR = 0xff333333;//0xff514f4f;//7a7a7a;
	static public final int HOTEL_POINTER_COLOR = 0xff8a3715;//0xff6f2910;//84371b;
	static public final int BANK_POINTER_COLOR = 0xff049839;//0xff078838;//14a34a;
	static public final int SKISCHOOL_POINTER_COLOR = 0xff000000;//0xff050708;
	static public final int BUDDY_POINTER_COLOR = 0xffc90d13;//ec1e24;
	static public final int OWNER_POINTER_COLOR = 0xff05c1c1;//00ffff;
	static public final int CDP_POINTER_COLOR = 0xff0a30d1;//123fff;
	static public final int PIN_POINTER_COLOR = 0xffa41016;//be2026;	
	
	static final int    NAME_TEXT_SIZE = 15;
	static final int    NAME_TEXT_HILITE_SIZE = 16;
	static final int    NAME_TEXT_COLOR = 0xffffffff;
	static final int    NAME_TEXT_TRACKED_COLOR = 0xffffffff;
	static final int    NAME_TEXT_HILITE_COLOR = 0xffffffff;	
	static final int    NAME_TEXT_HILITE_BACKGROUND_PADDING = 3;
	static final int    NAME_TEXT_DISTANCE_BACKGROUND_PADDING = 3;
	static final int    NAME_TEXT_NAME_BACKGROUND_PADDING = 1;
	
	static final int    ICON_SHADE_NORMAL = 0xffffffff;
	static final int    ICON_SHADE_HILITE = 0xffffffff;//0xfff6ff00;
	static final int 	ICON_SHADE_PADDING = 5;
	
	static final int 	POI_SCREEN_CORNER_WIDTH = 50;
	
	static final float	POI_SMALL_CIRCLE_RADIUS = 4;
	
	static final float	ICON_BORDER_WIDTH = 2;
	static final float 	RECT_DEF_RADIUS = 3;
	
	//buddie status
	static public final int    POI_STATUS_UNTRACKED = 0;
	static public final int    POI_STATUS_TRACKED = 1;
	
	//owner status
	static public final int    OWNER_STATUS_MEASURE_NONE = 0;			//no status
	static public final int    OWNER_STATUS_MEASURE_CENTER = 1;			//measure the distance from the center
	
	static final int    DRAW_OPTION_HILITE_LABEL = 0x01;
	static final int	DRAW_OPATION_HILITE_ICON = 0x02;
	
	//the poi icons files to be loaded from resource when initializing the application
	static private String[]  sPoIIconFileNames = { 
		"info.png",				//POI_TYPE_SKICENTER				 
		"restaurant.png", 		//POI_TYPE_RESTAURANT
		"bar.png", 				//POI_TYPE_BAR
		"park.png", 			//POI_TYPE_PARK
		"parking.png", 			//POI_TYPE_CARPARKING
		"restroom.png", 		//POI_TYPE_RESTROOM
		"lift.png",				//POI_TYPE_CHAIRLIFTING
		"parking.png",			//POI_TYPE_SKIERDROPOFF_PARKING
		"info.png",				//POI_TYPE_INFORMATION			
		"hotel.png",			//POI_TYPE_HOTEL
		"bank.png",				//POI_TYPE_BANK
		"skischool.png",		//POI_TYPE_SKISCHOOL
		"buddy.png",			//POI_TYPE_BUDDY
		"owner.png",			//POI_TYPE_OWNER	
		"pin.png"				//POI_TYPE_CDP 
	};		
	
	//the poi icons files to be loaded from resource when initializing the application
	static private String[]  sPoITrackedIconFileNames = { 
		"info_rev.png",				//POI_TYPE_SKICENTER				 
		"restaurant_rev.png", 		//POI_TYPE_RESTAURANT
		"bar_rev.png", 				//POI_TYPE_BAR
		"park_rev.png", 			//POI_TYPE_PARK
		"parking_rev.png", 			//POI_TYPE_CARPARKING
		"restroom_rev.png", 		//POI_TYPE_RESTROOM
		"lift_rev.png",				//POI_TYPE_CHAIRLIFTING
		"parking_rev.png",			//POI_TYPE_SKIERDROPOFF_PARKING
		"info_rev.png",				//POI_TYPE_INFORMATION			
		"hotel_rev.png",			//POI_TYPE_HOTEL
		"bank_rev.png",				//POI_TYPE_BANK
		"skischool_rev.png",		//POI_TYPE_SKISCHOOL
		"buddy.png",			//POI_TYPE_BUDDY
		"owner.png",			//POI_TYPE_OWNER	
		"pin.png"				//POI_TYPE_CDP 
	};		
	
	static private Bitmap[] sPoIIcons = null;					//the Poi icons used for rendering PoInterest
	static private Bitmap[] sPoITrackedIcons = null;					//the Poi icons used for rendering PoInterest
	static private Bitmap 	sHotBuddyIcon = null;
	static private Bitmap 	sFarOwnerIcon = null;
	
	static private Paint	sPoIPaint = null;					//the paint shared by all PoInterests
	static private Paint	sPoIIconPaint = null;				//the paint shared by all PoInterests
	
	static private Paint[]	sPoITextPaint			= null;				
	static private Paint	sPoITextTrackedPaint	= null;
	static private Paint	sPoIFarDistancePaint	= null;
	static private Paint	sPoIBGPaint				= null;
	static private Paint[]	sPoIColorPaint			= null;
	static private Paint[]	sPoIFarIconShade		= null;
	static private Paint[]	SPoICirclesPaint		= null;
		
	static private float[]	sTempPoint = new float[2];
	static private Rect		sTempBound = new Rect();
	static private RectF	sTempBoundF = new RectF();

	private enum EDrawDirection {eOriginTopBottom, eOriginRightLeft, eOriginBottomTop, eOriginLeftRight, eOriginCenter};	
	
	//the lat-lng of the point-of-interest
	public PointF mPosition;
	
	//the name of the point-of-interest
	public String mName;
	
	//the type of p-o-i
	private int mType;
	
	private boolean mHilited = false;
	
	private int mStatus = POI_STATUS_UNTRACKED;								//special field kept for some dynamic Poi such as buddies;
	
	//initialize painting related resources 	
	static public void InitPaints( Context context  )
	{
		//make sure not double-initialized
		if( sPoIIcons != null )
		{
			return;
		}
		
		sPoIIcons = new Bitmap[PoInterest.NUM_POI_TYPE];
		sPoITrackedIcons = new Bitmap[PoInterest.NUM_POI_TYPE];
		
		try
		{
			for( int i = 0; i < NUM_POI_TYPE; ++i )
			{
				InputStream stream = context.getAssets().open( "icons/" + sPoIIconFileNames[i] );
				sPoIIcons[i] = BitmapFactory.decodeStream(stream);
				
				stream.close();
			}
			
			for( int i = 0; i < NUM_POI_TYPE; ++i )
			{
				InputStream stream = context.getAssets().open( "icons/" + sPoITrackedIconFileNames[i] );
				sPoITrackedIcons[i] = BitmapFactory.decodeStream(stream);
				
				stream.close();
			}

			sHotBuddyIcon = BitmapFactory.decodeStream(context.getAssets().open( "icons/buddy_hot.png" ));

			sFarOwnerIcon = BitmapFactory.decodeStream(context.getAssets().open( "icons/owner_icon.png" ));			
		}
		catch(IOException e)
		{
			e.printStackTrace(System.out);
			Log.e(DebugUtil.LOG_TAG_LOADING, "ShpMap - Init Point-of-interest icons Exception", e);
		}	
		
		sPoIPaint = new Paint( );
		sPoIPaint.setAntiAlias(true);
		
		sPoIIconPaint = new Paint();
		sPoIIconPaint.setAntiAlias(false);
		
		sPoITextTrackedPaint = new Paint( );
		sPoITextTrackedPaint.setColor(Util.WHITE_COLOR);
		sPoITextTrackedPaint.setTextSize(NAME_TEXT_SIZE);
		sPoITextTrackedPaint.setTextAlign(Align.CENTER);
		sPoITextTrackedPaint.setAntiAlias(true);
		sPoITextTrackedPaint.setTypeface(Typeface.SANS_SERIF);
		sPoITextTrackedPaint.setTypeface(Typeface.DEFAULT_BOLD);

		sPoIFarDistancePaint = new Paint( );
		sPoIFarDistancePaint.setColor(Util.WHITE_COLOR);		
		sPoIFarDistancePaint.setTextSize(NAME_TEXT_SIZE+1);
		sPoIFarDistancePaint.setTextAlign(Align.CENTER);
		sPoIFarDistancePaint.setAntiAlias(true);
		sPoIFarDistancePaint.setTypeface(Typeface.SANS_SERIF);
		sPoIFarDistancePaint.setTypeface(Typeface.DEFAULT_BOLD);

		sPoIBGPaint = new Paint();
		sPoIBGPaint.setAntiAlias(true);
		sPoIBGPaint.setColor(Util.WHITE_COLOR);
		
		sPoIColorPaint = new Paint[PoInterest.NUM_POI_TYPE];
		SPoICirclesPaint = new Paint[PoInterest.NUM_POI_TYPE];
		sPoITextPaint = new Paint[PoInterest.NUM_POI_TYPE];
		sPoIFarIconShade = new Paint[PoInterest.NUM_POI_TYPE];
		for(int i = 0; i < NUM_POI_TYPE; i++)
		{
			sPoITextPaint[i] = new Paint( );
			sPoITextPaint[i].setTextSize(NAME_TEXT_SIZE);
			sPoITextPaint[i].setTextAlign(Align.CENTER);
			sPoITextPaint[i].setAntiAlias(true);
			sPoITextPaint[i].setTypeface(Typeface.SANS_SERIF);		
			sPoITextPaint[i].setTypeface(Typeface.DEFAULT_BOLD);

			sPoIColorPaint[i] = new Paint();
			sPoIColorPaint[i].setAntiAlias(true);
			
			SPoICirclesPaint[i]= new Paint( );
			SPoICirclesPaint[i].setAntiAlias(true);
			SPoICirclesPaint[i].setShadowLayer(3, 0, 0, Util.WHITE_COLOR);
			
			sPoIFarIconShade[i] = new Paint( );
			sPoIFarIconShade[i].setAntiAlias(true);
			sPoIFarIconShade[i].setShadowLayer(2, 2, 2, Util.BLACK_COLOR);

			switch (i)
			{
				case POI_TYPE_SKICENTER :
					sPoIColorPaint[i].setColor(PoInterest.SKICENTER_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.SKICENTER_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.SKICENTER_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.SKICENTER_POINTER_COLOR);
					break;
				case POI_TYPE_RESTAURANT :
					sPoIColorPaint[i].setColor(PoInterest.RESTAURANT_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.RESTAURANT_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.RESTAURANT_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.RESTAURANT_POINTER_COLOR);
					break;
				case POI_TYPE_BAR :
					sPoIColorPaint[i].setColor(PoInterest.BAR_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.BAR_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.BAR_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.BAR_POINTER_COLOR);
					break;
				case POI_TYPE_PARK :
					sPoIColorPaint[i].setColor(PoInterest.PARK_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.PARK_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.PARK_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.PARK_POINTER_COLOR);
					break;
				case POI_TYPE_CARPARKING :
					sPoIColorPaint[i].setColor(PoInterest.CARPARKING_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.CARPARKING_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.CARPARKING_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.CARPARKING_POINTER_COLOR);
					break;
				case POI_TYPE_RESTROOM :
					sPoIColorPaint[i].setColor(PoInterest.RESTROOM_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.RESTROOM_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.RESTROOM_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.RESTROOM_POINTER_COLOR);
					break;
				case POI_TYPE_CHAIRLIFTING : 
					sPoIColorPaint[i].setColor(PoInterest.CHAIRLIFTING_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.CHAIRLIFTING_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.CHAIRLIFTING_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.CHAIRLIFTING_POINTER_COLOR);
					break;
				case POI_TYPE_SKIERDROPOFF_PARKING : 
					sPoIColorPaint[i].setColor(PoInterest.SKIERDROPOFF_PARKING_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.SKIERDROPOFF_PARKING_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.SKIERDROPOFF_PARKING_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.SKIERDROPOFF_PARKING_POINTER_COLOR);
					break;
				case POI_TYPE_INFORMATION : 
					sPoIColorPaint[i].setColor(PoInterest.INFORMATION_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.INFORMATION_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.INFORMATION_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.INFORMATION_POINTER_COLOR);
					break;
				case POI_TYPE_HOTEL : 
					sPoIColorPaint[i].setColor(PoInterest.HOTEL_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.HOTEL_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.HOTEL_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.HOTEL_POINTER_COLOR);
					break;
				case POI_TYPE_BANK : 
					sPoIColorPaint[i].setColor(PoInterest.BANK_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.BANK_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.BANK_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.BANK_POINTER_COLOR);
					break;
				case POI_TYPE_SKISCHOOL : 
					sPoIColorPaint[i].setColor(PoInterest.SKISCHOOL_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.SKISCHOOL_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.SKISCHOOL_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.SKISCHOOL_POINTER_COLOR);
					break;
				case POI_TYPE_BUDDY : 
					sPoIColorPaint[i].setColor(PoInterest.BUDDY_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.BUDDY_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.BUDDY_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.BUDDY_POINTER_COLOR);
					break;
				case POI_TYPE_OWNER : 
					sPoIColorPaint[i].setColor(PoInterest.OWNER_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.OWNER_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.OWNER_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.OWNER_POINTER_COLOR);
					break;
				case POI_TYPE_CDP : 
					sPoIColorPaint[i].setColor(PoInterest.CDP_POINTER_COLOR);
					SPoICirclesPaint[i].setColor(PoInterest.CDP_POINTER_COLOR);
					sPoITextPaint[i].setColor(PoInterest.CDP_POINTER_COLOR);
					sPoIFarIconShade[i].setColor(PoInterest.CDP_POINTER_COLOR);
					break;				
			}		
		}
	}
	
	public int getType( )
	{
		return mType;
	}
	
	public boolean isTracked()
	{
		return ( mStatus == POI_STATUS_TRACKED || mType == POI_TYPE_CDP);
	}
	
	public void toggleStatus()
	{
		if( getStatus() != POI_STATUS_TRACKED )
		{
			setStatus(POI_STATUS_TRACKED);
		}
		else
		{
			setStatus(POI_STATUS_UNTRACKED);
		}		
	}
	
	public int getStatus( )
	{
		return mStatus;		
	}
	
	public void setStatus( int status )
	{
		mStatus = status;
	}
		
	public PoInterest( PointD pos, String name, int poiType )
	{
		mPosition = new PointF( (float)pos.x, (float)pos.y );
		mName = name;
		mType = poiType;		
	}
	
	public PoInterest( float x, float y, String name, int poiType )
	{
		mPosition = new PointF( x, y );
		mName = name;
		mType = poiType;		
		
	}
		
	public Bitmap getIcon()
	{
		return getIcon(false);
	}

	public Bitmap getIcon(boolean isFar)
	{
		return getIcon(mStatus, mType, mHilited, isFar);
	}
	
	public static Bitmap getIcon(int poiType)
	{
		if(sPoIIcons == null)
		{
			return null;
		}
		
		if( poiType == PoInterest.POI_TYPE_OWNER)
			return sFarOwnerIcon;
		
		return sPoIIcons[poiType];
	}	

	public Bitmap getIcon( int status, int poiType, boolean hilited, boolean isFar)
	{
		if( poiType == PoInterest.POI_TYPE_BUDDY && status == PoInterest.POI_STATUS_TRACKED )
			return sHotBuddyIcon;
	
		if( isFar && mType == PoInterest.POI_TYPE_OWNER)
			return sFarOwnerIcon;		
		
		if(isFar)
			return sPoITrackedIcons[poiType];
		
		if(status == PoInterest.POI_STATUS_TRACKED && !hilited)
			return sPoITrackedIcons[poiType];
		
		return sPoIIcons[poiType];
	}	
	
	protected RectF getRectPosition(float width, float height, int padding, PointF originLocation, EDrawDirection xDirection, EDrawDirection yDirection)
	{
		RectF retRectF = new RectF();
		
		if(xDirection == EDrawDirection.eOriginLeftRight)
		{
			retRectF.left = originLocation.x;
			retRectF.right = retRectF.left + width + 2*padding;
		}
		else if(xDirection == EDrawDirection.eOriginRightLeft)
		{
			retRectF.right = originLocation.x;
			retRectF.left = retRectF.right - width - 2*padding;
		}
		else // Centre 
		{
			retRectF.left = originLocation.x - width/2.f - padding;
			retRectF.right = originLocation.x + width/2.f + padding;		
		}
		
		if(yDirection == EDrawDirection.eOriginTopBottom)
		{
			retRectF.top = originLocation.y;
			retRectF.bottom = retRectF.top + height + 2*padding;
		}
		else if (yDirection == EDrawDirection.eOriginBottomTop)
		{
			retRectF.bottom = originLocation.y;
			retRectF.top = retRectF.bottom - height - 2*padding;		
		}
		else // Centre 
		{
			retRectF.bottom = originLocation.y - height/2.f - padding;
			retRectF.top = originLocation.y + height/2.f + padding;		
		}	
		
		return retRectF;
	}
	
	protected RectF drawFarDistanceLabel(Canvas canvas, int distance, PointF originLocation, EDrawDirection xDirection, EDrawDirection yDirection)
	{
		String distanceLbl = ( Util.mUnits == ReconSettingsUtil.RECON_UINTS_METRIC ) ?
									distance + "m" :
									(int)Util.meterToFeet( distance ) + "ft";
		
		sPoIFarDistancePaint.getTextBounds(distanceLbl, 0, distanceLbl.length(), sTempBound);
		
		RectF tempRectF =  getRectPosition(sTempBound.width(),sTempBound.height(), NAME_TEXT_DISTANCE_BACKGROUND_PADDING, originLocation, xDirection, yDirection);
		
		canvas.drawRoundRect(tempRectF, RECT_DEF_RADIUS, RECT_DEF_RADIUS, sPoIColorPaint[mType]);
		canvas.drawText(distanceLbl, tempRectF.left + (tempRectF.width())/2.f, tempRectF.bottom-NAME_TEXT_DISTANCE_BACKGROUND_PADDING, sPoIFarDistancePaint);
		
		return tempRectF;
	}

	protected RectF drawFarNameLabel(Canvas canvas, String nameLbl, PointF originLocation, EDrawDirection xDirection, EDrawDirection yDirection)
	{
		sPoITextPaint[mType].getTextBounds(nameLbl, 0, nameLbl.length(), sTempBound);

		RectF tempRectF = getRectPosition(sTempBound.width(),sTempBound.height(), NAME_TEXT_NAME_BACKGROUND_PADDING, originLocation, xDirection, yDirection);
		
		canvas.drawRoundRect(tempRectF, RECT_DEF_RADIUS, RECT_DEF_RADIUS, sPoIColorPaint[mType]);
		canvas.drawText(nameLbl, tempRectF.left + (tempRectF.width())/2.f, tempRectF.bottom-3, sPoITextTrackedPaint);
		
		return tempRectF;
	}
	
	protected void drawIconBackground(Canvas canvas, RectF iconBounds)
	{
		RectF bgBorder = new RectF(
				iconBounds.left-ICON_BORDER_WIDTH,
				iconBounds.top-ICON_BORDER_WIDTH,
				iconBounds.right+ICON_BORDER_WIDTH,
				iconBounds.bottom+ICON_BORDER_WIDTH);
				
		if(mStatus == POI_STATUS_TRACKED && !mHilited){
			canvas.drawRoundRect(bgBorder, RECT_DEF_RADIUS, RECT_DEF_RADIUS, sPoIColorPaint[mType]);
		}
		else {
			canvas.drawRoundRect(bgBorder, RECT_DEF_RADIUS, RECT_DEF_RADIUS, sPoIColorPaint[mType]);
			canvas.drawRoundRect(iconBounds, RECT_DEF_RADIUS, RECT_DEF_RADIUS, sPoIBGPaint);
		}
	}
	
	protected void drawFarIconShade(Canvas canvas, RectF iconBounds)
	{
		canvas.drawRoundRect(iconBounds, RECT_DEF_RADIUS, RECT_DEF_RADIUS, PoInterest.sPoIFarIconShade[mType]);
	}
	
	public void drawCircle(Canvas canvas, Matrix transform)
	{
		sTempPoint[0] = mPosition.x ;
		sTempPoint[1] = mPosition.y;
		
		transform.mapPoints(sTempPoint);
		
		canvas.drawCircle(sTempPoint[0], sTempPoint[1], POI_SMALL_CIRCLE_RADIUS, PoInterest.SPoICirclesPaint[mType]);
	}
	
	public void draw( Canvas canvas, Matrix transform , boolean drawLabel)
	{		
		sTempPoint[0] = mPosition.x ;
		sTempPoint[1] = mPosition.y;
		
		transform.mapPoints(sTempPoint);
		
		Bitmap icon = getIcon(false);

		sTempPoint[0] -= icon.getWidth()/2;
		sTempPoint[1] -= icon.getHeight()/2;

		RectF iconBound = new RectF(
				sTempPoint[0]-ICON_SHADE_PADDING,
				sTempPoint[1]-ICON_SHADE_PADDING,
				sTempPoint[0] + icon.getWidth()+ICON_SHADE_PADDING, 
				sTempPoint[1] + icon.getHeight()+ICON_SHADE_PADDING);		
		
		if(!( mType == PoInterest.POI_TYPE_OWNER || mType == PoInterest.POI_TYPE_BUDDY || mType == PoInterest.POI_TYPE_CDP))
		{
			drawIconBackground(canvas, iconBound);
		}
		
		canvas.drawBitmap( icon, sTempPoint[0], sTempPoint[1], sPoIIconPaint );
		
		//reset the sTempPoint as the center for drawing label
		sTempPoint[0] += icon.getWidth()/2;
		sTempPoint[1] += (icon.getHeight() + 2*ICON_SHADE_PADDING);
		
		//draw the name of the POI if the name is not null
		if( mName != null && drawLabel)
		{
			drawLabel( canvas, mName, sTempPoint[0], sTempPoint[1]);
		}
	}	
	//draw Poi at the specified location with specified name
	public void drawFar( Canvas canvas, RectF viewPortBBox, PointF location, String nameLbl, int distance)
	{
		int extraPixels = 7;
		
		Bitmap icon = getIcon(true);

		// Make sure no icons is being drawn outside the box
		sTempPoint[0] = location.x - icon.getWidth()/2;
		sTempPoint[1] = location.y - icon.getHeight()/2;
		
		RectF iconBound = new RectF(
				location.x - icon.getWidth()/2-ICON_SHADE_PADDING,
				location.y - icon.getHeight()/2-ICON_SHADE_PADDING,
				location.x + icon.getWidth()/2+ICON_SHADE_PADDING, 
				location.y + icon.getHeight()/2+ICON_SHADE_PADDING);
		
		/*  Our Screen Split:
		 * 
		 *  AABBBBCC
		 *  DD    EE
		 *  FFGGGGHH
		 */
		/*if(location.y < POI_SCREEN_CORNER_WIDTH)
		{
			if(location.x < POI_SCREEN_CORNER_WIDTH)  // A
			{
				RectF tempRectF = drawFarDistanceLabel(canvas, distance, new PointF(iconBound.left, iconBound.bottom), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginTopBottom);
				if( nameLbl != null ) {drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.left, iconBound.bottom + tempRectF.height()), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginTopBottom);}				
			}
			else if(location.x > (viewPortBBox.width() - POI_SCREEN_CORNER_WIDTH)) // C 
			{
				RectF tempRectF = drawFarDistanceLabel(canvas, distance, new PointF(iconBound.right, iconBound.bottom), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginTopBottom);
				if( nameLbl != null ) {drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.right, iconBound.bottom + tempRectF.height()), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginTopBottom);}				
			}
			else // B
			{
				RectF tempRectF = drawFarDistanceLabel(canvas, distance, new PointF(location.x, iconBound.bottom), EDrawDirection.eOriginCenter, EDrawDirection.eOriginTopBottom);
				if( nameLbl != null ) {drawFarNameLabel(canvas, nameLbl, new PointF(location.x, iconBound.bottom + tempRectF.height()), EDrawDirection.eOriginCenter, EDrawDirection.eOriginTopBottom);}				
			}
		}
		else if(location.y > (viewPortBBox.height() - POI_SCREEN_CORNER_WIDTH))
		{
			if(location.x < POI_SCREEN_CORNER_WIDTH) // F
			{
				RectF tempRectF = drawFarDistanceLabel(canvas, distance, new PointF(iconBound.left, iconBound.top), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginBottomTop);
				if( nameLbl != null ) {drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.left, iconBound.top - tempRectF.height()), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginBottomTop);}								
			}
			else if(location.x > (viewPortBBox.width() - POI_SCREEN_CORNER_WIDTH)) // H
			{
				RectF tempRectF = drawFarDistanceLabel(canvas, distance, new PointF(iconBound.right, iconBound.top), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginBottomTop);
				if( nameLbl != null ) {drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.right, iconBound.top - tempRectF.height()), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginBottomTop);}								
			}
			else // G
			{
				RectF tempRectF = drawFarDistanceLabel(canvas, distance, new PointF(location.x, iconBound.top), EDrawDirection.eOriginCenter, EDrawDirection.eOriginBottomTop);
				if( nameLbl != null ) {drawFarNameLabel(canvas, nameLbl, new PointF(location.x, iconBound.top - tempRectF.height()), EDrawDirection.eOriginCenter, EDrawDirection.eOriginBottomTop);}				
			}			
		}
		else
		{
			if(location.x < viewPortBBox.width()/2) // D
			{
				RectF tempRectF = drawFarDistanceLabel(canvas, distance, new PointF(iconBound.right, iconBound.top), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginTopBottom);
				if( nameLbl != null ) {drawFarNameLabel(canvas, nameLbl,  new PointF(iconBound.right, iconBound.top + tempRectF.height()), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginTopBottom);}								
			}
			else // E
			{
				RectF tempRectF = drawFarDistanceLabel(canvas, distance, new PointF(iconBound.left, iconBound.top), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginTopBottom);
				if( nameLbl != null ) {drawFarNameLabel(canvas, nameLbl,  new PointF(iconBound.left, iconBound.top + tempRectF.height()), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginTopBottom);}								
			}			
		}*/
		
		if( nameLbl == null )
		{
			nameLbl = "-";
			switch (mType)
			{
				case POI_TYPE_SKICENTER : nameLbl = "Ski Center";break;
				case POI_TYPE_RESTAURANT : nameLbl = "Restaurant";break;
				case POI_TYPE_BAR : nameLbl = "Bar";break;
				case POI_TYPE_PARK : nameLbl = "Park";break;
				case POI_TYPE_CARPARKING : nameLbl = "Car Parking";break;
				case POI_TYPE_RESTROOM : nameLbl = "Restrooms";break;
				case POI_TYPE_CHAIRLIFTING : nameLbl = "Chair Lift";break; 
				case POI_TYPE_SKIERDROPOFF_PARKING :nameLbl = "Skier Dropoff";break; 
				case POI_TYPE_INFORMATION : nameLbl = "Information";break; 
				case POI_TYPE_HOTEL : nameLbl = "Hotel";break; 
				case POI_TYPE_BANK : nameLbl = "Bank";break; 
				case POI_TYPE_SKISCHOOL :nameLbl = "Ski School";break; 
				case POI_TYPE_BUDDY : nameLbl = "Buddy";break; 
				case POI_TYPE_OWNER : nameLbl = "Owner";break;
				case POI_TYPE_CDP : nameLbl = "Pin";break; 
			}		
		}			
						
		if(location.y < POI_SCREEN_CORNER_WIDTH)
		{
			if(location.x < POI_SCREEN_CORNER_WIDTH)  // A
			{
				RectF tempRectF = drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.left, iconBound.bottom), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginTopBottom);
				drawFarDistanceLabel(canvas, distance, new PointF(iconBound.left, iconBound.bottom + tempRectF.height()), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginTopBottom);				
			}
			else if(location.x > (viewPortBBox.width() - POI_SCREEN_CORNER_WIDTH)) // C 
			{
				RectF tempRectF = drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.right, iconBound.bottom), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginTopBottom);
				drawFarDistanceLabel(canvas, distance, new PointF(iconBound.right, iconBound.bottom + tempRectF.height()), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginTopBottom);				
			}
			else // B
			{
				RectF tempRectF = drawFarNameLabel(canvas, nameLbl, new PointF(location.x, iconBound.bottom), EDrawDirection.eOriginCenter, EDrawDirection.eOriginTopBottom);
				drawFarDistanceLabel(canvas, distance, new PointF(location.x, iconBound.bottom + tempRectF.height()), EDrawDirection.eOriginCenter, EDrawDirection.eOriginTopBottom);				
			}
		}
		else if(location.y > (viewPortBBox.height() - POI_SCREEN_CORNER_WIDTH))
		{
			if(location.x < POI_SCREEN_CORNER_WIDTH) // F
			{
				RectF tempRectF = drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.left, iconBound.top), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginBottomTop);
				drawFarDistanceLabel(canvas, distance, new PointF(iconBound.left, iconBound.top - tempRectF.height()), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginBottomTop);
			}
			else if(location.x > (viewPortBBox.width() - POI_SCREEN_CORNER_WIDTH)) // H
			{
				RectF tempRectF = drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.right, iconBound.top), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginBottomTop);
				drawFarDistanceLabel(canvas, distance, new PointF(iconBound.right, iconBound.top - tempRectF.height()), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginBottomTop);								
			}
			else // G
			{
				RectF tempRectF = drawFarNameLabel(canvas, nameLbl, new PointF(location.x, iconBound.top), EDrawDirection.eOriginCenter, EDrawDirection.eOriginBottomTop);
				drawFarDistanceLabel(canvas, distance, new PointF(location.x, iconBound.top - tempRectF.height()), EDrawDirection.eOriginCenter, EDrawDirection.eOriginBottomTop);				
			}			
		}
		else
		{
			if(location.x < viewPortBBox.width()/2) // D
			{
				RectF tempRectF = drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.right, iconBound.top), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginTopBottom);
				drawFarDistanceLabel(canvas, distance,  new PointF(iconBound.right, iconBound.top + tempRectF.height()), EDrawDirection.eOriginLeftRight, EDrawDirection.eOriginTopBottom);								
			}
			else // E
			{
				RectF tempRectF = drawFarNameLabel(canvas, nameLbl, new PointF(iconBound.left, iconBound.top), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginTopBottom);
				drawFarDistanceLabel(canvas, distance,  new PointF(iconBound.left, iconBound.top + tempRectF.height()), EDrawDirection.eOriginRightLeft, EDrawDirection.eOriginTopBottom);
			}			
		}
		
		if(!( mType == PoInterest.POI_TYPE_OWNER || mType == PoInterest.POI_TYPE_BUDDY || mType == PoInterest.POI_TYPE_CDP))
		{
			drawFarIconShade(canvas, iconBound);
		}
		
		canvas.drawBitmap( icon, sTempPoint[0], sTempPoint[1], PoInterest.sPoIPaint );
		
		//reset the sTempPoint as the center for drawing label
		sTempPoint[0] = location.x ;
		//3 pixel down the icon;
		sTempPoint[1] = location.y + icon.getHeight()/2 + 3;
	}
	
	// Draw the name label of POI
	private void drawLabel( Canvas canvas, String label, float centerX, float topY )
	{	
		//float labelLen = PoInterest.sPoITextPaint.measureText(mName);
		//draw a semi-transparent black box around the name for hiliting
		if(mStatus == POI_STATUS_TRACKED)
		{
			sPoITextTrackedPaint.getTextBounds(label, 0, label.length(), sTempBound);
		}
		else
		{
			sPoITextPaint[mType].getTextBounds(label, 0, label.length(), sTempBound);
		}
		
		float w = sTempBound.width();
		float h = sTempBound.height();
		float centerY = topY + h/2.f + NAME_TEXT_HILITE_BACKGROUND_PADDING-3;
		sTempBoundF.left = centerX - w/2.f - NAME_TEXT_HILITE_BACKGROUND_PADDING;
		sTempBoundF.top = centerY - h/2.f - NAME_TEXT_HILITE_BACKGROUND_PADDING+1;
		sTempBoundF.right = sTempBoundF.left + w + 2*NAME_TEXT_HILITE_BACKGROUND_PADDING;
		sTempBoundF.bottom = sTempBoundF.top + h + NAME_TEXT_HILITE_BACKGROUND_PADDING;
		
		if( mHilited )
		{			
			RectF tempRectF = new RectF(sTempBoundF.left-2, sTempBoundF.top-2, sTempBoundF.right+2, sTempBoundF.bottom+2);
			canvas.drawRoundRect(tempRectF, RECT_DEF_RADIUS, RECT_DEF_RADIUS,  sPoIColorPaint[mType]);
			canvas.drawRoundRect(sTempBoundF, RECT_DEF_RADIUS, RECT_DEF_RADIUS, sPoIBGPaint);
			canvas.drawText(label, centerX, centerY + h/3, sPoITextPaint[mType]);
		}
		else
		{
			if(mStatus == POI_STATUS_TRACKED)
			{
				canvas.drawRoundRect(sTempBoundF, RECT_DEF_RADIUS, RECT_DEF_RADIUS, sPoIColorPaint[mType]);					
			}
			canvas.drawText(label, centerX, centerY + h/3, sPoITextTrackedPaint);
		}
	}
	
	/**
	 * Check if a POI is dynamic or not:
	 * Current, three type of POI's are dynamic
	 * BUDDY, OWNER and CDP(Customer defined POI, which is not yet supported)
	 */
	private boolean isDynamic( )
	{
		return (mType == PoInterest.POI_TYPE_CDP || mType == PoInterest.POI_TYPE_OWNER || mType == PoInterest.POI_TYPE_BUDDY );
	}
	
	/** 
	 * Update the position of the POI
	 * should only applied to BUDDY type of POI
	 * or custom-created POI.
	 */
	public void updatePostion( float x, float y )
	{
		if( isDynamic() )
		{
			mPosition.set(x,y);
		}
		else
		{
			Log.e(DebugUtil.LOG_TAG_RENDERING, "Can not update the position of a static POI");
		}
	}
	
	/**
	 * Set the poi as hilited or not
	 */
	public void setHilited( boolean flag )
	{
		mHilited = flag;
	}
	
	/**
	 * Check if a poi is hilited or not
	 */
	public boolean isHilited()
	{
		return mHilited;
	}	
}