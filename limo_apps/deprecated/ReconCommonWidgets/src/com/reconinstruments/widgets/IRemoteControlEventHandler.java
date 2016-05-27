/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.widgets;

import android.view.View;

/**
 * defined the interface for handling remote control event
 */

public interface IRemoteControlEventHandler
{	
	boolean onDownArrowDown( View srcView );	//the srcView is where the key originaly captured
	boolean onDownArrowUp( View srcView );
	boolean onUpArrowDown( View srcView );
	boolean onUpArrowUp( View srcView );
	boolean onLeftArrowDown( View srcView );
	boolean onLeftArrowUp( View srcView );
	boolean onRightArrowDown( View srcView );
	boolean onRightArrowUp( View srcView );
	boolean onSelectDown( View srcView );
	boolean onSelectUp( View srcView );
	boolean onBackDown( View srcView );
	boolean onBackUp( View srcView );
}
