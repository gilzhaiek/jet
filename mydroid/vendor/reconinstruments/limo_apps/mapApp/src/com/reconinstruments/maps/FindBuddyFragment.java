package com.reconinstruments.maps;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.reconinstruments.commonwidgets.ReconJetDialogFragment;

import com.reconinstruments.commonwidgets.R;
/**
 * Created by jinkim on 18/03/15.
 */
public class FindBuddyFragment extends ReconJetDialogFragment {

    public FindBuddyFragment(int defaultLayout, String defaultText, int defaultImage, int item) {
        super(defaultLayout, defaultText, defaultImage, item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(mDefaultLayout, container, false);
        RelativeLayout blockRL = ((RelativeLayout) v.findViewById(R.id.block));
        TextView messageTextView = (TextView) v.findViewById(R.id.text_view);
        ImageView iconIV = (ImageView) v.findViewById(R.id.image_view);
        messageTextView.setText(mDefaultText);
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
        }else if (mItem >= 1) { // initial status , left align item 1
            if(blockRL != null){
                blockRL.setGravity(Gravity.LEFT);
            }else{
                messageTextView.setGravity(Gravity.LEFT);
            }
            messageTextView.setTextColor(Color.rgb(95,95,95));
        }
        inflatedView = v;
        return v;
    }

}
