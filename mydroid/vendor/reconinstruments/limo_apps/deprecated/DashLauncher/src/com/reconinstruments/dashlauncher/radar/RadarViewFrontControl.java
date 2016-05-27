package com.reconinstruments.dashlauncher.radar;

//import com.reconinstruments.dashlauncher.radar.render.GLDotField;
import com.reconinstruments.dashlauncher.radar.render.GLUserArrow;
import com.reconinstruments.dashlauncher.radar.render.GLCompass;
import com.reconinstruments.dashlauncher.radar.render.GLFrontTexture;
import com.reconinstruments.dashlauncher.radar.render.GLMapRenderer;
import com.reconinstruments.dashlauncher.radar.render.GLZoomControl;

import android.os.AsyncTask;
import android.util.Log;

public class RadarViewFrontControl {
	private static final String TAG = "RadarViewFrontControl";
	
	private final static int	ANIM_ZOOM_HIDE_TIME_MS		= 1500;
	private final static int	ANIM_ZOOM_SHOW_TIME_MS		= 300;
	private final static int	ANIM_ZOOM_SHOW_TIMER		= 3000;
	private final static int	ANIM_TRANSITION_TIME_MS		= 800;
	private final static int	ANIM_TRANSITION_DTIME_MS	= 150;
	
	// Texture Info
	private final static float	TOP_MASK_HEIGHT_PX			= 25.0f;
	private final static float	BOTTOM_MASK_HEIGHT_PX		= 7.0f;
	
	public enum EControlEvents {
		eEventRadarMapLoaded,
		eEventCompassToDotMap,		eEventDotMapToCompass,
		eEventCompassToRadarMap,	eEventRadarMapToCompass,
		eEventRadarMapToFullMap,	eEventFullMapToRadarMap,
		eEventCompassToFullMap,		eEventFullMapToCompass,
		eEventShowZoomControl,
	}
	
	protected static RadarViewControllerHelper	mRadarViewControllerHelper	= null;
	protected static RadarViewRendererHelper	mRadarViewRendererHelper	= null;
	protected static RendererHandler			mRendererHandler			= null;
	
	// Animation Classes
	private static AnimateFrontMaskTask 	mAnimateFrontMaskTask		= null;
	private static ShowZoomControlTask		mShowZoomControlTask		= null;
	
	public RadarViewFrontControl(
			RadarViewControllerHelper radarViewControllerHelper,
			RadarViewRendererHelper radarViewRendererHelper) {
		
		mRadarViewControllerHelper = radarViewControllerHelper;
		mRadarViewRendererHelper = radarViewRendererHelper; 
	}
	
	public void SetRendererHandler(RendererHandler rendererHandler){
		mRendererHandler = rendererHandler;
	}
	
	public void ResetToRadarModeControls(boolean mapsLoaded, boolean compassMode){
		GLFrontTexture bottomMaskTexture	= mRadarViewRendererHelper.GetBottomMaskTexture();
		GLFrontTexture topMaskTexture		= mRadarViewRendererHelper.GetTopMaskTexture();
		GLFrontTexture discMaskCompass		= mRadarViewRendererHelper.GetDiscMaskCompassTexture();
		GLFrontTexture discMaskBuddyRadar	= mRadarViewRendererHelper.GetDiscMaskBuddyTexture();
		GLCompass compassFullTexture		= mRadarViewRendererHelper.GetCompassFullTexture();
		GLCompass compassBuddyModeTexture	= mRadarViewRendererHelper.GetCompassBuddyModeTexture();
		GLMapRenderer mapRenderer			= mRadarViewRendererHelper.GetGLMapRenderer();
		OOIController ooiController			= mRadarViewRendererHelper.getOOIController();
		GLUserArrow	userArrow				= mRadarViewRendererHelper.getUserArrow();
		GLFrontTexture focusRect			= mRadarViewRendererHelper.GetFocusRect();
		
		if(mapsLoaded) {
			bottomMaskTexture.SetHidden(false);
			topMaskTexture.SetHidden(false);
			bottomMaskTexture.SetOffsets(0.0f, 0.0f, 0.0f);
			topMaskTexture.SetOffsets(0.0f, 0.0f, 0.0f);			
			
			compassBuddyModeTexture.SetAlpha(0.0f);
			compassBuddyModeTexture.SetHidden(true); 

			if(compassMode) {
				mapRenderer.SetAlpha(0.0f);
				userArrow.SetAlpha(0.0f);
			} else {  // Buddy Mode
				mapRenderer.SetAlpha(1.0f);
				userArrow.SetAlpha(1.0f);
			}
		} else {  // No Maps
			bottomMaskTexture.SetOffsets(0.0f, -1.0f*BOTTOM_MASK_HEIGHT_PX, 0.0f);
			topMaskTexture.SetOffsets(0.0f, TOP_MASK_HEIGHT_PX, 0.0f);
			bottomMaskTexture.SetHidden(true);
			topMaskTexture.SetHidden(true);
			
			mapRenderer.SetAlpha(0.0f);		
			
			if(compassMode) {
				userArrow.SetAlpha(0.0f);
				compassBuddyModeTexture.SetAlpha(0.0f);
				compassBuddyModeTexture.SetHidden(true); 				
			} else {  // Buddy Mode
				userArrow.SetAlpha(1.0f);
				compassBuddyModeTexture.SetAlpha(1.0f);
				compassBuddyModeTexture.SetHidden(false);
			}
		}
		
		if(compassMode){
			compassFullTexture.SetHidden(false); 
			compassFullTexture.SetAlpha(1.0f);
			discMaskCompass.SetAlpha(1.0f);
			discMaskCompass.SetHidden(false);
			ooiController.SetAlpha(0.0f);
			ooiController.SetBuddiesEnabled(false);
			discMaskBuddyRadar.SetHidden(true);
			discMaskBuddyRadar.SetAlpha(0.0f);
		} else { // Show Buddy Radar
			compassFullTexture.SetHidden(true);
			compassFullTexture.SetAlpha(0.0f);
			discMaskCompass.SetAlpha(0.0f);
			discMaskCompass.SetHidden(true);
			ooiController.SetAlpha(1.0f);
			ooiController.SetBuddiesEnabled(true);			
			discMaskBuddyRadar.SetHidden(false);
			discMaskBuddyRadar.SetAlpha(1.0f);
		}
		
		ooiController.SetRestEnabled(false);
		ooiController.SetLiftsEnabled(false);
		ooiController.SetRadarMode(true);
		
		focusRect.SetAlpha(0.0f);
		focusRect.SetHidden(true);
	}
	
