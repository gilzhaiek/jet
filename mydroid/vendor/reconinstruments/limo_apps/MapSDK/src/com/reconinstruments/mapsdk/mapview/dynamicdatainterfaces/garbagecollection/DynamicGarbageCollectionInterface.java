package com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.garbagecollection;

import java.util.ArrayList;

import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;


public class DynamicGarbageCollectionInterface {
	public interface IDynamicGarbageCollection {
		public void DoGarbageCollection(boolean trackingUser);		
	}
}

