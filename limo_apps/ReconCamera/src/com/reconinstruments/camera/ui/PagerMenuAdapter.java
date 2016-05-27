package com.reconinstruments.camera.ui;

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * This is the adapter responsible for supplying the right fragment to the view pager
 * 
 * @author patrickcho
 *
 */
public class PagerMenuAdapter extends FragmentPagerAdapter {
	ArrayList<? extends Fragment> mFragmentList;

	public PagerMenuAdapter(FragmentManager fm, ArrayList<? extends Fragment> list) {
		super(fm);
		mFragmentList = list;
	}

	@Override
	public int getCount() {
		return mFragmentList.size();
	}

	@Override
	public Fragment getItem(int position) {
		return mFragmentList.get(position);
	}
}
