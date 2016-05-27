
package com.reconinstruments.commonwidgets;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import android.app.Activity;

import com.reconinstruments.commonwidgets.R;

public class ReconJetDialogFragment extends CarouselItemFragment {
    
    public ReconJetDialogFragment(int defaultLayout, String defaultText, int defaultImage, int item) {
        super(defaultLayout, defaultText, defaultImage, item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(mDefaultLayout, container, false);
        RelativeLayout blockRL = ((RelativeLayout) v.findViewById(R.id.block));
        TextView messageTextView = (TextView) v.findViewById(R.id.text_view);
        ImageView iconIV = (ImageView) v.findViewById(R.id.image_view);
        messageTextView.setText(mDefaultText);
	messageTextView.setShadowLayer(1, 0, 3, Color.BLACK);
	messageTextView.setTextColor(getResources().getColor(R.color.recon_carousel_deselect_grey));

        if(iconIV != null ){
            if(mDefaultImage > 0){
                iconIV.setVisibility(View.VISIBLE);
                iconIV.setImageResource(mDefaultImage);
            }else{
                iconIV.setVisibility(View.INVISIBLE);
            }
            
        }
        if(mItem == 0){ //highlight the first item
            if(blockRL != null){
                blockRL.setGravity(Gravity.CENTER);
            }else{
                messageTextView.setGravity(Gravity.CENTER);
            }
            messageTextView.setTextColor(getResources().getColor(R.color.recon_jet_highlight_text_button));
        }else if (mItem == 1) { // initial status , left align item 1
            if(blockRL != null){
                blockRL.setGravity(Gravity.LEFT);
            }else{
                messageTextView.setGravity(Gravity.LEFT);
            }

        }
        inflatedView = v;
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected Fragment newInstance(Activity nActivity) {
        return new ReconJetDialogFragment(mDefaultLayout, mDefaultText, mDefaultImage, mItem);
    }
}