	public void AnimateFrontMask(EControlEvents controlEvent) {
		if((mRendererHandler == null) || (mRadarViewRendererHelper == null)) {
			return;
		}
		
		if(mAnimateFrontMaskTask != null) {
			mAnimateFrontMaskTask.cancel(true);
		}
		
		mAnimateFrontMaskTask = new AnimateFrontMaskTask();
		mAnimateFrontMaskTask.execute(controlEvent);
	}
	
	
	public void HideZoomControlNow() {
		if((mRendererHandler == null) || (mRadarViewRendererHelper == null)) {
			return;
		}
		
		if(mShowZoomControlTask != null) {
			mShowZoomControlTask.cancel(true);
		}
		
		mRadarViewRendererHelper.GetZoomControlTexture().HideNow();
		mRadarViewRendererHelper.GetZoomControlTexture().SetHidden(true);
	}
	
	public void ShowZoomControl() {
		if((mRendererHandler == null) || (mRadarViewRendererHelper == null)) {
			return;
		}
		
		if(mShowZoomControlTask != null) {
			//Log.v(TAG,"mShowZoomControlTask.cancel(true);");
			mShowZoomControlTask.cancel(true);
		}
		
		mShowZoomControlTask = new ShowZoomControlTask();
		mShowZoomControlTask.execute((Void[])null);			
	}
	
	protected class AnimateFrontMaskTask extends AsyncTask<EControlEvents, Integer, EControlEvents> {

