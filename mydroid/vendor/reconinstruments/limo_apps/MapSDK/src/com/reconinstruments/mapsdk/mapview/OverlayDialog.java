package com.reconinstruments.mapsdk.mapview;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.drawable.ColorDrawable;
import android.app.Activity;
import com.reconinstruments.mapsdk.R;
import com.reconinstruments.utils.DeviceUtils;

/**
 * <code>OverlayDialog</code> represents a overlay message.
 */
public class OverlayDialog extends Dialog {

    protected Activity mActivity;
    protected int mLayout;
    protected boolean mFullscreen;

    public OverlayDialog(Context context, int layout, boolean fullscreen) {
    	this(context, layout, 0, fullscreen);
    }
    public OverlayDialog(Context context, int layout, int style, boolean fullscreen) {
        super(context, style);
        mLayout = layout;
        mActivity = (Activity) context;
        mFullscreen = fullscreen;
        setCancelable(true);
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(lp);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(125, 0, 0, 0)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(mLayout);
        ImageView iv = (ImageView) findViewById(R.id.missing_maps);
        if(mFullscreen){
            iv.setImageResource(DeviceUtils.isSun() ? R.drawable.jet_missing_maps_asset_full
                                                    : R.drawable.snow_missing_maps_asset_full);
        }
        else {
            iv.setImageResource(DeviceUtils.isSun() ? R.drawable.jet_missing_maps_asset
                                                    : R.drawable.snow_missing_maps_asset);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                this.dismiss();
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        mActivity.onBackPressed();
    }

}
