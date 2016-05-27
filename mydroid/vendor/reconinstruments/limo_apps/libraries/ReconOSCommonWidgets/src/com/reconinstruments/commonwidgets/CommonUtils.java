package com.reconinstruments.commonwidgets;

import android.content.Intent;
import android.content.Context;
import android.app.Activity;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.annotation.TargetApi;
import android.os.Build;

/**
 * Utilities to abstract the different transition animations for going
 * between Activities.
 *
 */
public class CommonUtils {

    /**
     * Describe <code>launchNext</code> method here. This is for
     * navigating betweeb similar looking activities usually within
     * one app. It launches an item that is deeper.
     *
     * @param activity an <code>Activity</code> value
     * @param intent an <code>Intent</code> value
     */
    public static void launchNext(Activity activity, Intent intent) {
	launchNext(activity, intent, false);
    }


    public static void launchNext(Activity activity, Intent intent, boolean shouldFinish) {
	if (intent != null) activity.startActivity(intent);
	if (shouldFinish) {
	    activity.finish();
	}
	activity.overridePendingTransition(R.anim.fade_slide_in_right, R.anim.fadeout_faster);
    }
    
    /**
     * Describe <code>launchPrevious</code> method here. launches the
     * previous
     *
     * @param activity an <code>Activity</code> value
     */
    public static void launchPrevious(Activity activity) {
	launchPrevious(activity, null, false);
    }

    /**
     * Describe <code>launchPrevious</code> method here. launches the
     * previous
     *
     * @param activity an <code>Activity</code> value
     */
    public static void launchPrevious(Activity activity, Intent i, boolean shouldFinish) {
	if (i != null) activity.startActivity(i);
	if (shouldFinish) activity.finish();
	activity.overridePendingTransition(R.anim.fade_slide_in_left, R.anim.fadeout_faster);
    }


    /**
     * Describe <code>launchNew</code> method here. For launching
     * activities that are in different apps (packages). 
     *
     * @param activity an <code>Activity</code> value
     * @param intent an <code>Intent</code> value
     */
    public static void launchNew(Activity activity, Intent intent) {
	launchNew(activity,intent,false);
    }

    /**
     * Describe <code>launchNew</code> method here. For launching
     * activities that are in different apps (packages). 
     *
     * @param activity an <code>Activity</code> value
     * @param intent an <code>Intent</code> value
     */
    public static void launchNew(Activity activity, Intent intent,boolean shouldFinish) {
	if (intent != null) activity.startActivity(intent);
	if (shouldFinish) activity.finish();
	activity.overridePendingTransition(R.anim.fade_slide_in_bottom,
					   R.anim.fadeout_faster);
    }


    /**
     * Describe <code>launchParent</code> method here.
     *
     * @param activity an <code>Activity</code> value
     * @param intent an <code>Intent</code> value
     */
    public static void launchParent(Activity activity, Intent intent,
				    boolean shouldFinish) {
	if (intent != null) {
	    activity.startActivity(intent);
	}
	if (shouldFinish) activity.finish();
	activity.overridePendingTransition(R.anim.fade_slide_in_top,
					   R.anim.fadeout_faster);
    }

    public static void launchParent(Activity activity) {
	launchParent(activity, null, false);
    }

    /**
     * Describe <code>scrollDown</code> method here.
     *
     * @param activity an <code>Activity</code> value
     * @param intent an <code>Intent</code> value
     */
    public static void scrollDown(Activity activity, Intent intent,
                                    boolean shouldFinish) {
        if (intent != null) activity.startActivity(intent);

        if (shouldFinish) activity.finish();

        activity.overridePendingTransition(R.anim.slide_in_bottom,
                R.anim.slide_out_top);
    }

    public static void scrollDown(Activity activity) {
        scrollDown(activity, null, false);
    }


    /**
     * Describe <code>scrollUp</code> method here.
     *
     * @param activity an <code>Activity</code> value
     * @param intent an <code>Intent</code> value
     */
    public static void scrollUp(Activity activity, Intent intent,
                                  boolean shouldFinish) {
        if (intent != null) activity.startActivity(intent);

        if (shouldFinish) activity.finish();

        activity.overridePendingTransition(R.anim.slide_in_top,
                R.anim.slide_out_bottom);
    }

    public static void scrollUp(Activity activity) {
        scrollUp(activity, null, false);
    }
    /**
     * <code>scaleAnimate</code> method animates the argument view
     * Calls <code>pushIn</code> and <code>pushOut</code> methods
     *
     * @param v an <code>View</code> value
     *
     */
    public static void scaleAnimate(final View v){
        pushIn(v);
        android.os.Handler hndlr = new android.os.Handler();
        hndlr.postDelayed(new Runnable() {
            @Override
            public void run() {
                pushOut(v);
            }
        }, 40);
    }

    /**
     * <code>pushIn</code> method scales down the argument view by 10%
     *
     * @param v an <code>View</code> value
     *
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static void pushIn(View v) {
        ViewPropertyAnimator anmtr;
        if(v != null){
            anmtr = v.animate();
            anmtr.setDuration(35);
            anmtr.scaleX(0.9f);
            anmtr.scaleY(0.9f);
        }
    }

    /**
     * <code>pushOut</code> method scales up the argument view to 100%
     *
     * @param v an <code>View</code> value
     *
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static void pushOut(View v) {
        ViewPropertyAnimator anmtr;
        if(v != null){
            anmtr = v.animate();
            anmtr.setDuration(20);
            anmtr.scaleX(1f);
            anmtr.scaleY(1f);
        }
    }
}