
package com.reconinstruments.commonwidgets;

import java.util.List;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

import java.lang.reflect.Field;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

public abstract class ReconJetDialog extends DialogFragment {

    protected ViewPager mPager;
    protected String mTitle;
    protected String mDesc;
    protected List<Fragment> mList;
    protected int mLayout = -1;
    protected int mItemWidth = -1;

    protected int mStartX = -1;
    protected int mStartY = -1;;
    protected int mHeight = -1;;
    protected float mAlpha = -1.0f;

    protected boolean enableInfinitePager = false;
    protected boolean enableDynamicPager = false; // true for left-aligned view pager only

    public ReconJetDialog(String title, List<Fragment> list) {
        this.mTitle = title;
        this.mList = list;
    }

    /**
       @deprecated
     * constructor
     * @param title
     * @param list
     * @param layout
     * @param width @deprecated use setItemWidth to custom item width, if not set, width is ViewGroup.LayoutParams.MATCH_PARENT by default
     */
    public ReconJetDialog(String title, List<Fragment> list, int layout, int width) {
	this(title,list,layout);
	mItemWidth = -1;
    }

    public ReconJetDialog(String title, List<Fragment> list, int layout) {
	this(title,list);
        this.mLayout = layout;
    }

    public ReconJetDialog(String title, String desc, List<Fragment> list, int layout) {
	this(title,list);
        this.mDesc = desc;
        this.mLayout = layout;
    }

    public ReconJetDialog(String title, List<Fragment> list, int layout, int width, int startX, int startY, int height, float alpha) {
	this(title,list,layout,width);
        this.mStartX = startX;
        this.mStartY = startY;
        this.mHeight = height;
        this.mAlpha = alpha;
    }

    //custom the pager item width
    public void setItemWidth(int width){
        mItemWidth = width;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Window window = getDialog().getWindow();
        LayoutParams params = window.getAttributes();
        /*if(mAlpha < 0.0f) {
        	params.alpha = 0.8f;
        }else {
        	params.alpha = mAlpha;
        }*/
        if(mStartX >= 0 && mStartY >= 0 && mHeight>=0) {
	        params.x= mStartX;
	        params.y= mStartY;
	        params.height = mHeight;
        }
        window.setAttributes((android.view.WindowManager.LayoutParams) params);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        getDialog().getWindow()
                .getAttributes().windowAnimations = R.style.dialog_animation;
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.carousel_with_text, container);
        if(mLayout == -1){ //use default layout
            view = inflater.inflate(R.layout.carousel_with_text, container);
        }else{ // use custom layout
            view = inflater.inflate(mLayout, container);
        }

        TextView textTV = (TextView) view.findViewById(R.id.text_view);
        if(textTV != null){
            textTV.setText(mTitle);
        }else{ // try another attribute - title
            textTV = (TextView) view.findViewById(R.id.title);
            if(textTV != null){
                textTV.setText(mTitle);
            }
        }
        TextView bodyTV = (TextView) view.findViewById(R.id.body);
        if(bodyTV != null){
            bodyTV.setText(mDesc);
        }

        // new ViewPager logic
        mPager = (ViewPager) view.findViewById(R.id.view_pager);

        CarouselItemPageAdapter mPagerAdapter =
                new CarouselItemPageAdapter(getChildFragmentManager(),
                        (List<CarouselItemFragment>)((List <?>)mList),
                        mPager, enableInfinitePager, enableDynamicPager, null);

        ViewGroup.LayoutParams mPagerParams = mPager.getLayoutParams();
        if(mItemWidth == -1){
            mPagerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        }else{
            mPagerParams.width = mItemWidth;
        }
        mPager.setLayoutParams(mPagerParams);
        mPager.setPageMargin(10); // margin between the pages in px

        // to center fragments
        mPager.setClipToPadding(false); // set to false in the layout XML
        mPager.setPadding(120,0,120,0); // padding to center fragments -- test new spacing
        mPager.setCurrentItem(0, false);
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

        setupKeyListener();
        view.setBackgroundColor(0xB3000000);
        return view;
    }

    protected abstract void setupKeyListener();

        // smooth scrolling
    public class FixedSpeedScroller extends Scroller {
        private int mDuration = CarouselItemHostActivity.SCROLL_SPEED; // in ms

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

    public ViewPager getPager() {
        return mPager;
    }
}
