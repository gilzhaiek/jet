package com.reconinstruments.example;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.reconinstruments.commonwidgets.CarouselItemHostActivity;

public class TestFragmentActivity extends CarouselItemHostActivity {

	@Override 
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.carousel_with_text_choose_activity);
		initPager(); 
		mPager.setCurrentItem(1);
	}
	
	@Override
	protected List<Fragment> getFragments() {
		List<Fragment> fList = new ArrayList<Fragment>();
        fList.add(new TestActivityFragment(R.layout.sample_view_pager_fragment, "CYCLING",
                                             R.drawable.cycling_icon, 0));
        fList.add(new TestActivityFragment(R.layout.sample_view_pager_fragment,
               	"NEW ACTIVITY", R.drawable.newactivity_icon, 1));
        fList.add(new TestActivityFragment(R.layout.sample_view_pager_fragment,
        		"NOTIFICATIONS", R.drawable.notifications_icon, 2));
        fList.add(new TestActivityFragment(R.layout.sample_view_pager_fragment,
        		"SETTINGS", R.drawable.settings_icon, 3));
        return fList;
	}

}
