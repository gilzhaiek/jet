package com.reconinstruments.mapImages.common;

import com.reconinstruments.mapImages.drawings.MapDrawings;

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

