//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;

import com.reconinstruments.commonwidgets.AutofitHelper;
import com.reconinstruments.utils.stats.StatsUtil;

public class ReconPowerWidget extends ReconDashboardWidget {

    private static final String TAG = ReconPowerWidget.class.getSimpleName();
    protected String mBundleKey = "Power";
    protected String mTitle = "Power";
    public enum PowerType {
        POWER,AVG_POWER, MAX_POWER, AVG_3S_POWER, AVG_10S_POWER, AVG_30S_POWER
            };
            
    public ReconPowerWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ReconPowerWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReconPowerWidget(Context context) {
        super(context);
    }
    public ReconPowerWidget(Context context, PowerType type) {
        super(context);
        switch (type) {
        case POWER:
            setupPowerWidget("Power", "POWER");
            break;
        case AVG_POWER:
            setupPowerWidget("AveragePower", "AVG POWER");
            break;
        case MAX_POWER:
            setupPowerWidget("MaxPower", "MAX POWER");
            break;
        case AVG_3S_POWER:
            setupPowerWidget("3sAveragePower", "3S POWER");
            break;
        case AVG_10S_POWER:
            setupPowerWidget("10sAveragePower", "10S POWER");
            break;
        case AVG_30S_POWER:
            setupPowerWidget("30sAveragePower", "30S POWER");
            break;
        }
    }


    @Override
    public void updateInfo(Bundle fullinfo, boolean inActivity) {
        super.updateInfo(fullinfo, inActivity);
        Bundle tempBundle = (Bundle) fullinfo.get("POWER_BUNDLE");
        titleTextView.setText(mTitle);
        Object value = tempBundle.get(mBundleKey);
        if(value instanceof Integer){
            if(((Integer)value) !=  StatsUtil.INVALID_POWER) fieldTextView.setText(prefixZeroes((Integer)value, inActivity));
            else setInvalidText(inActivity);
        }
        unitTextView.setText("W");
        fieldTextView.invalidate();
    }
    
    private void setInvalidText(boolean inActivity){
        fieldTextView.setText(Html.fromHtml(((inActivity) ? mInvalidString : mInvalidGreyString)));
    }
        
    public void setupPowerWidget(String bundleKey, String title){
        setBundleKey(bundleKey);
        setTitle(title);
    }
        
    public void setBundleKey(String mBundleKey) {
        this.mBundleKey = mBundleKey;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    @Override
    public void prepareInsideViews() {
        super.prepareInsideViews();
        AutofitHelper.create(fieldTextView);
    }

}
