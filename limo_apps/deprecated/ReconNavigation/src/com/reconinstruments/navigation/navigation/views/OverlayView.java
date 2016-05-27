/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.views;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.reconinstruments.navigation.navigation.IOverlayManager;

/**
 *This class define the base class of the view
 *that will be shown on top of the MapView for
 *responds to different user inputs from the remote control
 */
public class OverlayView extends FrameLayout implements IRemoteControlEventHandler
{
	public IRemoteControlEventHandler mThrowBack;				//process the event that wont be processed by this view	
	protected View mLauncher = null;				//the View that launch this view. If null, the view is launched from the root
	protected IOverlayManager mOverlayManager = null;

	public OverlayView( Context context, IOverlayManager overlayManager,  IRemoteControlEventHandler throwBack )
	{
		super( context );
		
		mOverlayManager = overlayManager;
		
		mThrowBack = throwBack;
		
		//set as focusable so that key-event can be captured
		this.setFocusable(true);
	}

	
	/**
	 * 
	 * @param launcher: defined the view who launched this OverlayView.
	 * 
	 */	
	public OverlayView( Context context, View launcher, IOverlayManager overlayManager )
	{
		super( context );
		mLauncher = launcher;
		
		//set as focusable for capturing key-event
		this.setFocusable(true);		
		
		mOverlayManager = overlayManager;
	}	
	 
	
	/**
	 * Force focus to be set to this overlay, can be override by dervide class
	 */
	public void setFocus( )
	{
		requestFocus( );
	}
	/**
	 * Handle the key-down event from the remote control
	 */
	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event )
	{
		switch( keyCode )
		{
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return onDownArrowDown( this );
				
			case KeyEvent.KEYCODE_DPAD_UP:
				return onUpArrowDown( this );
				
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return onLeftArrowDown( this );
			
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return onRightArrowDown( this );
				
			case KeyEvent.KEYCODE_DPAD_CENTER:
				return onSelectDown( this );
				
			case KeyEvent.KEYCODE_BACK:
				return onBackDown( this );
				
			//all the other buttons, just ignore it
			default:
				return false;
		}
	}
	
	/**
	 *Handle the key-up event from the remote control 
	 */
	public boolean onKeyUp( int keyCode, KeyEvent event )
	{
		switch( keyCode )
		{
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return onDownArrowUp( this );
				
			case KeyEvent.KEYCODE_DPAD_UP:
				return onUpArrowUp( this );
				
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return onLeftArrowUp( this );
			
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return onRightArrowUp( this );
				
			case KeyEvent.KEYCODE_DPAD_CENTER:
				return onSelectUp( this );
				
			case KeyEvent.KEYCODE_BACK:
				return onBackUp( this );
				
			//all the other buttons, just ignore it
			default:
				return false;
		}
	}
		
	/**
	 * Down arrow is down. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onDownArrowDown(  View srcView )
	{
		return true;
	}
	
	/**
	 * Down arrow is up. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onDownArrowUp( View srcView  )
	{
		return true;
	}

	/**
	 * Up arrow is down. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onUpArrowDown( View srcView  )
	{
		return true;
	}
	
	/**
	 * Up arrow is up. The derived views should implement
	 * this if they want to handle the up-arrow key event
	 */
	public boolean onUpArrowUp( View srcView  )
	{
		return true;
	}

	/**
	 * Left arrow is down. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onLeftArrowDown(  View srcView )
	{
		return true;
	}
	
	/**
	 * Left arrow is up. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onLeftArrowUp(  View srcView )
	{
		return true;
	}

	/**
	 * Right arrow is down. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onRightArrowDown(  View srcView )
	{
		return true;
	}
	
	/**
	 * Right arrow is up. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onRightArrowUp(  View srcView )
	{
		return true;
	}

	/**
	 * Select button is down. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onSelectDown(  View srcView )
	{
		return true;
	}
	
	/**
	 * Select button is up. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onSelectUp(  View srcView )
	{
		return true;
	}

	
	/**
	 * back button is down. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onBackDown(  View srcView )
	{
		//hide the overlay view
		//this.setVisibility(View.GONE)
		
		if( mOverlayManager != null )
		{
			mOverlayManager.rollBack();
		}
		return true;
	}
	
	/**
	 * Back button is up. The derived views should implement
	 * this if they want to handle the down-arrow key event
	 */
	public boolean onBackUp(  View srcView )
	{
		return true;
	}

	/**
	 * About to set the overlay view active. do something pre-processing
	 * if want
	 */
	public void onPreShow()
	{
		
	}
	

	/**
	 * About to hide the overlay view now, do some post-processing if necessary
	 */
	public void onPreHide()
	{
		
	}

}
