package com.reconinstruments.dashlivestats;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout.LayoutParams;

/**
 * Created by root on 06/11/14.
 */
public class JetDialog extends Dialog {

    protected Activity mActivity;

    public JetDialog(Context context, int layout) {
        super(context);
        mActivity = (Activity) context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(layout);
        setCancelable(true);
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        window.setAttributes(params);
        window.getAttributes().windowAnimations = R.style.dialog_animation;
        window.setBackgroundDrawable(new ColorDrawable(Color.argb(180,0,0,0)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                this.dismiss();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
}