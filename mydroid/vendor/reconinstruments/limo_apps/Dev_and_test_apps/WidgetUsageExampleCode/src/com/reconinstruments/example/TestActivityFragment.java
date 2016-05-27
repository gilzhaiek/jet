package com.reconinstruments.example;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;

public class TestActivityFragment extends CarouselItemFragment {

	public TestActivityFragment(int defaultLayout, String defaultText,
			int defaultImage, int item) {
		super(defaultLayout, defaultText, defaultImage, item);
		// TODO Auto-generated constructor stub
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
        reAlign(((CarouselItemHostActivity)getActivity()).getPager());
        return v;
    }
}
