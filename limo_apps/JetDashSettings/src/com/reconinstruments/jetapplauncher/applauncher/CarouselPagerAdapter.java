package com.reconinstruments.jetapplauncher.applauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.reconinstruments.jetapplauncher.R;

public class CarouselPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener, View.OnClickListener {
	
	private static final String TAG = CarouselPagerAdapter.class.getSimpleName();
//	private static final String MUSIC_APP = "com.reconinstruments."
	
	private Context mContext;
	private Resources mRes;
	private ArrayList<ApplicationInfo> mApplications = new ArrayList<ApplicationInfo>();
	private FragmentManager fm;
	private ViewPager mPager;
	private View mAppRootView;
	
	/**
	 * Scale of the item in center focus
	 */
	private float mCenterScale = 1f;
	
	private int mPreviousPos = -1, mCurrentPos = -1;
	
	public CarouselPagerAdapter(Context context, FragmentManager fm, View rootView){
		super(fm);
		this.mContext = context;
		this.fm = fm;
		mRes = context.getResources();
		mAppRootView = rootView;
		PackageManager manager = mContext.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
    	mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
    	// query for a list of currently installed apps, sort and add to List
        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
    	Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));
    	for(ResolveInfo resolveInfo : apps) {
    		ApplicationInfo application = new ApplicationInfo();
    		
    		application.title = resolveInfo.loadLabel(manager);
    		
    		application.setActivity(new ComponentName(
    				resolveInfo.activityInfo.applicationInfo.packageName,
    				resolveInfo.activityInfo.name),
			Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    		application.icon = resolveInfo.activityInfo.loadIcon(manager);

    		mApplications.add(application);
    	}
    	
    	// set item text for first element, as it sometimes doesn't show up
    	TextView tv = (TextView) mAppRootView.findViewById(R.id.item_text);
		tv.setText(mApplications.get(0).title);
	}
	
	@Override
	public void onClick(View v){
		mContext.startActivity(mApplications.get(mPager.getCurrentItem()).intent);
	}
    
	@Override
	public Fragment getItem(int index) {
		return CarouselFragment.getNewInstance(mContext, mCenterScale, mApplications, index);
	}
	
	@Override
	public int getCount() {
		return mApplications.size();
	}

	/**
	 * This method will be invoked when the current page is scrolled, either as part of a 
	 * programmatically initiated smooth scroll or a user initiated touch scroll. If you 
	 * override this method you must call through to the superclass implementation (e.g. 
	 * super.onPageScrolled(position, offset, offsetPixels)) before onPageScrolled returns.
	 * 
	 * @param position Position index of the first page currently being displayed. Page position+1 
	 * 		   			will be visible if positionOffset is nonzero.
	 * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
	 * @param positionOffsetPixels Value in pixels indicating the offset from position.
	 */
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){
	}
	
	
	@Override
	public void onPageScrollStateChanged(int state) {
	}

	@Override
	public void onPageSelected(int position) {
		// update the name of the currently focused app
		TextView tv = (TextView) mAppRootView.findViewById(R.id.item_text);
		tv.setText(mApplications.get(position).title);
		
		// set full opacity for focused app, 50% for others
		mPreviousPos = mCurrentPos;
		mCurrentPos = position;
		
		CarouselFragment currentFrag = (CarouselFragment) fm.findFragmentByTag(getFragmentTag(mCurrentPos));
		currentFrag.setViewAlpha(1f);
		if(mPreviousPos != -1) {
			CarouselFragment previousFrag = (CarouselFragment) fm.findFragmentByTag(getFragmentTag(mPreviousPos));
			previousFrag.setViewAlpha(0.4f);
		}
	}
	
	private MyLinearLayout getRootView(int position)
	{
		return (MyLinearLayout) 
				fm.findFragmentByTag(this.getFragmentTag(position))
				.getView().findViewById(R.id.item_root_layout_id);
	}
	
	public void setFocusedItemScale(float scale){
		mCenterScale = scale;
	}
	
	private String getFragmentTag(int position)
	{
	    return "android:switcher:" + ((AppLauncherActivity)mContext).getViewPager().getId() + ":" + position;
	}
	
	public void setViewPagerReference(ViewPager pager){
		mPager = pager;
	}
	
	public ViewPager getViewPagerReference(){
		return mPager;
	}
}
