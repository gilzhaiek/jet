/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation;

import com.reconinstruments.navigation.navigation.views.OverlayView;

public interface IOverlayManager
{
	void setOverlayView( OverlayView overlay );  //set the active overlay, this will clear the stack then add the new overlay if it is not null
	OverlayView getActiveOverlay();
	void rollBack();							 //roll back to previous overlay
	void addOverlayView( OverlayView overlay );  //add an overlay view, the present active one will be override, but still kept in the stack
}