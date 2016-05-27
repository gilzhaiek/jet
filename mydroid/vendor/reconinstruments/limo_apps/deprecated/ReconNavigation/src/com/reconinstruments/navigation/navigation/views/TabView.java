
package com.reconinstruments.navigation.navigation.views;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import com.reconinstruments.navigation.R;
import com.reconinstruments.navigation.navigation.IOverlayManager;

public class TabView extends OverlayView
{
	protected ListView mTabList = null;
	protected  FrameLayout mTabPageContainer = null;
	protected  ArrayList<TabPage> mPages = null;
	protected int mSelectedTabIdx = -1;
	
	public TabView( Context context, IOverlayManager overlayManager )
	{	
		super( context, overlayManager, null );
		initView( context );
	}
	
	//return the overlaymanager of the tabview
	public IOverlayManager getOverlayManager()
	{
		return mOverlayManager;
	}
	
	protected void initView( Context context )
	{
		//inflate a view from predefined xml file
        LayoutInflater factory = LayoutInflater.from(context);
        View navView = factory.inflate(R.layout.tab_view, null);
        this.addView(navView);
                      
        mTabList = (ListView)navView.findViewById(R.id.tab_bar);
        mTabPageContainer = (FrameLayout)navView.findViewById(R.id.tab_page_container);
        
        mTabList.setOnItemSelectedListener(mTabSelectedListener);
        mTabList.setOnKeyListener(mTabBarKeyListener);
        mTabList.setOnFocusChangeListener(mTabBarFocusChanged);
        
        mTabList.setDivider(null);
        mTabList.setDividerHeight( 0 );
                
	}
	
	public void focusTabBar( )
	{
		mTabList.requestFocus();
	}
	
	public boolean tabListHasFocus()
	{
		return this.hasFocus();
	}
		
	public void setTabPages( ArrayList<TabPage> pages )
	{
		TabIconAdapter menItems = new TabIconAdapter(this.getContext(), R.layout.tab_icon_view, (ArrayList)pages);
        mTabList.setAdapter(menItems);
		mPages = pages;
	}
	
	/*
	 * private class for defining an ArrayAdapter that has it own view of list item
	 * 
	 */
	protected class TabIconAdapter extends ArrayAdapter<Object>
	{
		
		public TabIconAdapter( Context context, int textViewResourceId, ArrayList<Object> tabPages )
		{
			super( context, textViewResourceId, tabPages );			
		}
		
		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{			
			View v = convertView;
			if( v == null )
			{
				//create a new view from the poiCategoryitem_layout
				LayoutInflater inflater = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.tab_icon_view, null);

				//v.setMinimumHeight(50);
				ImageView icon = (ImageView)v.findViewById(R.id.tab_item_icon);
				icon.setImageDrawable( mPages.get(position).mRegularIconSelector);
			}
			
			return v;
			
		}	
		
	}


	private AdapterView.OnItemSelectedListener mTabSelectedListener = new AdapterView.OnItemSelectedListener() 
	{

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) 
		{
			if( mTabPageContainer.getChildCount() > 0 )
			{
				mTabPageContainer.removeAllViews();
			}
			
			mTabPageContainer.addView( mPages.get(position));	
			mTabList.requestFocus();

			if(mSelectedTabIdx >= 0)
			{
				ImageView icon = (ImageView)mTabList.getChildAt(mSelectedTabIdx).findViewById(R.id.tab_item_icon);
				icon.setImageDrawable( mPages.get(mSelectedTabIdx).mRegularIconSelector );
			}
			
			mSelectedTabIdx = position;			
			
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub
			
		}	
	};
	
	protected  View.OnFocusChangeListener mTabBarFocusChanged = new View.OnFocusChangeListener() {
		
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			// TODO Auto-generated method stub
			
			if( mSelectedTabIdx >= 0 )
			{
				if( hasFocus )
				{
				}
				else
				{
				}
			}
	
		}
	};

	
	protected View.OnKeyListener mTabBarKeyListener = new View.OnKeyListener() {
		
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			// TODO Auto-generated method stub
			if( keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_CENTER  )
			{
				if( event.getAction() == KeyEvent.ACTION_DOWN)
				{
					if( mSelectedTabIdx >= 0 )
					{
						//focus switch to Tab Page now, let's set the selected tab background
						//to be a bit dim then the selector
						//mTabList.getChildAt(mSelectedTabIdx).setBackgroundColor(0xff008799);
						ImageView icon = (ImageView)mTabList.getChildAt(mSelectedTabIdx).findViewById(R.id.tab_item_icon);
						icon.setImageDrawable( mPages.get(mSelectedTabIdx).mSelectedIconSelector );
						mPages.get(mSelectedTabIdx).setFocus();
					}

				}
				return true;
			}
			/*else if( keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_BACK )
			{
				if( event.getAction() == KeyEvent.ACTION_DOWN)
				{
					if( mSelectedTabIdx >= 0 )
					{
						//focus set back to the tabbar, the selector will take over, so set the
						//the background clear
						//mTabList.getChildAt(mSelectedTabIdx).setBackgroundColor(0x00000000);	
						ImageView icon = (ImageView)mTabList.getChildAt(mSelectedTabIdx).findViewById(R.id.tab_item_icon);
						icon.setImageDrawable( mPages.get(mSelectedTabIdx).mDefaultIconSelector );

					}

				}
				return true;
			}*/
			else if( keyCode == KeyEvent.KEYCODE_BACK )
			{
				if( event.getAction() == KeyEvent.ACTION_DOWN)
				{
					//let the TabView handle Back keyevent
					TabView.this.onBackDown( TabView.this );
				}
				return true;
			}
			else
			{
				return false;
			}
		}
	 };

	 @Override
	 protected void onFocusChanged (boolean gainFocus, int direction, Rect previouslyFocusedRect)
	 {
		 super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		 if( gainFocus == true )
		 {
			 focusTabBar();
		 }
	 }
}