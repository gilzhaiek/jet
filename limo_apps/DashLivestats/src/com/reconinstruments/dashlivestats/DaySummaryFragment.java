package com.reconinstruments.dashlivestats;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;
import android.support.v4.view.ViewPager;
import android.widget.RelativeLayout;

import com.reconinstruments.commonwidgets.CarouselItemFragment;
import com.reconinstruments.commonwidgets.CarouselItemHostActivity;
import com.reconinstruments.commonwidgets.CarouselItemPageAdapter;
import com.reconinstruments.utils.UIUtils;
import com.reconinstruments.utils.stats.StatTextUtils;
import com.reconinstruments.utils.stats.StatsUtil.FormattedStat;
import com.reconinstruments.utils.stats.StatsUtil.StatType;

public class DaySummaryFragment extends CarouselItemFragment {
    
    FormattedStat stat;
    StatType statType;

    TextView textTV;
    TextView unitTV;
    TextView typeTV;

    public DaySummaryFragment(FormattedStat stat,StatType statType, int item) {
        super(R.layout.day_summary_fragment, null, 0, item, false);
        this.stat = stat;
        this.statType = statType;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(mDefaultLayout, container, false);
        textTV = (TextView) v.findViewById(R.id.text_view);
        unitTV = (TextView) v.findViewById(R.id.unit_view);
        typeTV = (TextView) v.findViewById(R.id.type_view);
        Typeface semiBold = UIUtils.getFontFromRes(getActivity().getApplicationContext(), R.raw.opensans_semibold);
        typeTV.setTypeface(semiBold);
        unitTV.setTypeface(semiBold);
        if(stat!=null) {
            textTV.setText(stat.value);
            unitTV.setText(stat.unit.toUpperCase());
            typeTV.setText(StatTextUtils.getTitleForStat(statType, false));
        } else {
            textTV.setText("");
            unitTV.setText("");
            typeTV.setText("");
        }
        inflatedView = v;
        reAlign(((CarouselItemHostActivity)getActivity()).getPager());
        return v;
    }
    public String getTypeName(StatType type) {
        return "";
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

                inflatedView.setAlpha(1);
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

    }

    private void removeCenterAlignment(LayoutParams textTVLayoutParams) {
        //remove center alignment rule to allow block to reset gravity
        textTVLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
        textTV.setLayoutParams(textTVLayoutParams);
    }

    private void setTextSelectColor() {
        textTV.setTextColor(getResources().getColor(R.color.recon_white));
    }

    private void setTextDeselectColor() {
        textTV.setTextColor(getResources().getColor(R.color.recon_carousel_deselect_grey));
    }

    private void hideSurroundingText() {
        unitTV.setVisibility(View.GONE);
        typeTV.setVisibility(View.GONE);
    }

    private void showSurroundingText() {
        unitTV.setVisibility(View.VISIBLE);
        typeTV.setVisibility(View.VISIBLE);
    }

    @Override
    protected android.support.v4.app.Fragment newInstance(Activity nActivity) {
        return new DaySummaryFragment(stat, statType, mItem);
    }
}