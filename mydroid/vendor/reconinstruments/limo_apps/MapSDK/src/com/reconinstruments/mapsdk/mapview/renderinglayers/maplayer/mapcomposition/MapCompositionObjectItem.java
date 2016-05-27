package com.reconinstruments.mapsdk.mapview.renderinglayers.maplayer.mapcomposition;

import com.reconinstruments.mapsdk.geodataservice.clientinterface.capabilities.Capability;
import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;

public class MapCompositionObjectItem extends MapCompositionItem {
	public Capability.DataSources mSource = Capability.DataSources.RECON_BASE;
	
	public MapCompositionObjectItem(WorldObjectDrawing.WorldObjectDrawingTypes objType, Capability.DataSources source) {
		super(objType);
		mSource = source;
	}

}
