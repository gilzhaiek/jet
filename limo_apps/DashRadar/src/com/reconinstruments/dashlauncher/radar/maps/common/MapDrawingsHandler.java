package com.reconinstruments.dashlauncher.radar.maps.common;

import com.reconinstruments.dashlauncher.radar.maps.drawings.MapDrawings;

public class MapDrawingsHandler {
	
	public MapDrawings mMapDrawings = null;
	
    public MapDrawingsHandler() {
		mMapDrawings = new MapDrawings();
    }
    
    public void onProgressUpdate(int progress) {
    }
    
    public void onPostExecute(MapDrawings mapDrawings) {
    	mMapDrawings = mapDrawings;
    }    
}

