//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.passcodelock;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;


public class PasscodeLockFragment extends CarouselItemFragment {

    private String carouselItemNumber;

    public PasscodeLockFragment(){
        //required Android constructor
    }

    public PasscodeLockFragment(int defaultLayout, String defaultText, int defaultImage,
                                int item){
        super(defaultLayout, defaultText, defaultImage, item);
        carouselItemNumber = defaultText;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(mDefaultLayout, container, false);
        TextView textView = (TextView) v.findViewById(R.id.text_view);
        textView.setText(mDefaultText);
        inflatedView = v;
        reAlign(((CarouselItemHostActivity)getActivity()).getPager());
        return v;
    }

    public String getCarouselItemNumber(){
        return carouselItemNumber;
    }

    // new requiremnent -- for infinite view pager
    @Override
    protected Fragment newInstance(Activity nActivity) {
        return new PasscodeLockFragment(mDefaultLayout, mDefaultText, mDefaultImage, mItem);
    }



}
