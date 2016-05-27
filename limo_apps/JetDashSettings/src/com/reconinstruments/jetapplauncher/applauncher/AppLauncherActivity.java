package com.reconinstruments.jetapplauncher.applauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;

import com.reconinstruments.jetapplauncher.R;


public class AppLauncherActivity extends FragmentActivity implements CarouselFragment.NotifyActivity {
	public static final String TAG = "AppLauncherActivity";

	private AppAdapter mAppAdapter;
	private boolean blocked = true;
	private int previousColumn;
	private CarouselViewPager carouselViewPager;
	private CarouselPagerAdapter carouselAdapter;
	private View mRootView;
	
	private AppAddDetect mAppAddDetect = new AppAddDetect();
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.app_carousel_layout);
	    
	    mRootView = findViewById(R.id.root_view);
	    carouselViewPager = (CarouselViewPager) mRootView.findViewById(R.id.frame_layout).findViewById(R.id.carousel_viewpager_id);

	    carouselAdapter = new CarouselPagerAdapter(this, getSupportFragmentManager(), mRootView);
	    carouselViewPager.setAdapter(carouselAdapter);
	    carouselViewPager.setOnPageChangeListener(carouselAdapter);
	    carouselViewPager.setOnClickListener(carouselAdapter);
	    
	    carouselViewPager.setCurrentItem(0);
	    
	    carouselViewPager.setOffscreenPageLimit(carouselAdapter.getCount());
	    
	    carouselViewPager.setPageMargin(-303);
	    
	}

	public void onResume() {
		super.onResume();
		IntentFilter intentfilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
		intentfilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		intentfilter.addDataScheme("package");
		registerReceiver(mAppAddDetect,intentfilter);
	}
	
	public void onPause() {
		super.onPause();
		unregisterReceiver(mAppAddDetect);
	}
	
	public ViewPager getViewPager(){
		return carouselViewPager;
	}
	
	public void clickItem(View view){
		view.performClick();
	}
	
	@Override
	public void fragmentAtPosCreated(int position){
		if(position == 0){
			carouselAdapter.onPageSelected(position);
		}
		
	}
	
	private class AppAddDetect extends BroadcastReceiver {
		@Override
		public void onReceive (Context c, Intent i) {
		    Log.d("AppAddDetect", "++++++++ Package add/remove +++++++++++");
		    
		    carouselAdapter = new CarouselPagerAdapter(AppLauncherActivity.this, getSupportFragmentManager(), mRootView);
		    carouselViewPager.setAdapter(carouselAdapter);
		    
		    //ReconPackageRecorder.writePackageData(c, new File(Environment.getExternalStorageDirectory(),PACKAGES_XML_FILE));
		    //goToFavoriteApp();

		}
	}
}
