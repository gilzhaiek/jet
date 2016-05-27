/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.views;

/**
 * This class defined the base for a menu item shown in the MenuView
 */
public class MenuViewItem extends Object
{
	protected String mTitle;
	protected int mID;
	protected MenuView mParentView;
	protected Object mDataObject; 
	
	public MenuViewItem( String title, int id, MenuView parentView )
	{
		mTitle = title;
		mID = id;
		mParentView = parentView;
	}

	public MenuViewItem( String title, int id, MenuView parentView , Object dataObject)
	{
		mTitle = title;
		mID = id;
		mParentView = parentView;
		mDataObject = dataObject;
	}
		
	public void SetParentView(MenuView parentView)
	{
		mParentView = parentView;
	}
	
	@Override 
	public String toString()
	{
		return mTitle;
	}
	
	public int getID()
	{
		return mID;
	}
	
	public Object getDataObject()
	{
		return mDataObject;
	}

}