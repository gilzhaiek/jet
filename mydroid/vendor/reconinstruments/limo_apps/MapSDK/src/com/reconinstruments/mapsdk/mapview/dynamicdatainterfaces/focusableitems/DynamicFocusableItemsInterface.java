package com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.focusableitems;

import java.util.ArrayList;

import com.reconinstruments.mapsdk.mapview.WO_drawings.POIDrawing;


public class DynamicFocusableItemsInterface {
	public interface IDynamicFocusableItems {
		public ArrayList<POIDrawing> GetFocusableItems();		
	}
}

