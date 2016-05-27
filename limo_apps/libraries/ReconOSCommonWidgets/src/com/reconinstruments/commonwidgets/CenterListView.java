package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

/*
 * 
 * Custom List View that shows three items with the selected item always in the center and uses a circular adapter
 * 
 * to use, ensure that the ListAdapter passed in setAdapter implements the CircularListAdapter interface
 * 
 */
public class CenterListView extends ListView{

	private static final String TAG = "CenterListView";

	CircularListAdapter mAdapter;
	int mChildHeightSize = 0;
	int mChildHeightMode = 0;

	boolean initialized = false;

	public CenterListView(Context context) {
		super(context);
	}
	public CenterListView(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	public CenterListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		mAdapter = (CircularListAdapter) adapter;
		super.setAdapter(mAdapter);
	}

	public int getRealCount(){
		return mAdapter!=null?mAdapter.getRealCount():0;
	}
	public int getFirstPosition(){
		if(getRealCount()==0) 
			return Integer.MAX_VALUE/2;
		return ((Integer.MAX_VALUE/getRealCount())/2)*getRealCount();
	}

	@Override
	protected void handleDataChanged() {
		super.handleDataChanged();

		// assume the size of the list increased by one
		int lastSize = getRealCount()-1;
		int item = getSelectedItemPosition()%lastSize;

		setSelection(getFirstPosition()+item);
	}
	
	@Override
	public void invalidate() {
		setSelection(getSelectedItemPosition());
		super.invalidate();
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 
		if(mAdapter!=null)
			measureChild(widthMeasureSpec, heightMeasureSpec);
		
		int height = mChildHeightSize*Math.min(getRealCount(),3);

		int measuredHeight = MeasureSpec.makeMeasureSpec(height, mChildHeightMode);
		setMeasuredDimension(getMeasuredWidth() , measuredHeight);

		if(!initialized&&mAdapter!=null){
			setSelection(getFirstPosition());
			initialized = true;
		}
	}
	@Override
	protected float getTopFadingEdgeStrength(){
		return getRealCount()>2?super.getTopFadingEdgeStrength():0;
	}
	@Override
	protected float getBottomFadingEdgeStrength(){
		return getRealCount()>1?super.getBottomFadingEdgeStrength():0;
	}
	private void measureChild(int widthMeasureSpec, int heightMeasureSpec){
		final View child = mAdapter.getView(0, null, this);
		if (child.getLayoutParams() == null) {
			child.setLayoutParams(generateDefaultLayoutParams());
		}
		measureChild(child,widthMeasureSpec,heightMeasureSpec);
		mChildHeightSize = MeasureSpec.getSize(child.getMeasuredHeight());
		mChildHeightMode = MeasureSpec.getMode(child.getMeasuredHeight());
	}
	@Override
	public void setSelection(int position) {
		super.setSelectionFromTop(position, getRealCount()>2?mChildHeightSize:0);
	}

	public void selectNext(){
		int selection = (getSelectedItemPosition()+1)%getCount();
		setSelection(selection);
	}
	
	@Override
	public boolean isInTouchMode() {
		return false;
	}
}