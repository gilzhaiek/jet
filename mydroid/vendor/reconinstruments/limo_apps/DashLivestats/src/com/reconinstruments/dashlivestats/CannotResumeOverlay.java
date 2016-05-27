package com.reconinstruments.dashlivestats;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import com.reconinstruments.utils.stats.ActivityUtil;
import com.reconinstruments.utils.stats.TranscendUtils;


public class CannotResumeOverlay extends Dialog{

    private static final String TAG = CannotResumeOverlay.class.getSimpleName();

    public CannotResumeOverlay(Context context, String title, String desc) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.can_not_resume_warning);
        setCancelable(false);
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        window.setAttributes(params);
        window.getAttributes().windowAnimations = R.style.dialog_animation;
        window.setBackgroundDrawable(new ColorDrawable(Color.argb(180,0,0,0)));
        TextView titleView = (TextView) findViewById(R.id.warning_title);
        titleView.setText(title);
        TextView descView = (TextView) findViewById(R.id.warning_text);
        descView.setText(desc);
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
                dismiss();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }
    
    public static CannotResumeOverlay showCannotResumeOverlayIfNeeded(Context context){
        Bundle fullInfo = TranscendUtils.getFullInfoBundle(context);
        if(fullInfo == null) return null;
        Bundle timeBundle = fullInfo.getBundle("TIME_BUNDLE");
        if(timeBundle != null){
            boolean isTimeStillLost = timeBundle.getBoolean("IsTimeStillLost");
            if(isTimeStillLost && (ActivityUtil.getActivityState(context) == ActivityUtil.SPORTS_ACTIVITY_STATUS_ONGOING || ActivityUtil.getActivityState(context) == ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED)){ 
                CannotResumeOverlay cannotResumeOverlay = new CannotResumeOverlay(context, "CANNOT RESUME", "You must have a GPS signal.\nConnect a smartphone to get a faster GPS fix.");
                cannotResumeOverlay.show();
                return cannotResumeOverlay;
            }
        }
        return null;
    }
}
