package com.reconinstruments.dashlauncher.radar.maps.bo;

import com.reconinstruments.dashlauncher.radar.maps.objects.Area;
import com.reconinstruments.dashlauncher.radar.maps.objects.POI;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortElementsList;
import com.reconinstruments.dashlauncher.radar.maps.objects.Trail;
import com.reconinstruments.dashlauncher.radar.maps.objects.ResortElementsList.EElementArea;
import com.reconinstruments.dashlauncher.radar.maps.shp.ShpType;

public class ResortElementsListBO {
	public ResortElementsListBO(){
		
	}
	
	public static void Initialize(ResortElementsList resortElementsList){
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYLINE,	Trail.GREEN_TRAIL,		"GREEN_TRAIL");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYLINE,	Trail.GREEN_TRUNK,		"GREEN_TRUNK");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYLINE,	Trail.BLUE_TRAIL,		"BLUE_TRAIL");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYLINE,	Trail.BLUE_TRUNK,		"BLUE_TRUNK");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYLINE,	Trail.BLACK_TRAIL,		"BLACK_TRAIL");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYLINE,	Trail.BLACK_TRUNK,		"BLACK_TRUNK");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYLINE,	Trail.SKI_LIFT,			"SKI_LIFT");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYLINE,	Trail.CHWY_RESID_TRAIL,	"CHWY_RESID");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYLINE,	Trail.WALKWAY_TRAIL,	"TRAIL");
		
		resortElementsList.AddElement(ResortElementsList.EElementArea.eAmericas, ShpType.SHAPE_POLYLINE, Trail.DBLBLACK_TRAIL,"DBLBLCK_TRAIL");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eAmericas, ShpType.SHAPE_POLYLINE, Trail.DBLBLACK_TRUNK,"DBLBLCK_TRUNK");
		
		resortElementsList.AddElement(ResortElementsList.EElementArea.eNotAmericas, ShpType.SHAPE_POLYLINE, Trail.RED_TRAIL,		"RED_TRAIL");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eNotAmericas, ShpType.SHAPE_POLYLINE, Trail.RED_TRUNK,		"RED_TRUNK");
		
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYGON,	Area.AREA_WOODS,		"WOODS");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYGON,	Area.AREA_SCRUB,		"SCRUB");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYGON,	Area.AREA_TUNDRA,		"TUNDRA");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POLYGON,	Area.AREA_PARK,			"URBAN_PARK");
		
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POINT,		POI.POI_TYPE_CHAIRLIFTING,	"SKI_CENTER");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POINT,		POI.POI_TYPE_RESTAURANT,	"RESTAURANT_AMERICAN");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POINT,		POI.POI_TYPE_CARPARKING,	"PARKING");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POINT,		POI.POI_TYPE_SKISCHOOL,		"SCHOOL");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POINT,		POI.POI_TYPE_INFORMATION,	"INFORMATION");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POINT,		POI.POI_TYPE_BAR,			"BAR");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POINT,		POI.POI_TYPE_RESTROOM,		"RESTROOM");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POINT,		POI.POI_TYPE_HOTEL,			"HOTEL");
		resortElementsList.AddElement(ResortElementsList.EElementArea.eBoth, ShpType.SHAPE_POINT,		POI.POI_TYPE_BANK,			"BANK");
	}
}
