package com.reconinstruments.jetconnectdevice;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.reconinstruments.commonwidgets.CarouselItemFragment;

public class ConnectSmartphoneFragment extends CarouselItemFragment {
    
    public static final ConnectSmartphoneFragment newInstance(int defaultLayout, String defaultText, int defaultImage,
            int item) {
        ConnectSmartphoneFragment instance = new ConnectSmartphoneFragment();
        instance.mDefaultLayout = defaultLayout;
        instance.mDefaultText = defaultText;
        instance.mDefaultImage = defaultImage;
        instance.mItem = item;
        return instance;
        
    }
    
    public ConnectSmartphoneFragment(){
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(mDefaultLayout, container, false);
        TextView messageTextView = (TextView) v.findViewById(R.id.text_view);
        messageTextView.setText(mDefaultText);
        if(mItem == 0){ //highlight the first item
            messageTextView.setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
        }else if (mItem == 1) { // initial status , left align item 1
            messageTextView.setGravity(Gravity.LEFT);
            v.setAlpha(0.5f);
        }
        inflatedView = v;
        return v;
    }
}
