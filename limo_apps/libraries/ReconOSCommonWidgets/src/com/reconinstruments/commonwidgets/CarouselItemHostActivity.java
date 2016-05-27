
package com.reconinstruments.commonwidgets;

import java.lang.reflect.Field;

import java.util.ArrayList; 
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;

import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/**
 * <code>CarouselItemHostActivity</code> is the main activity to host the items. It's
 * implemented by ViewPager with Fragment. RemoteView generated from the
 * <code>ItemHostService</code> would be represented in fragment.
 *
 */
public abstract class CarouselItemHostActivity extends FragmentActivity {

    private static final String TAG = CarouselItemHostActivity.class.getSimpleName();

    protected ViewPager mPager;

    protected HideBreadcrumbView hideBreadcrumbView;
    protected boolean enableInfinitePager = false;
    protected boolean enableDynamicPager = false; // true for left-aligned view pager only
    protected abstract List<Fragment> getFragments();
    static final int SCROLL_SPEED = 300; // in milli-seconds

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    public void onDestroy(Bundle savedInstanceState){	
    	super.onDestroy();
    }
    
    public void onPause(Bundle savedInstanceState){
    	super.onPause();
    }
    
    protected void initPager(){
        List<Fragment> fragments = getFragments();
        
        mPager = (ViewPager) findViewById(R.id.view_pager);

        CarouselItemPageAdapter mPagerAdapter =
	    new CarouselItemPageAdapter(getSupportFragmentManager(),
					(List<CarouselItemFragment>)((List <?>)fragments),
					mPager, enableInfinitePager, enableDynamicPager, this);

        ViewGroup.LayoutParams mPagerParams = mPager.getLayoutParams();
        mPagerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        mPager.setLayoutParams(mPagerParams);
        mPager.setPageMargin(10); // margin between the pages in px

        // to center fragments
        mPager.setClipToPadding(false); // set to false in the layout XML
        mPager.setPadding(90,0,90,0); // padding on right and left sides
//        mPager.setCurrentItem(0, false);
        mPager.setOffscreenPageLimit(1);
        mPager.setAdapter(mPagerAdapter);
        mPager.setOnPageChangeListener(mPagerAdapter);

        // smooth scrolling
        Interpolator sInterpolator = new DecelerateInterpolator();
        try {
            Field mScroller;
            mScroller = ViewPager.class.getDeclaredField("mScroller");
            mScroller.setAccessible(true);
            FixedSpeedScroller scroller = new FixedSpeedScroller(mPager.getContext(), sInterpolator);
            // scroller.setFixedDuration(5000);
            mScroller.set(mPager, scroller);
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }

    }


    public ViewPager getPager() {
        return mPager;
    }

    public void setPager(ViewPager pager) {
        this.mPager = pager;
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    switch (keyCode) {
        case KeyEvent.KEYCODE_DPAD_UP:
            return true;

        case KeyEvent.KEYCODE_ENTER:
        
        case KeyEvent.KEYCODE_DPAD_CENTER:

            // push in and push out animation
            // Enter on older models, and Dpad_center on newer

            final CarouselItemFragment fg = (CarouselItemFragment) ((CarouselItemPageAdapter)mPager.getAdapter())
                    .instantiateItem(mPager,  mPager.getCurrentItem());
            android.os.Handler mHandler = new android.os.Handler();
            Runnable pushOutRunnable = new Runnable() {
                @Override
                public void run() {
                    fg.pushOut();
                }
            };
            fg.pushIn();
            mHandler.postDelayed(pushOutRunnable, 50);

            String optionName = ((CarouselItemFragment)((CarouselItemPageAdapter)mPager.getAdapter())
            		.getItem(mPager.getCurrentItem())).getDefaultText();
            Log.v("CarouselItem Host: ", ""+optionName);
            return true;
        
        default:
            return super.onKeyUp(keyCode, event);
        }
    }

    // smooth scrolling
    public class FixedSpeedScroller extends Scroller {
        private int mDuration = SCROLL_SPEED; // in ms

        public FixedSpeedScroller(Context context) {
            super(context);
        }

        public FixedSpeedScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public FixedSpeedScroller(Context context, Interpolator interpolator, boolean flywheel) {
            super(context, interpolator, flywheel);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            // Ignore received duration, use fixed one instead
            super.startScroll(startX, startY, dx, dy, mDuration);
        }
    }

}
