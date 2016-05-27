package com.reconinstruments.power;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;

@SuppressLint("ValidFragment")
public class ReconPowerMenuFragment extends CarouselItemFragment {

	private String mAssociatedProfileName = "";
	private int mPowerMenuActionType = 0;

	public ReconPowerMenuFragment(int defaultLayout, String defaultText,
			int defaultImage, int item, String profileName) {
		super(defaultLayout, defaultText, defaultImage, item);
		mAssociatedProfileName = profileName;
	}
	
	public String getAssociatedProfileName(){
		return mAssociatedProfileName; 
	}

	public ReconPowerMenuFragment(int defaultLayout, String defaultText,
			int defaultImage, int item, String profileName, int powerMenuActionType) {
		this(defaultLayout, defaultText, defaultImage, item, profileName);
		mPowerMenuActionType = powerMenuActionType; 
	}
	
	public void setPowerMenuActionType(int powerMenuActiontype){
		mPowerMenuActionType = powerMenuActiontype; 
	}

	public int getPowerMenuActionType(){
		return mPowerMenuActionType;
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(mDefaultLayout, container, false);
        TextView textView = (TextView) v.findViewById(R.id.text_view);
        textView.setText(mDefaultText);
        ImageView imageView = (ImageView) v.findViewById(R.id.image_view);
        imageView.setBackgroundResource(mDefaultImage);
        
        RemoteViews views = generateRemoteView();
        ViewGroup vg = (ViewGroup) v.findViewById(R.id.remote_view);
        inflatedView = views.apply(getActivity().getApplicationContext(), vg);
        if (vg.getChildCount() > 0) {
            vg.removeAllViews();
        }
        reAlign(((CarouselItemHostActivity)getActivity()).getPager());
        vg.addView(inflatedView);
        return v;
    }

    private RemoteViews generateRemoteView() {
        RemoteViews remoteView = null;
        remoteView = new RemoteViews(getActivity().getPackageName(), R.layout.carousel_text_only);
        remoteView.setTextViewText(R.id.text_view, mDefaultText);
        return remoteView;
    }
	
}
