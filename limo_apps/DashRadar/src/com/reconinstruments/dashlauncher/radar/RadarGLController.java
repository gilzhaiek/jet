package com.reconinstruments.dashlauncher.radar;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;

public class RadarGLController extends RendererHandler {
	private static final String TAG = "RadarGLController";
	
	private RadarGLView				mRadarGLView = null;
	private RadarGLRenderer 		mRadarGLRenderer = null;
	
	private boolean					mIsResumed = false;
	
	private BaseControllerHelper	mControllerHelper = null;
	
	public RadarGLController(Activity activity) {
		mRadarGLView = new RadarGLView((Context)activity);  
		mRadarGLRenderer = new RadarGLRenderer();
		
		mRadarGLView.setRenderer(mRadarGLRenderer);
		mRadarGLView.setRenderMode(RadarGLView.RENDERMODE_WHEN_DIRTY);
		//mRadarGLView.setRenderMode(RadarGLView.RENDERMODE_CONTINUOUSLY);
				
		SetGLSurfaceView(mRadarGLView);
		
		mControllerHelper = null;
		
		activity.setContentView(mRadarGLView);
	}
	
	public RadarGLView GetView(){
		return mRadarGLView;
	}


	public void SetControllerHelper(BaseControllerHelper controllerHelper){
		if(mControllerHelper == controllerHelper)
			return;
		
		mRadarGLView.onPause();
		
		if(mControllerHelper != null) {
			mControllerHelper.onPause();
		}
		
		mControllerHelper = controllerHelper;
		mControllerHelper.SetRendererHandler(this);
		mRadarGLRenderer.SetRendererHelper(mControllerHelper.GetRendererHelper());
		
		if(mIsResumed) {
			mRadarGLView.onResume();
		}
	}
	
	public BaseControllerHelper GetContollerHelper(){
		return mControllerHelper;
	} 
		
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(mControllerHelper == null) { return false;}
		return mControllerHelper.onKeyDown(keyCode, event);
	}		
	
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(mControllerHelper == null) { return false;}
		return mControllerHelper.onKeyUp(keyCode, event);
	}
	
	public void onResume(){
		if(mIsResumed) return;
		
		mControllerHelper.onResume();
		mRadarGLView.onResume();
		mIsResumed = true;
	}
	
	public void onPause(){
		if(!mIsResumed) return;
		
		if(mControllerHelper != null) {
			mControllerHelper.onPause();
		}
		if(mRadarGLView != null) {
			mRadarGLView.onPause();
		}
		
		mIsResumed = false;
	}
	
}
