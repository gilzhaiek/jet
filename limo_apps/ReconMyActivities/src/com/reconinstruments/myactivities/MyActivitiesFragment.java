
package com.reconinstruments.myactivities;

import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.utils.UIUtils;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import android.app.Activity;

/**
 * <code>MyActivitiesFragment</code> provides two option, all time and last
 */
public class MyActivitiesFragment extends CarouselItemFragment {

	private MyActivitiesActivity mActivity;
	private TextView mMessageText;

	public MyActivitiesFragment(MyActivitiesActivity activity, int defaultLayout, String defaultText, int defaultImage, int item) {
		super(defaultLayout, defaultText, defaultImage, item);
		mActivity = activity;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		inflatedView = inflater.inflate(mDefaultLayout, container, false);
		TextView messageTextView = (TextView) inflatedView.findViewById(R.id.text_view);
		messageTextView.setText(mDefaultText);
		messageTextView.setTextSize(44f);
		mMessageText = messageTextView;
		reAlign(((CarouselItemHostActivity) getActivity()).getPager());
		return inflatedView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		Typeface semiBold = UIUtils.getFontFromRes(getActivity().getApplicationContext(), R.raw.opensans_regular);
		mMessageText.setTypeface(semiBold);
	}

	// newInstance method - new requirement for infinite view pager only
	@Override
	protected Fragment newInstance(Activity nActivity) {
		return new MyActivitiesFragment((MyActivitiesActivity) nActivity, mDefaultLayout, mDefaultText, mDefaultImage, mItem);
	}
}
