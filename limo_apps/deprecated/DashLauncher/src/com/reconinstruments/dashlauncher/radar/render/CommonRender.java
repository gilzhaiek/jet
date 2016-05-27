package com.reconinstruments.dashlauncher.radar.render;

import android.util.Log;

public class CommonRender {
	// General Values
	public static final long BUDDY_NOT_FRESH_SECONDS	= 2*60; // 2 Minutes
	public static final long BUDDY_OFFLINE_SECONDS		= 5*60; // 5 Minutes
	
	// Colors
	public static final float FOG_GRAY_COLOR_R			= 81.0f/255.0f;
	public static final float FOG_GRAY_COLOR_G			= 105.0f/255.0f;
	public static final float FOG_GRAY_COLOR_B			= 127.0f/255.0f;
	
	// POI Dimension
	public static final float POI_ASPECT				= 1.37f;
	public static final float POI_WIDTH					= 1.0f;
	public static final float POI_HEIGHT				= 1.0f;
	public static final float POI_UNFOCUSED_TIP_HEIGHT	= 1.0f;
	public static final float POI_FOCUSED_TIP_HEIGHT	= 3.0f;
	public static final float POI_FOCUSED_TIP_WIDTH		= 0.5f;
	public static final float POI_OUTLINE_WIDTH_PX		= 1.0f;//3.0f;
	public static final float POI_FOCUSED_BOX_HEIGHT	= 1.1f; 
	
	// POI Colors
	public static final float POI_BORDER				= 0.0f/255.0f;
	public static final int POI_TYPE_UNDEFINED_COLOR	= 0x00FFFF;
	public static final int POI_TYPE_RESTAURANT_COLOR	= 0x006699;
	public static final int POI_TYPE_CHAIRLIFTING_COLOR	= 0x990000;
	public static final int POI_TYPE_BUDDY_COLOR		= 0x009933;
	
	// Focused POI BG Color
	public static final float POI_BG_COLOR				= 204.0f/255.0f; // 0xcc
	public static final float POI_TEXT					= 0.0f;
	public static final float POI_NOT_FRESH				= 0.4f;
	

	// Scaling of POIs and Transparency
	public static final float TOO_CLOSE_POI_ALPHA		= 0.2f; // When POI is covering the User
	public static final float MIN_POI_SCALE				= 0.3f; // Min Scale Value on the border of the distance
	public static final float MIN_POI_ALPHA				= 0.5f; // The fartest POI - what will be its Alpha
	public static final float ALWAYS_SOLID_POI_AREA		= 0.5f; // Based on the drawing area - where is the area where the POIs are solid
	
	// (Distance between 2 POIs divided by the distance to the group) - should give you a control ratio for bundling of groups
	public static final float POI_GROUP_RATIO			= 0.25f;
	public static final float POI_RADAR_GROUP_RATIO		= 0.20f;
	//public static final float POI_GROUP_RATIO			= 0.35f;
 
	
}
