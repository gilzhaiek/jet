
package com.reconinstruments.dashboard;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.utils.UIUtils;

/**
 * <code>ActivitySummaryFragment</code> represents the activity summary main
 * screen.
 */
public class ActivitySummaryFragment extends CarouselItemFragment {
    private String mMetricType = "";
    private String mMetricUnit = "";
    private Typeface semiboldTypeface;

    public ActivitySummaryFragment() {
	// Android requires it
    }

    public ActivitySummaryFragment(Activity activity, int defaultLayout, String defaultText, int defaultImage,
            int item, String metricType, String metricUnit) {
        super(defaultLayout, defaultText, defaultImage, item);
        mMetricType = metricType;
        mMetricUnit = metricUnit;
        semiboldTypeface = UIUtils.getFontFromRes(activity.getApplicationContext(), R.raw.opensans_semibold);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        inflatedView = inflater.inflate(mDefaultLayout, container, false);
        TextView textView = (TextView) inflatedView.findViewById(R.id.text_view);
        
        textView.setText(removePrefixZero(mDefaultText));
        TextView unitTV = (TextView) inflatedView.findViewById(R.id.unit);
        unitTV.setText(mMetricUnit);
        TextView nameTV = (TextView) inflatedView.findViewById(R.id.name);
        nameTV.setText(mMetricType);
        reAlign(((CarouselItemHostActivity) getActivity()).getPager());
        return inflatedView;
    }
    
    private String removePrefixZero(String value){
        if(value.endsWith("---") || value.contains(":")){
            return value;
        }else if(value.startsWith("+00") || value.startsWith("-00") || value.startsWith("00") || value.startsWith("000")){
            return value.replace("00", "");
        }else if(value.startsWith("+0") || value.startsWith("-0") || value.startsWith("0")){
            return value.replace("0", "");
        }else{
            return value;
        }
    }
    
    @Override
    public void reAlign(ViewPager mPager) {
        if (inflatedView != null) {
            int currentItem = mPager.getCurrentItem();
            //This layout holds some views as a block, set gravity on this block instead of TextView in some cases
            RelativeLayout blockRL = ((RelativeLayout) inflatedView.findViewById(R.id.block));
            // align center when it's current view
            if(blockRL != null){
                if (mItem == currentItem) {
                    blockRL.setGravity(Gravity.CENTER);
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) blockRL.getLayoutParams();
                    lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    blockRL.setLayoutParams(lp);
                    
                    TextView tv = ((TextView) inflatedView.findViewById(R.id.text_view));
                    lp = (RelativeLayout.LayoutParams) tv.getLayoutParams();
                    lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    tv.setLayoutParams(lp);
                    
                    inflatedView.setAlpha(1);
                } else if (mItem < currentItem) { // on the left side
                    blockRL.setGravity(Gravity.RIGHT);
                    inflatedView.setAlpha(0.5f);
                } else { // on the right side
                    blockRL.setGravity(Gravity.LEFT);
                    inflatedView.setAlpha(0.5f);
                }
            }
        }
    }
}
