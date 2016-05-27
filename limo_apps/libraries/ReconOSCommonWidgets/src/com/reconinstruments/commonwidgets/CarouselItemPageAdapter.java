package com.reconinstruments.commonwidgets;


import android.graphics.Paint;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;
import java.util.List;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class CarouselItemPageAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {

    private List<CarouselItemFragment> fragments;
    private ViewPager mPager;
    final String TAG = CarouselItemPageAdapter.class.getSimpleName();
    protected final int NUM_DATA_ITEMS;
    protected final int NUM_FRAG_ITEMS;
    private boolean isInfinite;
    private boolean isDynamic;
    static final int MIN_NUM_FRAGMENTS_TO_SHOW_BREADCRUMBS = 3;
    static final int MIN_NUM_FRAGMENTS_TO_LOOP = 4;
    static final int MIN_NUM_FRAGMENTS_NOT_TO_ADD_COPIES = 6;
    //^ becasue the view pager caches 5 fragments (double check that)
    //the way the looping effect is achieved differs based on the
    //number of fragments that we have. If a looped carousel has less
    //than MIN_NUM_FRAGMENTS_NOT_TO_DOUBLE then we double the fragments
    //otherwise the viewpager 

    // breadcrumbs
    private Toast breadcrumbToast;
    private BreadcrumbView mBreadcrumbView;
    private boolean breadcrumbViewCreated = false; // double check this
    private boolean mEnableBreadcrumbView = true;

    // needed for HostActivity, num_data_items required when enlarging the fragments list 
    // (i.e., when fragments.size() is less than 6 but larger than 3)
    public CarouselItemPageAdapter(FragmentManager fm, List<CarouselItemFragment> fragments, ViewPager mPager,
                                   boolean isInfinite, boolean isDynamic, FragmentActivity activity) {
        super(fm);
        this.mPager = mPager;
        this.fragments = fragments;
        this.isInfinite = isInfinite;
        this.isDynamic = isDynamic; // for left aligned view pager only
        NUM_DATA_ITEMS = fragments.size();

        if (isInfinite) makeInfinite(activity);
        NUM_FRAG_ITEMS = fragments.size();
    }

    public CarouselItemPageAdapter(FragmentManager fm, List<CarouselItemFragment> fragments, ViewPager mPager) {
	    this(fm,fragments,mPager,false,false,null);
    }

    // deprecated constructor - DO NOT USE
    public CarouselItemPageAdapter(FragmentManager fm, List<CarouselItemFragment> fragments) {
	    this(fm,fragments,null,false,false,null);
    }

    @Override
    public Fragment getItem(int position) {
        CarouselItemFragment fg = fragments.get(position%NUM_FRAG_ITEMS); 
        // Log.v("NewInstanceInAdapter", fg+""); 
        return fg; 
    }

    @Override
    public int getCount() {
        // looping, return more than required amount here
        if (NUM_DATA_ITEMS > 3 && isInfinite)
            return Integer.MAX_VALUE;
        else
            return NUM_DATA_ITEMS;
    }

    @Override
    public float getPageWidth(int position) {
        if(isDynamic){
            CarouselItemFragment cif = (CarouselItemFragment) this.getItem(position);
            String text = cif.getDefaultText();

            // assumption: text size is 44sp, spacing calculated using 44+4 for padding
            Paint pnt = new Paint();
            pnt.setTextSize(48f);
            float text_size = pnt.measureText(text);

            // 320px truncation, 300 to eliminate unwanted padding inherent to textview
            if (text_size < 300)
                return text_size/428f;
            else
                return 300/428f;

        }
        return 1.0f;
    }

    @Override
    public void onPageSelected(int i) {
	if (mPager == null) return;
        if (fragments.size() >= MIN_NUM_FRAGMENTS_TO_SHOW_BREADCRUMBS) {
        	if(mEnableBreadcrumbView){
	            // redraw breadcrumbs
	            showHorizontalBreadcrumb(mPager.getContext(), this.NUM_DATA_ITEMS, mPager.getCurrentItem() % NUM_DATA_ITEMS);
        	}
        }

        CarouselItemFragment cif = (CarouselItemFragment) this.getItem(i);
        if (i==0) {
            cif.reAlign(mPager);
            cif = (CarouselItemFragment) this.getItem(i + 1);
            cif.reAlign(mPager);
        }
        else if (i==this.getCount()-1) {
            cif.reAlign(mPager);
            cif = (CarouselItemFragment) this.getItem(i - 1);
            cif.reAlign(mPager);
        }
        else {
            cif.reAlign(mPager);
            cif = (CarouselItemFragment) this.getItem(i + 1);
            cif.reAlign(mPager);
            cif = (CarouselItemFragment) this.getItem(i - 1);
            cif.reAlign(mPager);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public int getDataItemCount() { return NUM_DATA_ITEMS; }

    // breadcrumb function from pagercontainer
    public void showHorizontalBreadcrumb(Context context, int size, int currentPosition){

        if(breadcrumbViewCreated){
            mBreadcrumbView.redrawBreadcrumbs(context, currentPosition);
            mBreadcrumbView.invalidate();
        } else {
            int[] dashFrags = new int[size];
            for(int i=0; i<dashFrags.length; i++)
                dashFrags[i] = BreadcrumbView.DYNAMIC_ICON;

            mBreadcrumbView = new BreadcrumbView(context, true, currentPosition, dashFrags);
            mBreadcrumbView.invalidate();

            if(breadcrumbToast == null)
                breadcrumbToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);

            breadcrumbToast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 0);
            breadcrumbViewCreated = true;
        }

        breadcrumbToast.setView(mBreadcrumbView);
        breadcrumbToast.show();
    }

    public void hideBreadcrumbs(){
        if(breadcrumbToast != null) breadcrumbToast.cancel();
    }

    public void makeInfinite(FragmentActivity activity) {
	    if (activity == null) return;
        boolean newInstanceExists = false;
	    if (fragments.size() == 0) return; // Sanity check

        // check if newInstance method returns fragments
	    newInstanceExists = (((CarouselItemFragment)(fragments.get(0))).newInstance(activity)
			     != null );

	    if (!newInstanceExists) {
            this.isInfinite = false;
            Log.i("Please override and implement 'newInstance' method for the fragments passed to enable infinite pager.", "");
            return;
        }
	    int fragmentsSize = fragments.size();

        // optimization, feed six or more distinct fragments into pager adapter for looping
        if (newInstanceExists && this.isInfinite &&
	                            fragmentsSize < MIN_NUM_FRAGMENTS_NOT_TO_ADD_COPIES) {

            int factor = (int) ((((float) MIN_NUM_FRAGMENTS_NOT_TO_ADD_COPIES) - 0.1f) /
				                    (float)(fragmentsSize)) + 1;

		    // factor * fragmentsSize is the number of items that need to
		    // be ADDED to the existing list to make the total
		    // number of elements in the list exceed
		    // MIN_NUM_FRAGMENTS_NOT_TO_DOUBLE
		    // Loop through and add copies of the fragments in
		    // order of apearance.
		    for (int i = 0; i < fragmentsSize*factor; i++)
		        fragments.add((CarouselItemFragment)((fragments.get(i%fragmentsSize)).newInstance(activity)));

        }
    }

    /**
     * Set whether to show BreadcrumbVBiew on this ViewPager
     * @param enableBreadcrumbView
     */
    public void setBreadcrumbView(boolean enableBreadcrumbView){
    	this.mEnableBreadcrumbView = enableBreadcrumbView;
    }
    
    /**
     * 
     * @return whether BreadcrumbView is enabled on this ViewPager
     */
    public boolean getBreadcrumbViewEnabled(){
    	return mEnableBreadcrumbView;
    }
}
