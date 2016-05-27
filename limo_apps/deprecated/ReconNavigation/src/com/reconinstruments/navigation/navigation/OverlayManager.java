/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */
package com.reconinstruments.navigation.navigation;

import java.util.ArrayList;

import android.view.View;
import android.view.ViewGroup;

import com.reconinstruments.navigation.navigation.views.OverlayView;

public class OverlayManager implements IOverlayManager
{
	//the overlayview stack keep the currently overlay chain, the last OverlayView in the list is the currently active one
	private ArrayList<OverlayView> mOverlayStack = null;		
	
	//the ViewGroup that will contain the overlayView
	private ViewGroup mRootView = null;			
	
	public OverlayManager( ViewGroup rootView )
	{
		mRootView = rootView;
		mOverlayStack = new ArrayList<OverlayView>( 8 );
	}
	
	@Override
	public OverlayView getActiveOverlay()
	{
		if( mOverlayStack != null && mOverlayStack.size() > 0 )
		{
			return mOverlayStack.get( mOverlayStack.size() - 1);
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Added an overlay view to the stack, it turns to the active one
	 * and the stack will be cleared before push the overlay 
	 */
	@Override
	public void setOverlayView(OverlayView overlay) 
	{		
		OverlayView activeOverlay = getActiveOverlay();
		if( activeOverlay != overlay )
		{
			if( activeOverlay != null )
			{
				//about to hide this active overlay
				//let the view know about and do some clean up if necessary
				activeOverlay.onPreHide();
				
				activeOverlay.setVisibility(View.GONE);
			
				mRootView.removeView(activeOverlay);
			}

							
			//clear all overlay from the OverlayStack
			mOverlayStack.clear();
			
			if( overlay != null )
			{									
				mRootView.addView(overlay);
							
				//about to show this view now, let it be 
				//awared of this, and do some preprocessing
				//if necessary
				overlay.onPreShow();
				overlay.setVisibility(View.VISIBLE);				
				overlay.requestFocus();

				mOverlayStack.add(overlay);
			}
		}			
	}
	
	@Override 
	public void rollBack()
	{
		//remove the currently active overlay
		OverlayView activeOverlay = getActiveOverlay();	
		if( activeOverlay != null )
		{
			//about to hide this active overlay
			//let the view know about and do some clean up if necessary
			activeOverlay.onPreHide();
			
			activeOverlay.setVisibility(View.GONE);
			activeOverlay.clearFocus();
		
			mRootView.removeView(activeOverlay);
			
			mOverlayStack.remove(mOverlayStack.size() - 1);
			
		}
		
		//active the last overlay in the stack if there is one available
		activeOverlay = getActiveOverlay();	
		if( activeOverlay != null )
		{
			mRootView.addView(activeOverlay);
			
			//about to show this view now, let it be 
			//awared of this, and do some preprocessing
			//if necessary
			//
			activeOverlay.onPreShow();				
			activeOverlay.setVisibility(View.VISIBLE);				
			activeOverlay.requestFocus();
		}
	}
	
	/**
	 * Added an overlay view to the stack, it turns to the active one
	 * 
	 */
	@Override
	public void addOverlayView( OverlayView overlay )
	{
		OverlayView activeOverlay = getActiveOverlay();
		if( activeOverlay != overlay )
		{
			if( activeOverlay != null )
			{
				
				//about to hide this active overlay
				//let the view know about and do some clean up if necessary
				activeOverlay.onPreHide();
				
				activeOverlay.setVisibility(View.GONE);
			
				mRootView.removeView(activeOverlay);
				
			}
								
			
			if( overlay != null )
			{									
				mRootView.addView(overlay);
							
				//about to show this view now, let it be 
				//awared of this, and do some preprocessing
				//if necessarye
				overlay.onPreShow();
				overlay.setVisibility(View.VISIBLE);				
				overlay.requestFocus();

				mOverlayStack.add(overlay);
			}
		}			

	}
	
}