		@Override
		protected EControlEvents doInBackground(EControlEvents... params) {
			EControlEvents targetControl = params[0];
			
			GLFrontTexture bottomMaskTexture	= mRadarViewRendererHelper.GetBottomMaskTexture();
			GLFrontTexture topMaskTexture		= mRadarViewRendererHelper.GetTopMaskTexture();
			GLFrontTexture discMaskCompass		= mRadarViewRendererHelper.GetDiscMaskCompassTexture();
			GLFrontTexture discMaskBuddyRadar	= mRadarViewRendererHelper.GetDiscMaskBuddyTexture();
			GLCompass compassFullTexture		= mRadarViewRendererHelper.GetCompassFullTexture();
			GLCompass compassBuddyModeTexture	= mRadarViewRendererHelper.GetCompassBuddyModeTexture();
			GLMapRenderer mapRenderer			= mRadarViewRendererHelper.GetGLMapRenderer();
			OOIController ooiController			= mRadarViewRendererHelper.getOOIController();
			GLUserArrow	userArrow				= mRadarViewRendererHelper.getUserArrow();
			GLFrontTexture focusRect			= mRadarViewRendererHelper.GetFocusRect();
			
			Log.v(TAG,"targetControl="+targetControl);
			
			boolean enterFullMap	= (targetControl == EControlEvents.eEventRadarMapToFullMap) || (targetControl == EControlEvents.eEventCompassToFullMap);
			boolean showCompass		= (targetControl == EControlEvents.eEventRadarMapToCompass) || (targetControl == EControlEvents.eEventFullMapToCompass) || (targetControl == EControlEvents.eEventDotMapToCompass);
			boolean hasMap			= (targetControl != EControlEvents.eEventCompassToDotMap) && (targetControl != EControlEvents.eEventDotMapToCompass);
			
			float yBottom				= bottomMaskTexture.GetYOffset();
			float yTop					= topMaskTexture.GetYOffset();
			float yBottomStep			= 0.0f;
			float yTopStep				= 0.0f;
			
			float discBuddyOpacity		= discMaskBuddyRadar.GetAlpha();
			float compassOpacity		= compassFullTexture.GetAlpha();
			float discBuddyOpacityStep	= 0.0f;
			float compassOpacityStep	= 0.0f;

			if(enterFullMap) {
				yBottomStep				= -1.0f*(BOTTOM_MASK_HEIGHT_PX+yBottom)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS);
				yTopStep				= (TOP_MASK_HEIGHT_PX+yTop)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS);
				discBuddyOpacityStep 	= -1.0f*(discBuddyOpacity)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS);
				compassOpacityStep 		= -1.0f*(compassOpacity)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS);
				
				ooiController.SetRadarMode(false);
				ooiController.SetRestEnabled(true);
				ooiController.SetLiftsEnabled(true);
				ooiController.SetBuddiesEnabled(true);
				focusRect.SetHidden(false);
			} 
			else // Enter Compass or Buddy Map
			{
				yBottomStep		= (Math.abs(yBottom)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS));
				yTopStep		= -1.0f*(Math.abs(yTop)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS));
				
				bottomMaskTexture.SetHidden(false);
				topMaskTexture.SetHidden(false);
				if(showCompass) {
					compassOpacityStep = (1.0f-compassOpacity)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS);
					compassFullTexture.SetHidden(false);
					ooiController.SetBuddiesEnabled(false);
					discMaskCompass.SetHidden(false);
					discBuddyOpacityStep 	= -1.0f*(discBuddyOpacity)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS);
				} else {
					if(!hasMap) {
						compassBuddyModeTexture.SetHidden(false);
					} else {
						compassBuddyModeTexture.SetHidden(true); // No Gradient Change when maps is loaded
					}
					
					compassOpacityStep = -1.0f*(compassOpacity)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS);
					ooiController.SetBuddiesEnabled(true);
					discMaskBuddyRadar.SetHidden(false);
					discBuddyOpacityStep = (1.0f-discBuddyOpacity)/((float)ANIM_TRANSITION_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS);
				}
				
				ooiController.SetRestEnabled(false);
				ooiController.SetLiftsEnabled(false); 
				ooiController.SetRadarMode(true);
			}
			
			for(int totalSleep = ANIM_TRANSITION_DTIME_MS; totalSleep < ANIM_TRANSITION_TIME_MS; totalSleep +=ANIM_TRANSITION_DTIME_MS)
			{
				yBottom				+= yBottomStep;
				yTop				+= yTopStep;
				discBuddyOpacity	+= discBuddyOpacityStep;
				compassOpacity		+= compassOpacityStep;
				
				//Log.v(TAG,"Radar yTop="+yTop+" yBottom="+yBottom);
				
				if(hasMap) {
					bottomMaskTexture.SetOffsets(0.0f, yBottom, 0.0f);
					topMaskTexture.SetOffsets(0.0f, yTop, 0.0f);
					mapRenderer.SetAlpha(1.0f-compassOpacity);
					userArrow.SetAlpha(1.0f - compassOpacity);
				}
				else {
					userArrow.SetAlpha(1.0f - compassOpacity);
					compassBuddyModeTexture.SetAlpha(1.0f-compassOpacity);
				}
				
				discMaskBuddyRadar.SetAlpha(discBuddyOpacity);
				discMaskCompass.SetAlpha(compassOpacity);
				compassFullTexture.SetAlpha(compassOpacity);
				ooiController.SetAlpha(1.0f - compassOpacity);
				focusRect.SetAlpha(1.0f - compassOpacity);
				
				mRendererHandler.RedrawScene();
				try { Thread.sleep(ANIM_TRANSITION_DTIME_MS); } catch (InterruptedException e) {}
				if(isCancelled()) return null; 
			}
			
			if(enterFullMap) {
				bottomMaskTexture.SetOffsets(0.0f, -1.0f*BOTTOM_MASK_HEIGHT_PX, 0.0f);
				topMaskTexture.SetOffsets(0.0f, TOP_MASK_HEIGHT_PX, 0.0f);
				discMaskBuddyRadar.SetAlpha(0.0f);
				discMaskCompass.SetAlpha(0.0f);
				
				bottomMaskTexture.SetHidden(true);
				topMaskTexture.SetHidden(true);
				discMaskBuddyRadar.SetHidden(true);
				discMaskCompass.SetHidden(true);
				focusRect.SetAlpha(1.0f);
			} else {
				if(showCompass) {
					discMaskBuddyRadar.SetAlpha(0.0f);
					discMaskBuddyRadar.SetHidden(true);
					mapRenderer.SetAlpha(0.0f);
					discMaskCompass.SetAlpha(1.0f);
					ooiController.SetAlpha(0.0f);
					userArrow.SetAlpha(0.0f);
					compassFullTexture.SetAlpha(1.0f);	
				} else { // Show Buddy Map
					discMaskBuddyRadar.SetAlpha(1.0f);
					ooiController.SetAlpha(1.0f);
					compassFullTexture.SetAlpha(0.0f);
					compassFullTexture.SetHidden(true);
					userArrow.SetAlpha(1.0f);
					
					discMaskCompass.SetAlpha(0.0f);
					discMaskCompass.SetHidden(true);

					if(hasMap) {
						bottomMaskTexture.SetOffsets(0.0f, 0.0f, 0.0f);
						topMaskTexture.SetOffsets(0.0f, 0.0f, 0.0f);
						mapRenderer.SetAlpha(1.0f);
						compassBuddyModeTexture.SetAlpha(0.0f);
					} else {
						compassBuddyModeTexture.SetAlpha(1.0f);
					}
				}
				focusRect.SetAlpha(0.0f);
				focusRect.SetHidden(true);
			}
			
			mRendererHandler.RedrawScene();
			
			return targetControl;
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			mAnimateFrontMaskTask = null;
		}	
				
		@Override
		protected void onPostExecute(EControlEvents controlEvent) {
			mAnimateFrontMaskTask = null;
			mRadarViewControllerHelper.OnControlEvent(controlEvent);
		}		
    }
	
	protected class ShowZoomControlTask extends AsyncTask<Void, Integer, EControlEvents> {

		@Override
		protected EControlEvents doInBackground(Void... params) {
			GLZoomControl zoomControlTexture = mRadarViewRendererHelper.GetZoomControlTexture();
			zoomControlTexture.SetHidden(false);
			
			float xLeft	= zoomControlTexture.GetXOffset();
			
			if(xLeft < 0) { // Hidden
				float incrXLeft = (Math.abs(xLeft)/((float)ANIM_ZOOM_SHOW_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS)); 
				
				for(int totalSleep = ANIM_TRANSITION_DTIME_MS; totalSleep < ANIM_ZOOM_SHOW_TIME_MS; totalSleep +=ANIM_TRANSITION_DTIME_MS)
				{
					xLeft += incrXLeft;
				
					zoomControlTexture.SetOffsets(xLeft, 0.0f, 0.0f);
					
					mRendererHandler.RedrawScene();
					try { Thread.sleep(ANIM_TRANSITION_DTIME_MS); } catch (InterruptedException e) {}
					if(isCancelled()) return null; 
				}
				
				if(isCancelled()) return null;
				xLeft = 0.0f;
				zoomControlTexture.ShowNow();
				mRendererHandler.RedrawScene();
			}
			
			try { Thread.sleep(ANIM_ZOOM_SHOW_TIMER); } catch (InterruptedException e) {} // No Activity
			if(isCancelled()) return null;
			
			float decrXLeft = (Math.abs(GLZoomControl.HIDE_X_OFFSET)/((float)ANIM_ZOOM_HIDE_TIME_MS/(float)ANIM_TRANSITION_DTIME_MS)); 
			
			for(int totalSleep = ANIM_TRANSITION_DTIME_MS; totalSleep < ANIM_ZOOM_HIDE_TIME_MS; totalSleep +=ANIM_TRANSITION_DTIME_MS)
			{
				xLeft -= decrXLeft;
			
				zoomControlTexture.SetOffsets(xLeft, 0.0f, 0.0f);
				
				mRendererHandler.RedrawScene();
				try { Thread.sleep(ANIM_TRANSITION_DTIME_MS); } catch (InterruptedException e) {}
				if(isCancelled()) {Log.v(TAG,"isCancelled");return null;} 
			}
			
			if(isCancelled()) return null;
			zoomControlTexture.HideNow();
			mRendererHandler.RedrawScene();
			
			return EControlEvents.eEventShowZoomControl;
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
		}	
				
		@Override
		protected void onPostExecute(EControlEvents controlEvent) {
			mShowZoomControlTask = null;
			mRadarViewControllerHelper.OnControlEvent(controlEvent);
		}		
    }	
}
