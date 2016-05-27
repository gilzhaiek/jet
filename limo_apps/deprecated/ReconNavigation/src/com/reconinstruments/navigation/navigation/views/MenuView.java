/**
 *Copyright 2011 Recon Instruments
 *All Rights Reserved.
 */

package com.reconinstruments.navigation.navigation.views;


import java.util.ArrayList;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.IOverlayManager;
import com.reconinstruments.navigation.navigation.Util;
import com.reconinstruments.navigation.navigation.menuHandler.MenuHandler;

/**
 *The view for menu selection
 */

public class MenuView extends OverlayView
{		
	public ListView mMenuList = null;
	protected TextView mTitleLabel = null;
	ArrayList<Object> mMenuItems = null;	
	public MenuHandler mMenuHandler = null;
	IIconProvider mIconProvider = null;
	
	/**
	 * 
	 * The interface for supplying the MenuView
	 * with icon for an MenuItem.
	 *
	 */
	public interface IIconProvider
	{
		//given the menu item position, and the ImageView for that item
		//set its icon resource. If nothing set, then there is no
		//icon displayed for this menu item
		public void setIcon( ImageView imageView, int position );		
	};
	/*
	 * private class for defining an ArrayAdapter that has it own view of list item
	 * 
	 */
	private class MenuViewAdapter extends ArrayAdapter<Object>
	{
		private ArrayList<Object> mMenuItems;
		
		public MenuViewAdapter( Context context, int textViewResourceId, ArrayList<Object> menuItems)
		{
			super( context, textViewResourceId, menuItems );
			mMenuItems = menuItems;
		}
		
		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			View v = convertView;
			if( v == null )
			{
				//create a new view from the poiCategoryitem_layout
				LayoutInflater inflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.menuview_item_layout, null);				
				TextView title = (TextView)v.findViewById(R.id.menuview_item_text);
				title.setTypeface(Util.getMenuFont(this.getContext()));
			}
			
			if( mIconProvider != null )
			{
				ImageView icon = (ImageView)v.findViewById(R.id.menuview_item_icon);
				mIconProvider.setIcon( icon, position );
			}
				
			
			TextView title = (TextView)v.findViewById(R.id.menuview_item_text);
			title.setText(mMenuItems.get(position).toString());
		
