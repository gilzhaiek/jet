
package com.reconinstruments.messagecenter.frontend;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.messagecenter.R;

/**
 * <code>SummaryFragment</code> provides the Carousel ability
 */
public class SummaryFragment extends CarouselItemFragment {

    public SummaryFragment(int defaultLayout, String defaultText, int defaultImage, int item) {
        super(defaultLayout, defaultText, defaultImage, item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        isDynamic = true; // to disable superclass text realignment inside reAlign method
        View v = inflater.inflate(mDefaultLayout, container, false);
        TextView messageTextView = (TextView) v.findViewById(R.id.text_view);
        messageTextView.setText(mDefaultText);
        messageTextView.setTextColor(getResources().getColor(R.color.recon_jet_non_highlight_text_button));
        v.setAlpha(0.5f);
        reAlign(((CarouselItemHostActivity)getActivity()).getPager());
        inflatedView = v;
        reAlign(((CarouselItemHostActivity)getActivity()).getPager());
        return v;
    }

    // required only for infinite view pager
    @Override
    protected Fragment newInstance(Activity nActivity) {
        return new SummaryFragment(mDefaultLayout, mDefaultText, mDefaultImage, mItem);
    }
}
