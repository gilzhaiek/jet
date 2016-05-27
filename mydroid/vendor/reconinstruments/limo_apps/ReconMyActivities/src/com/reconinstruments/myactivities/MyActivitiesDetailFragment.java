
package com.reconinstruments.myactivities;

import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.utils.stats.StatTextUtils;
import com.reconinstruments.utils.stats.StatsUtil.FormattedStat;
import com.reconinstruments.utils.stats.StatsUtil.StatType;
import com.reconinstruments.utils.stats.StatsUtil;
import com.reconinstruments.utils.DeviceUtils;
import com.reconinstruments.utils.UIUtils;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.support.v4.app.Fragment;

/**
 * <code>MyActivitiesDetailFragment</code> displays a single value visible in the DetailActivity
 */
public class MyActivitiesDetailFragment extends CarouselItemFragment {


	private Typeface semiboldTypeface;
	private TextView mTypeTV;
	private StatType statType;
	private FormattedStat stat;
	private TextView textTV;
	private TextView unitTV;
	private TextView typeTV;
	private RelativeLayout blockRL;

	public MyActivitiesDetailFragment(MyActivitiesDetailActivity activity, StatType statType, int item) {
		super(R.layout.my_activities_detail_fragment, null, 0, item, true);
		this.statType = statType;
		SummaryManager summaryManager = activity.summaryManager;
		this.stat = summaryManager.getFormattedStat(statType);

		semiboldTypeface = UIUtils.getFontFromRes(activity.getApplicationContext(), R.raw.opensans_semibold);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(mDefaultLayout, container, false);
        textTV = (TextView) v.findViewById(R.id.text_view);
        unitTV = (TextView) v.findViewById(R.id.unit_view);
		typeTV = (TextView) v.findViewById(R.id.type_view);
        unitTV.setTypeface(semiboldTypeface);
		if(stat!=null) {
	        textTV.setText(stat.value);
	        unitTV.setText(stat.unit);
		} else {
            textTV.setText("NO DATA");
            unitTV.setText("");
		}
		mTypeTV = (TextView) v.findViewById(R.id.type_view);
		mTypeTV.setText(StatTextUtils.getTitleForStat(statType,DeviceUtils.isSnow2()));
		mTypeTV.setTypeface(semiboldTypeface);

		inflatedView = v;
		reAlign(((CarouselItemHostActivity)getActivity()).getPager());
		return v;
	}

	@Override
	public void reAlign(ViewPager mPager) {
		if (inflatedView != null) {
			CarouselItemPageAdapter cipa = (CarouselItemPageAdapter) mPager.getAdapter();
			int numItems = cipa.getDataItemCount();
			int currentItem = mPager.getCurrentItem();

			RelativeLayout.LayoutParams textTVLayoutParams = (RelativeLayout.LayoutParams) textTV.getLayoutParams();
			// align center when it's current view
			if (mItem == currentItem % numItems) {
				textTVLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
				textTVLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
				textTVLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
				textTV.setLayoutParams(textTVLayoutParams);

				showSurroundingText();
				setTextSelectColor();

			} else if (mItem < currentItem % numItems) { // on the left side
				removeCenterAlignment(textTVLayoutParams);
				textTVLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				hideSurroundingText();
				setTextDeselectColor();

			} else { // on the right side
				removeCenterAlignment(textTVLayoutParams);
				textTVLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				hideSurroundingText();
				setTextDeselectColor();

			}
			textTVLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		}

		int textColor = StatTextUtils.getColorForStat(statType,DeviceUtils.isSnow2());
		if(textColor != -1) mTypeTV.setTextColor(textColor);
	}

	private void setTextSelectColor() {
		textTV.setTextColor(getResources().getColor(R.color.recon_white));
	}

	private void setTextDeselectColor() {
		textTV.setTextColor(getResources().getColor(R.color.recon_carousel_deselect_grey));
	}

	private void hideSurroundingText() {
		unitTV.setVisibility(View.INVISIBLE);
		typeTV.setVisibility(View.INVISIBLE);
	}

	private void showSurroundingText() {
		unitTV.setVisibility(View.VISIBLE);
		typeTV.setVisibility(View.VISIBLE);
	}

	private void removeCenterAlignment(RelativeLayout.LayoutParams textTVLayoutParams) {
		//remove center alignment rule to allow block to reset gravity
		textTVLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
		textTV.setLayoutParams(textTVLayoutParams);
	}

	// new requirement -- for infinite view pager
	@Override
	protected Fragment newInstance(Activity nActivity) {
		return new MyActivitiesDetailFragment((MyActivitiesDetailActivity)nActivity, statType, mItem);
	}
}
