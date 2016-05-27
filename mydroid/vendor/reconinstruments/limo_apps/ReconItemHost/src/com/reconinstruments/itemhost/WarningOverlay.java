package com.reconinstruments.itemhost;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import com.reconinstruments.utils.stats.TranscendUtils;
import com.reconinstruments.utils.stats.ActivityUtil;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * <code>WarningOverlay</code> Helper class for showing different
 * dialogues concerning the state of user activity before shutting
 * down and the state of battery and time and proper vs improper
 * shutdown.
 */
public class WarningOverlay extends Dialog{

    private static final String TAG = WarningOverlay.class.getSimpleName();

    public WarningOverlay(Context context, String title, String desc, boolean showIcon) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.battery_warning);
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
        ImageView imgView = (ImageView) findViewById(R.id.warning_img);

        if(showIcon){
            imgView.setVisibility(View.VISIBLE);
        }else{
            imgView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    public static WarningOverlay showWarningOverlayIfNeeded(Context context){
        String show = System.getProperty("show");
        if("1".equals(show)){
            return null;
        }
        System.setProperty("show", "1");
        Bundle fullInfo = TranscendUtils.getFullInfoBundle(context);
        if(fullInfo == null) return null;
        Bundle timeBundle = fullInfo.getBundle("TIME_BUNDLE");
        if(timeBundle != null){
            boolean isTimeStillLost = timeBundle.getBoolean("IsTimeStillLost");
            boolean lastShutdownWasGraceful = timeBundle.getBoolean("LastShutdownWasGraceful");
            long lastShutDownTime = timeBundle.getLong("LastShutDownTime", -1);
            if(isTimeStillLost){
                return showImproperShutdownLostClockOverlay(context); //battery removed
            }else if(ActivityUtil.wasShutDownWhileActivityOngoing(context) && !lastShutdownWasGraceful){ // Show improper shutdown might lose activity data
                return showImproperShutdownActivityOverlay(context);
            }else if(!lastShutdownWasGraceful){ //warning that jet was not properly shutdown
                return showImproperShutdownOverlay(context);
            }else if((ActivityUtil.getActivityState(context) == ActivityUtil.SPORTS_ACTIVITY_STATUS_ONGOING) || (ActivityUtil.getActivityState(context) == ActivityUtil.SPORTS_ACTIVITY_STATUS_PAUSED)){ //incomplete activity
                if(lastShutDownTime > 0){
                    return showIncompleteActivityOverlay(context, lastShutDownTime);
                }
            }
        }
        return null;
    }
    
    //Warning when user does an ungraceful shutdown with an activity in progress.
    private static WarningOverlay showImproperShutdownActivityOverlay(Context context) {
        WarningOverlay warningOverlay = new WarningOverlay(context, "WARNING", Build.MODEL.toString() + " was not shutdown properly. This may affect the data for your activity in progress.", true);
        warningOverlay.show();
        return warningOverlay;
    }
    
    //Warning when the system loses the clock due to prolonged battery removal.
    private static WarningOverlay showImproperShutdownLostClockOverlay(Context context) {
        WarningOverlay warningOverlay = new WarningOverlay(context, "BATTERY REMOVED", "This has reset your system clock.\nConnect a smartphone to fix the system clock.", true);
        warningOverlay.show();
        return warningOverlay;
    }
    
    //Default warning when the user does an ungraceful shutdown.
    private static WarningOverlay showImproperShutdownOverlay(Context context) {
        WarningOverlay warningOverlay = new WarningOverlay(context, "WARNING", Build.MODEL.toString() + " was not turned off properly. Be sure to shutdown " + Build.MODEL.toString() + " from the OPTIONS menu.", true);
        warningOverlay.show();
        return warningOverlay;
    }
    
    //Warning when the user performs a graceful shutdown with an activity in progress.
    private static WarningOverlay showIncompleteActivityOverlay(Context context, long timestamp) {
        SimpleDateFormat sdf = null;
        if(DateFormat.is24HourFormat(context)){
            sdf = new SimpleDateFormat("MMM dd, yyyy, H:mm");
        }else{
            sdf = new SimpleDateFormat("MMM dd, yyyy, h:mm a");
        }
        WarningOverlay warningOverlay = new WarningOverlay(context, "INCOMPLETE ACTIVITY", "You have an unfinished activity from " + sdf.format(new Date(timestamp)) + ".", false);
        warningOverlay.show();
        return warningOverlay;
    }
}