			return v;
			
		}
		
	}

	
	public MenuView( Context context, IOverlayManager overlayManager, IRemoteControlEventHandler throwBack, String title, MenuHandler menuHandler )
	{
		super( context, overlayManager, throwBack );
		mThrowBack = throwBack;
		mMenuHandler = menuHandler;
		

		//inflate a view from predefined xml file
        LayoutInflater factory = LayoutInflater.from(context);
        View navView = factory.inflate(R.layout.menu_view, null);
        this.addView(navView);
        
        mMenuList = (ListView)navView.findViewById(R.id.menu_list);
        mMenuList.setOnItemClickListener(mItemClickListener);      
        mMenuList.setOnItemSelectedListener(mItemSelectedListener);
        
        mMenuList.setItemsCanFocus(true);
        mMenuList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mMenuList.setOnKeyListener(mListViewKeyListener);
        
        //the code to remove the divider of the listview
        mMenuList.setDivider(null);
        mMenuList.setDividerHeight(0);
       
        mTitleLabel = (TextView)navView.findViewById(R.id.menu_view_title);
        mTitleLabel.setTypeface(Util.getMenuFont(context));
        mTitleLabel.setTextColor(Util.BLUE_MENU_COLOR);
        
        if( title != null )
        {
        	mTitleLabel.setText(title);
        }
	}	
	
	
	@Override
	public void setFocus( )
	{
		mMenuList.requestFocus();
	}
	
	public void setMenuItems( ArrayList items )
	{
        //ArrayAdapter<Object> menItems = new ArrayAdapter<Object>(this.getContext(), R.layout.limo_default_list_item, items);        
		MenuViewAdapter menItems = new MenuViewAdapter(this.getContext(), R.layout.menuview_item_layout, items);
        mMenuList.setAdapter(menItems);
        mMenuItems = items;
	}
	
	public void setIconProvider( IIconProvider iconProvider )
	{
		mIconProvider = iconProvider;
	}
	
    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener( )
    {
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    	{
    		//Hide the menu view, then handle the menu-item selection action
    		//mOverlayManager.setOverlayView(null);
    		
    		Object item = mMenuItems.get(position);   
    		
    		if( mMenuHandler != null )
    		{
    			mMenuHandler.OnMenuItemSelected(item);
    		}
    	}
    };
    
    private AdapterView.OnItemSelectedListener mItemSelectedListener = new AdapterView.OnItemSelectedListener()
    {

		@Override
		public void onItemSelected(AdapterView parent, View view, int position, long id)
		{
			
			//force to set the focus to the list so
			//that the first item can be highlighted when
			//the menuview is showing
			if( parent == mMenuList )
			{
				//mMenuList.requestFocus();				
			}
		}

		@Override
		public void onNothingSelected(AdapterView parent) 
		{
			
			
		}
    	
	};
	
    private View.OnKeyListener mListViewKeyListener = new View.OnKeyListener() {
		
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			// TODO Auto-generated method stub
			//intercept the back key from the listView 
			// and translate it to cancel the menu view
			if( keyCode == KeyEvent.KEYCODE_BACK && MenuView.this.mThrowBack != null  )
			{
				if( event.getAction() == KeyEvent.ACTION_DOWN  )
				{
					MenuView.this.mThrowBack.onBackDown(MenuView.this);
				}
				else
				{
					MenuView.this.mThrowBack.onBackUp(MenuView.this);
				}
				return true;
			}
			else if( keyCode == KeyEvent.KEYCODE_BACK )
			{
				if( event.getAction() == KeyEvent.ACTION_DOWN  )
				{
					MenuView.this.onBackDown(MenuView.this);
				}
				else
				{
					MenuView.this.onBackUp(MenuView.this);
				}
				return true;
			} 
			else if( keyCode == KeyEvent.KEYCODE_DPAD_LEFT && MenuView.this.mThrowBack != null )
			{
				if( event.getAction() == KeyEvent.ACTION_DOWN )
				{
					MenuView.this.mThrowBack.onLeftArrowDown(MenuView.this);
				}
				else
				{
					MenuView.this.mThrowBack.onLeftArrowUp(MenuView.this);
				}
				return true;
				
			}
			else if( keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && MenuView.this.mThrowBack != null )
			{
				if( event.getAction() == KeyEvent.ACTION_DOWN )
				{
					MenuView.this.mThrowBack.onRightArrowDown(MenuView.this);
				}
				else
				{
					MenuView.this.mThrowBack.onRightArrowUp(MenuView.this);
				}
				return true;
				
			}
			return false;
		}
	};
	
    @Override
	public boolean onDownArrowDown(  View srcView )
	{
    	//this.setFocusable(false);
        mMenuList.setFocusable(true);
        mMenuList.requestFocus();
		return true;
	}
    
    @Override
	public boolean onUpArrowDown(  View srcView )
	{
    	//this.setFocusable(false);
        mMenuList.setFocusable(true);
        mMenuList.requestFocus();
		return true;
	}    	

    @Override
    public boolean onBackUp( View srcView )
    {
    	//since we hide the active overlay within onBackDown
    	//and activated the rolled-back view
    	//this is the view that been activated now
    	//so force to set the focus
    	//otherwise, the mMenuList wont get the 
    	//focus when we back to the previous level of menu
    	if( this.getVisibility() == View.VISIBLE )
    	{
	        mMenuList.setFocusable(true);
	        mMenuList.requestFocus();
    	}
		return true;
    	
    }
}
