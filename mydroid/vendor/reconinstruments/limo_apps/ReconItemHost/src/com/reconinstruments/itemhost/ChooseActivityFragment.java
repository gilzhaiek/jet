
package com.reconinstruments.itemhost;

import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.reconinstruments.utils.stats.ActivityUtil;

/**
 * <code>ChooseActivityFragment</code> is designed to represent the remote view item
 * layout. It receives the remote view from <code>ItemHostService</code> and
 * show the content as an item.
 */
public class ChooseActivityFragment extends CarouselItemFragment {
	
    private String mAssociatedProfileName = "";
    public ChooseActivityFragment() {
	// Empty constructor as required by Android
    }
    public ChooseActivityFragment(int defaultLayout, String defaultText, int defaultImage, int item, String profileName) {
        super(defaultLayout, defaultText, defaultImage, item);
        mAssociatedProfileName = profileName;
    }
    public ChooseActivityFragment(int defaultLayout, String defaultText, int defaultImage, int item, String profileName, int activityType) {
        this (defaultLayout, defaultText,defaultImage, item,profileName);
        mSportsAcitivityType = activityType;
    }

    private int mSportsAcitivityType = ActivityUtil.SPORTS_TYPE_CYCLING; // 
    
    public void setSportsActivityType(int sportsActivityType) {
        mSportsAcitivityType = sportsActivityType;
    }
    public int getSportsActivityType() {
        return mSportsAcitivityType;
    }

    /**
     * Getter for mAssociatedProfileName
     *
     * @return a <code>String</code> value
     */
    public String getAssociatedProfileName() {
        return mAssociatedProfileName;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(mDefaultLayout, container, false);
        TextView textView = (TextView) v.findViewById(R.id.text_view);
        textView.setText(mDefaultText);
        ImageView imageView = (ImageView) v.findViewById(R.id.image_view);
        imageView.setBackgroundResource(mDefaultImage);
        
        inflatedView = v;
	// In the future remote View code can be inserted here. 
        reAlign(((CarouselItemHostActivity)getActivity()).getPager());
        return v;
    }
    
}
