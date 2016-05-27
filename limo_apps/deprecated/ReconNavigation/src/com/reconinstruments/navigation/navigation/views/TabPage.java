package com.reconinstruments.navigation.navigation.views;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.FrameLayout;

public class TabPage extends FrameLayout implements IRemoteControlEventHandler
{
	
	private Drawable mIconRegular = null;
	private Drawable mIconSelected = null;
	private Drawable mIconFocused = null;
	public StateListDrawable mRegularIconSelector = null;
	public StateListDrawable mFocusedIconSelector = null;
	public StateListDrawable mSelectedIconSelector = null;
	
	protected TabView mHostView = null;	
	
	public TabPage( Context context, Drawable iconRegular, Drawable iconSelected, Drawable iconFocused, TabView hostView )
	{	
		super( context );		
		mIconRegular = iconRegular;
		mIconSelected = iconSelected;
		mIconFocused = iconFocused;
		mHostView = hostView;  	
		
		StateListDrawable states = new StateListDrawable();		
		states.addState(new int[] {android.R.attr.state_selected}, mIconFocused);		
		states.addState(new int[] {}, mIconRegular );
		mRegularIconSelector = states;

		states = new StateListDrawable();
		states.addState(new int[] {android.R.attr.state_selected}, mIconSelected);
		states.addState(new int[] { }, mIconFocused );
		mFocusedIconSelector = states;
				
		states = new StateListDrawable();
		states.addState(new int[] {android.R.attr.state_selected}, mIconFocused);
		states.addState(new int[] { }, mIconSelected );
		mSelectedIconSelector = states;	
	}

	public Drawable getIconRegular( )
	{
		return mIconRegular;
	}

	public Drawable getIconSelected( )
	{
		return mIconSelected;
	}

	public Drawable getIconFocused( )
	{
		return mIconFocused;
	}

	//set the focus to this tab page
	public void setFocus( )
	{
		this.requestFocus();
	}
	
	@Override
	public boolean onDownArrowDown(View srcView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onDownArrowUp(View srcView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUpArrowDown(View srcView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUpArrowUp(View srcView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onLeftArrowDown(View srcView) 
	{
		if( mHostView != null )
		{
			mHostView.focusTabBar();
		}

		return true;
	}

	@Override
	public boolean onLeftArrowUp(View srcView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onRightArrowDown(View srcView) 
	{
		return false;
	}

	@Override
	public boolean onRightArrowUp(View srcView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onSelectDown(View srcView) {
		if( mHostView != null )
		{
			mHostView.focusTabBar();
		}
		return true;
	}

	@Override
	public boolean onSelectUp(View srcView) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onBackDown(View srcView) 
	{
		//call the TabVew onBackDown 
		if( mHostView != null )
		{
			if(mHostView.tabListHasFocus())
			{
				mHostView.focusTabBar();
			}
			else
			{
				mHostView.onBackDown(srcView);
			}
		}
		
		return true;
	}

	@Override
	public boolean onBackUp(View srcView) {
		// TODO Auto-generated method stub
		return false;
	}
}