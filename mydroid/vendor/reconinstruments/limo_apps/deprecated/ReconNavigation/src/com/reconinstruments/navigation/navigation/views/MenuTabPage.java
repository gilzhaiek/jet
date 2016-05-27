package com.reconinstruments.navigation.navigation.views;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import com.reconinstruments.navigation.navigation.OverlayManager;

public class MenuTabPage extends TabPage
{
	//the menuview hosted by this TabPage
	private MenuView mMenuView = null;
	public LocalOverlayManager mOverlayManager = null;
	public MenuTabPage( Context context, Drawable iconRegular, Drawable iconSelected, Drawable iconFocused, TabView hostView, MenuView menuView )
	{	
		super( context, iconRegular, iconSelected, iconFocused, hostView );
		mOverlayManager = new LocalOverlayManager( this );
		setMenuView( menuView );
		
	}

	@Override
	public void setFocus( )
	{
		OverlayView overlay = mOverlayManager.getActiveOverlay();
		if( overlay != null )
		{
			overlay.setFocus();
		}
		
	}
	
	private class LocalOverlayManager extends OverlayManager
	{
		public LocalOverlayManager( ViewGroup rootView )
		{
			super(rootView);
		}
		
		/** 
		 * If no more overlay on the page after setOverlayView
		 * ask the TabView to rollback
		 */
		@Override
		public void setOverlayView( OverlayView overlay )
		{			
			super.setOverlayView(overlay);
			if( this.getActiveOverlay() == null )
			{
				MenuTabPage.this.mHostView.mOverlayManager.rollBack();				
			}		
			else
			{
				//set the overlay's event throwback to the tab-page itself
				overlay.mThrowBack = MenuTabPage.this;
				
				//if the TabPage currently hold the focus
				//pass the focus down to the newly added Overlay
				if( MenuTabPage.this.hasFocus() )
				{
					overlay.setFocus();
				}
			}
			
		}
	
		/** 
		 * If no more overlay on the page after roll back
		 * ask the TabView to rollback
		 */
		@Override
		public void rollBack( )
		{
			super.rollBack();
			if( this.getActiveOverlay() == null )
			{
				MenuTabPage.this.mHostView.mOverlayManager.rollBack();
			}
		}
		
		@Override
		public void addOverlayView( OverlayView overlay )
		{
			super.addOverlayView(overlay);
			if( this.getActiveOverlay() != null )
			{				
				//set the overlay's event throwback to the tab-page itself
				overlay.mThrowBack = MenuTabPage.this;
				
				//if the TabPage currently hold the focus
				//pass the focus down to the newly added Overlay
				if( MenuTabPage.this.hasFocus() )
				{
					overlay.setFocus();
				}

			}		
			
		}
	
	}
	
	public void setMenuView( MenuView menuView )
	{
		if( this.getChildCount() > 0 )
		{
			this.removeAllViews();
		}
		
		mMenuView = menuView;
		
		//set the menuView's overlay manager
		mMenuView.mOverlayManager = this.mOverlayManager;
		
		//the throw back event handler should set to the TabPage
		//for processing event such as Left Right key
		mMenuView.mThrowBack = this;
		
		//add the menuView to the local overlay manager
		if( mMenuView != null )
		{
			mOverlayManager.addOverlayView( mMenuView );
		}
	}
}