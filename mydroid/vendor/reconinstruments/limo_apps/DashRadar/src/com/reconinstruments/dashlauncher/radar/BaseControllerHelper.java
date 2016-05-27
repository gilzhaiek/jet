package com.reconinstruments.dashlauncher.radar;

import com.reconinstruments.dashlauncher.radar.maps.helpers.LocationTransformer;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

public class BaseControllerHelper {
	protected static Activity mActivity = null;
	protected BaseRendererHelper mRendererHelper = null;
	protected static RendererHandler mRendererHandler = null;
	protected LocationTransformer mLocationTransformer = null;
	
	public BaseControllerHelper(Activity activity, BaseRendererHelper rendererHelper, LocationTransformer locationTransformer){
		mActivity = activity;
		mRendererHelper = rendererHelper;
		mLocationTransformer = locationTransformer;
	} 
	
	public void SetRendererHandler(RendererHandler rendererHandler){
		mRendererHandler = rendererHandler;
	}
	
	public BaseRendererHelper GetRendererHelper(){
		return mRendererHelper; 
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		return false;
	}
	
	public void UpdateBuddies(Bundle bundle, Location userLocation){	}
	
	public boolean IsTopLevel(){return true;}
	
	protected static void SetFullScreenMode( boolean on )
	{
		Window window = mActivity.getWindow(); 
		if(on)
		{
			window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		else
		{
			window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			window.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}

	public void onResume(){
		mRendererHelper.onResume();
	}
	
	public void onPause(){
		mRendererHelper.onPause();
	}
	
	
	public void OnLocationHeadingChanged(Location location, float yaw, float pitch, boolean gpsHeading) {
	}		
}
