/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.menuHandler;

import android.content.Context;

import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.views.IRemoteControlEventHandler;

/**
 * 
 * define the base class for handling menu item
 *
 */
public abstract class MenuHandler
{
	protected Context mContext = null;
	protected IOverlayManager mOverlayManager = null;
	protected IRemoteControlEventHandler mDefaultRemoteEventHandler = null;
	public MenuHandler( Context context, IOverlayManager overlayManager, IRemoteControlEventHandler defaultEventHandler )
	{
		mContext = context;
		mOverlayManager = overlayManager;
		mDefaultRemoteEventHandler = defaultEventHandler;
		
	}
	
	public void setOverlayManager( IOverlayManager overlayManager )
	{
		mOverlayManager = overlayManager;
	}
	
	/*
	 * Override by the derived class for handling an menuItem object is selected
	 */
	abstract public void OnMenuItemSelected( Object menuItem );

}
