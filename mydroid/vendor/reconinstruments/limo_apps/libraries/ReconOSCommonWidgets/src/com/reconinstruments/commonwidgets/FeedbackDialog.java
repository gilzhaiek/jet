package com.reconinstruments.commonwidgets;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.Override;

public class FeedbackDialog extends GeneralizedDialog {

    public static final boolean SHOW_SPINNER = true;
    public static final boolean HIDE_SPINNER = false;
    public enum ICONTYPE { CHECKMARK, WARNING }
    private ICONTYPE icon;

    public FeedbackDialog(FragmentActivity parent, String title, String subtitle, ICONTYPE icon, boolean showSpinner) {
        super(title, subtitle, showSpinner, parent);
        this.icon = icon;
    }
    
    public FeedbackDialog(FragmentActivity parent, String title, String subtitle, ICONTYPE icon, boolean showSpinner, boolean fullScreen) {
        super(title, subtitle, showSpinner, parent, fullScreen);
        this.icon = icon;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ImageView mIcon = (ImageView) view.findViewById(R.id.icon_view);

        int drawID = -1;

        if(icon == ICONTYPE.CHECKMARK) drawID = R.drawable.checkmark_icon;
        else if(icon == ICONTYPE.WARNING) drawID = R.drawable.warning_icon;

        Drawable d = null;
        if(drawID != -1) {
            d = getResources().getDrawable(drawID);
            mIcon.setVisibility(View.VISIBLE);
            mIcon.setImageDrawable(d);
        } else mIcon.setVisibility(View.GONE);

        return view;
    }

    public static void showDialog(FragmentActivity parent, String title, String subtitle, ICONTYPE icon, boolean showSpinner) {
        showDialog(parent, title, subtitle, icon, showSpinner, false);
    }
    
    public static void showDialog(FragmentActivity parent, String title, String subtitle, ICONTYPE icon, boolean showSpinner, boolean cancelable) {
        android.support.v4.app.FragmentManager fm = parent.getSupportFragmentManager();
        Fragment dialog = fm.findFragmentByTag(getMyTag());
        if(dialog != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(dialog);
            ft.addToBackStack(null);
        }
        FeedbackDialog cd = new FeedbackDialog(parent, title, subtitle, icon, showSpinner, true);
        cd.setCancelable(cancelable);
        cd.show(fm, getMyTag());
    }
    
    public static void showDialog(FragmentActivity parent, String title, String subtitle, ICONTYPE icon, boolean showSpinner, boolean cancelable, boolean fullScreen) {
        android.support.v4.app.FragmentManager fm = parent.getSupportFragmentManager();
        Fragment dialog = fm.findFragmentByTag(getMyTag());
        if(dialog != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(dialog);
            ft.addToBackStack(null);
        }
        FeedbackDialog cd = new FeedbackDialog(parent, title, subtitle, icon, showSpinner, fullScreen);
        cd.setCancelable(cancelable);
        cd.show(fm, getMyTag());
    }
    
    //provides the ability of keeping updating the title, sub title context
    public static void updateDialog(FragmentActivity parent, String title, String subtitle) {
        android.support.v4.app.FragmentManager fm = parent.getSupportFragmentManager();
        Fragment dialog = fm.findFragmentByTag(getMyTag());
        if(dialog == null) return; //not created, skip
        if(title != null) ((FeedbackDialog)dialog).titleTV.setText(title);
        if(subtitle != null) ((FeedbackDialog)dialog).subTitleTV.setText(subtitle);
    }

    @Override
    protected DialogIdBundle getDialogIdBundle() {
        // pass layout, title, subtile and progressBar ids here (in the correct order)
        return DialogIdBundle.setBundle( R.layout.feedback_dialog, R.id.title, R.id.subtitle, R.id.progress_bar);
    }
}
