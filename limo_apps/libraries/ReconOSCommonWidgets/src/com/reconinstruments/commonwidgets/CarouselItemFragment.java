
package com.reconinstruments.commonwidgets;

import android.annotation.TargetApi;
import android.content.Intent;

import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import android.util.Log;
import android.view.Gravity;
import android.view.View;

import android.view.ViewPropertyAnimator;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.app.Activity;

/**
 * <code>CarouselItemFragment</code> is designed to represent the remote view item
 * layout. It receives the remote view from <code>ItemHostService</code> and
 * show the content as an item.
 */
public class CarouselItemFragment extends Fragment {

    protected int mDefaultLayout;
    protected int mItem;
    protected String mDefaultText = "";
    protected int mDefaultImage;

    protected boolean mRenderItem = true; // to render TextView color when it's selected

    protected View inflatedView;
    protected Intent mUpLauncher;
    protected Intent mDownLauncher;
    protected boolean isDynamic = false;

    // #new
    private TextView tv;
    private RelativeLayout blockRL;

    // Override in subclass to facilitate infinite_viewpager
    protected Fragment newInstance(Activity nActivity){
        return null;
    }

    public CarouselItemFragment() {
        
    }

    public CarouselItemFragment(int defaultLayout, String defaultText, int defaultImage, int item) {
        mDefaultLayout = defaultLayout;
        mDefaultText = defaultText;
        mDefaultImage = defaultImage;
        mItem = item;
        mRenderItem = true;
    }

    public CarouselItemFragment(int defaultLayout, String defaultText, int defaultImage, int item, boolean render) {
        mDefaultLayout = defaultLayout;
        mDefaultText = defaultText;
        mDefaultImage = defaultImage;
        mItem = item;
        mRenderItem = render;
    }


    public void reAlign(ViewPager mPager) {
        if (inflatedView != null) {
            CarouselItemPageAdapter cipa = (CarouselItemPageAdapter) mPager.getAdapter();
            int numItems = cipa.getDataItemCount();
            int currentItem = mPager.getCurrentItem();
            tv = ((TextView) inflatedView.findViewById(R.id.text_view));
            //This layout holds some views as a block, set gravity on this block instead of TextView in some cases
            blockRL = ((RelativeLayout) inflatedView.findViewById(R.id.block));
            // align center when it's current view
            Log.d("reAlign Call: ", "mItem:"+mItem+", currentItem:"+currentItem+", numItems:"+numItems+", blockRL:"+blockRL+", tv;"+tv );
            if(blockRL != null){
                // align center when it's current view
                if (mItem == currentItem % numItems) {
                    if (!isDynamic) blockRL.setGravity(Gravity.CENTER);
                    inflatedView.setAlpha(1);
                    if(mRenderItem){
                        tv.setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
                        Log.d("Coloring: ", "Colored:"+mItem);
                    }
                } else if (mItem < currentItem % numItems) { // on the left side
                    if (!isDynamic) blockRL.setGravity(Gravity.RIGHT);
                    if(mRenderItem)
                        tv.setTextColor(getResources().getColor(R.color.recon_carousel_deselect_grey));
                } else { // on the right side
                    if (!isDynamic) blockRL.setGravity(Gravity.LEFT);
                    if(mRenderItem)
                        tv.setTextColor(getResources().getColor(R.color.recon_carousel_deselect_grey));
                }
            }else{
                // align center when it's current view
                if(tv != null){
                    // align center when it's current view
                    if (mItem == currentItem % numItems) {
                        if (!isDynamic) tv.setGravity(Gravity.CENTER);
                        inflatedView.setAlpha(1);
                        if(mRenderItem) {
                            tv.setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
                            Log.d("Coloring: ", "Colored:"+mItem);
                        }
                    } else if (mItem < currentItem % numItems) { // on the left side
                        if (!isDynamic) tv.setGravity(Gravity.RIGHT);
                        if(mRenderItem)
                            tv.setTextColor(getResources().getColor(R.color.recon_carousel_deselect_grey));
                    } else { // on the right side
                        if (!isDynamic) tv.setGravity(Gravity.LEFT);
                        if(mRenderItem)
                            tv.setTextColor(getResources().getColor(R.color.recon_carousel_deselect_grey));
                    }
                }
            }
        }
    }

    // #new
    // function called by the ViewPager on key press
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void pushIn() {
        ViewPropertyAnimator anmtr;
        // just in case pushIn gets called before reAlign
        tv = ((TextView) inflatedView.findViewById(R.id.text_view));
        blockRL = ((RelativeLayout) inflatedView.findViewById(R.id.block));
        if(blockRL != null){
            anmtr = blockRL.animate();
            anmtr.setDuration(35);
            anmtr.scaleX(0.90f);
            anmtr.scaleY(0.90f);
        }else{
            if(tv != null){
                anmtr = tv.animate();
                anmtr.setDuration(35);
                anmtr.scaleX(0.90f);
                anmtr.scaleY(0.90f);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    /**
     * <code>pushOut</code> works in conjuction with
     * <code>pushIn</code> to give a beat feelign on selection of
     * item. <code>pushOut</code> will scale the view up to its
     * defautl scale. It has been shrunk by <code>pushIn</code>.
     *
     */
	public void pushOut() {
        ViewPropertyAnimator anmtr;
        // just in case pushIn gets called before reAlign
        tv = ((TextView) inflatedView.findViewById(R.id.text_view));
        blockRL = ((RelativeLayout) inflatedView.findViewById(R.id.block));
        if(blockRL != null){
            anmtr = blockRL.animate();
            anmtr.setDuration(20);
            anmtr.scaleX(1f);
            anmtr.scaleY(1f);
        }else{
            if(tv != null){
                anmtr = tv.animate();
                anmtr.setDuration(20);
                anmtr.scaleX(1f);
                anmtr.scaleY(1f);
            }
        }
    }

    public Intent getUpLauncher() {
        return mUpLauncher;
    }

    public void setUpLauncher(Intent uplauncher) {
        this.mUpLauncher = uplauncher;
    }

    public Intent getDownLauncher() {
        return mDownLauncher;
    }

    public String getDefaultText(){
        return mDefaultText;
    }

    public void setDownLauncher(Intent downlauncher) {
        this.mDownLauncher = downlauncher;
    }
}
