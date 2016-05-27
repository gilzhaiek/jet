package com.reconinstruments.mapsdk.mapview.dynamicdatainterfaces.reticleitems;

import java.util.ArrayList;

import com.reconinstruments.mapsdk.mapview.WO_drawings.WorldObjectDrawing;
import com.reconinstruments.mapsdk.mapview.camera.CameraViewport;
import com.reconinstruments.mapsdk.mapview.renderinglayers.reticulelayer.ReticleItem;

public class DynamicReticleItemsInterface {
	public interface IDynamicReticleItems {
		/**
		 * This returns ReticleItems with its location converted to drawing coordinates
		 * @param camera
		 * @param withinDistInM
		 * @return List of ReticleItems that will be drawn in ReticuleLayer
		 */
		public ArrayList<ReticleItem> GetReticleItems(CameraViewport camera, float withinDistInM);		
	}

}
