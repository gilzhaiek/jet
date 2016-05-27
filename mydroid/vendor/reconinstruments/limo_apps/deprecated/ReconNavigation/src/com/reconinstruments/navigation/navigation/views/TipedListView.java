package com.reconinstruments.navigation.navigation.views;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TipedListView extends ListView
{
	private TextView mTipView = null;
	private View mLastSelectedItem = null;
	private ITipsProvider mTipsProvider = null;
	private boolean mShowTips = false;						//disable show tips for now

	public interface ITipsProvider
	{
		public String getTips( int position );				//given a item position(of the ListAdapter) return the tips for that item;
	};
	
	public TipedListView( Context context )
	{
		super(context);
		
		initView( context );
				
	}
	
	public TipedListView( Context context, AttributeSet attrs )
	{
		super( context, attrs );
		
		initView( context );
			
	}
	
	public void setTipsProvider( ITipsProvider tipsProvider )
	{
		mTipsProvider = tipsProvider;
	}
	
	private void initView( Context context )
	{
		mTipView = new TextView( context );
		mTipView.setLayoutParams(new LayoutParams( LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mTipView.setBackgroundColor(0xAA000000);
		mTipView.setTextColor(0xFFFFFFFF);
		mTipView.setVisibility(View.GONE);
		mTipView.setText("Test Tips..");
				
		this.setFocusable(true);
		this.setItemsCanFocus(true);
		this.setOnItemSelectedListener(mItemSelectedListener);
	}
		
	
	public AdapterView.OnItemSelectedListener mItemSelectedListener = new AdapterView.OnItemSelectedListener() 
	{		
		
		@Override
		public void onItemSelected (AdapterView parent, View view, int position, long id)
		{
			ViewGroup listViewContainer = (ViewGroup)parent.getParent();
			
			//if the listview has no parent, we dont show the tips at all
			//if there is no tips-provider hooked, do nothing as well
			if( listViewContainer == null || mTipsProvider == null )
			{
				return;
			}
			
			TipedListView.this.requestFocus();
			
			if( parent == TipedListView.this && parent.hasFocus() )
			{
				if( mLastSelectedItem != view )
				{											
					showTips( view, mTipsProvider.getTips(position));
					mLastSelectedItem = view;										
				}				
			}
		}
		@Override
		public void onNothingSelected (AdapterView parent)
		{
			if( parent == TipedListView.this )
			{
				mTipView.setVisibility(View.GONE);
				mLastSelectedItem = null;
			}

		}

	}; 
	
	@Override
	protected void onFocusChanged( boolean gainFocus, int direction, Rect previouslyFocusedRect )
	{
		if( gainFocus == false )
		{
			mTipView.setVisibility(View.GONE);			
		}
		else
		{	
			View item = this.getSelectedView();
			if( item != null && mTipsProvider != null )
			{				
				showTips( item, mTipsProvider.getTips(this.getSelectedItemPosition()) );				
			}
		}
	}
	
	private void showTips( View item, String tips )
	{
		ViewGroup listViewContainer = (ViewGroup)getParent();
		
		
		//the parent view of TipListView must be RelativeLayout
		//otherwise, we can not position the tips at the right location
		//also, the listview have to align to the left of the parent view
		//so that the tips showing correct at the right side
		if( listViewContainer == null || listViewContainer.getClass() != RelativeLayout.class )
			return;
		
		
		mTipView.setText(tips);

		if( mTipView.getVisibility() != View.VISIBLE )
		{
			if( mTipView.getParent() == null )
			{
				listViewContainer.addView(mTipView);	
				
			}
			
			if( mShowTips )
			{
				mTipView.setVisibility(View.VISIBLE);
			}			
		}		
	
		RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);		
		listViewContainer.updateViewLayout(mTipView, layout);					

		int y = item.getTop();
		int x = 0;
		
		y += getTop();					
		x = getRight() + 5;	//add 5 pixel paddings


		layout.topMargin = y;
		layout.leftMargin = x;
		
		listViewContainer.updateViewLayout(mTipView, layout);					
		
	}
}