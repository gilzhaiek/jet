package com.reconinstruments.dashlauncher.radar.maps.bundlers;

import android.os.Bundle;

public class MapBundle {
	public Bundle mBundle = null;
	
	public MapBundle(){
		mBundle = new Bundle();
	}
	
	public void Release() {
		if(mBundle != null) {
			mBundle.clear();
		}
	}
	
	public static boolean IsMyInstance(Object object) {
		return object instanceof MapBundle; 
	}	
}
