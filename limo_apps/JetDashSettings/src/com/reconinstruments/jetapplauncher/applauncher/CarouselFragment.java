package com.reconinstruments.jetapplauncher.applauncher;


import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.reconinstruments.jetapplauncher.R;

public class CarouselFragment extends Fragment {
	private static final String TAG = CarouselFragment.class.getSimpleName();
	private ArrayList<ApplicationInfo> mAppInfo;
	private View mCreatedView = null;
	private int mPosition;
	private float mScale;
	
	NotifyActivity mCallback;
	
	public interface NotifyActivity {
		public void fragmentAtPosCreated(int position);
	}
	
	public static Fragment getNewInstance(Context c, float scale, ArrayList<ApplicationInfo> app, int position){
		CarouselFragment cf;
		cf = (CarouselFragment)Fragment.instantiate(c, CarouselFragment.class.getName());
		cf.setApplicationInfo(app);
		cf.setPosition(position);
		cf.setScale(scale);
		return cf;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (NotifyActivity) activity;
		} catch (ClassCastException ce) {
			throw new ClassCastException(activity.toString()
					+ " must implement NotifyActivity");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mCreatedView = inflater.inflate(R.layout.app_carousel_item, container, false);
		// get application icon and apply in view, the name of the app will be set in the PagerAdapter
		// in a TextView residing outside the CarouselViewPager
		ApplicationInfo app = mAppInfo.get(mPosition);
		ImageView iv = (ImageView) mCreatedView.findViewById(R.id.item_image);
		iv.setImageDrawable(app.icon);
		iv.setAlpha(0.4f);
		
		MyLinearLayout myLL = (MyLinearLayout) mCreatedView.findViewById(R.id.item_root_layout_id);
		myLL.setWillNotDraw(false);
		myLL.setLayoutScale(mScale);
		return mCreatedView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		mCallback.fragmentAtPosCreated(mPosition);
	}
	
	public void setApplicationInfo(ArrayList<ApplicationInfo> apps){
		mAppInfo = apps;
	}
	
	public ArrayList<ApplicationInfo> getApplicationInfor(){
		return mAppInfo;
	}
	
	public void setPosition(int pos){
		this.mPosition = pos;
	}
	
	public int getPosition(){
		return mPosition;
	}
	
	public void setScale(float scale){
		mScale = scale;
	}
	
	public float getScale(){
		return mScale;
	}
	
	public void setViewAlpha(float alpha){
		if(mCreatedView != null) ((ImageView)mCreatedView.findViewById(R.id.item_image)).setAlpha(alpha);
		else Log.e(TAG, "mCreatedView in fragment is null!");
	}
}
