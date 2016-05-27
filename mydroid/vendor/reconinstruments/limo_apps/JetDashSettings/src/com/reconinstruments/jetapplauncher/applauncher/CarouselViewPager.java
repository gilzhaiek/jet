package com.reconinstruments.jetapplauncher.applauncher;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.widget.Adapter;

public class CarouselViewPager extends ViewPager  {

	public CarouselViewPager(Context context) {
		super(context);
	}
	
	public CarouselViewPager(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	
	@Override
	public void setAdapter(PagerAdapter adapter){
		CarouselPagerAdapter cAdapter = (CarouselPagerAdapter)adapter;
		cAdapter.setViewPagerReference(this);
		super.setAdapter(adapter);
	}
	
//	@Override
//	public void setCurrentItem(int position){
//		position = getOffsetValue() + (position % getAdapter().getCount());
//		super.setCurrentItem(position);
//	}
//	
//	private int getOffsetValue(){
//		PagerAdapter adapter = getAdapter();
//		if(adapter instanceof CarouselPagerAdapter){
//			return ((CarouselPagerAdapter) adapter).getCount() * 100;
//		}
//		else {
//			return 0;
//		}
//		
//	}
}
