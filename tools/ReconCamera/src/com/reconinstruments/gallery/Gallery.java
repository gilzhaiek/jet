package com.reconinstruments.gallery;

import java.io.File;

import android.app.Activity;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.reconinstruments.camera.R;
import com.reconinstruments.camera.ReconCamera;
import com.reconinstruments.camera.util.PhotoUtil;
import com.reconinstruments.camera.util.VideoUtil;

public class Gallery extends Activity {
	private final static String TAG = Gallery.class.getSimpleName();
	private final static boolean DEBUG = ReconCamera.DEBUG;
	
	private FragmentManager mFragmentManager;
	private ViewPager mViewPager;
	private DisplayImageOptions mDisplayImageOptions;
	private File[] mImageFiles;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.d(TAG, "onCreate()");
		
		setContentView(R.layout.gallery_main);
		
		mImageFiles = PhotoUtil.getPhotoFiles();
		
		if (mImageFiles.length == 0) {
			Toast.makeText(this, "There is no picture to show", Toast.LENGTH_LONG).show();
			finish();
		}
		
		int pagerPosition = 0;
		Bundle bundle = getIntent().getExtras();
		if (savedInstanceState != null) {
			pagerPosition = bundle.getInt(ReconCamera.Gallery.EXTRA_PICTURE_POSITION, 0);
		}
		
		mDisplayImageOptions = new DisplayImageOptions.Builder()
		.showImageForEmptyUri(R.drawable.ic_launcher)
		.showImageOnFail(R.drawable.ic_launcher)
		.resetViewBeforeLoading(true)
		.cacheOnDisc(true)
		.imageScaleType(ImageScaleType.EXACTLY)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.displayer(new FadeInBitmapDisplayer(300))
		.build();
		
		mViewPager = (ViewPager) findViewById(R.id.gallery_viewpager);
		mViewPager.setAdapter(new PhotoAdapter(this, mImageFiles , mDisplayImageOptions));
		mViewPager.setCurrentItem(pagerPosition);
		
		
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(ReconCamera.Gallery.EXTRA_PICTURE_POSITION, mViewPager.getCurrentItem());
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

}
