//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.dashlauncher.livestats.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import com.reconinstruments.dashlivestats.R;

public class ReconPowerWidget3x4 extends ReconPowerWidget {

    public ReconPowerWidget3x4(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        addAndPrepareViews(context);
    }


    public ReconPowerWidget3x4(Context context, AttributeSet attrs) {
        super(context, attrs);
        addAndPrepareViews(context);
    }

    public ReconPowerWidget3x4(Context context) {
        super(context);
        addAndPrepareViews(context);
    }
    public ReconPowerWidget3x4(Context context,PowerType type ) {
        super(context, type);
        addAndPrepareViews(context);
    }

    
    private void addAndPrepareViews(Context context){
        LayoutInflater infl = LayoutInflater.from(context);
        View myView = infl.inflate(R.layout.livestats_widget_power_3x4, null);
        this.addView(myView);
        
        prepareInsideViews();
    }
}
