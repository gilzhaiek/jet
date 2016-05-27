package com.reconinstruments.commonwidgets;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class PopUpDialog extends GeneralizedDialog{

    public enum ICONTYPE { CHECKMARK, WARNING }
    public static final int DEFAULT_TIMEOUT = 2;
    private ICONTYPE mIcon;
    private FragmentActivity mParentActivity;

    public PopUpDialog(String title, String subtitle, FragmentActivity parent, ICONTYPE icon) {
        super(title, subtitle, false, parent);
        mParentActivity = parent;
        mIcon = icon;
    }

    public PopUpDialog setSpinner(boolean spinnerSet){
        this.showProgressBar = spinnerSet;
        return this;
    }

    //generic showDialog with preset timer of 2 seconds
    public PopUpDialog showDialog(){
        initShowDialog();
        setTimer(mParentActivity, DEFAULT_TIMEOUT, null);
        return this;
    }

    public PopUpDialog showDialog(int timerSeconds, Runnable obj){
        initShowDialog();
        setTimer(mParentActivity, timerSeconds, obj);
        return this;
    }

    private void initShowDialog() {
        android.support.v4.app.FragmentManager fm = mParentActivity.getSupportFragmentManager();
        Fragment dialog = fm.findFragmentByTag(getMyTag());
        if(dialog != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(dialog);
            ft.addToBackStack(null);
        }
        this.show(fm,getMyTag());
    }

    public static void setTimer(final FragmentActivity mParent, int seconds, final Runnable obj) {
        new CountDownTimer(seconds * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                if(obj != null){
                    obj.run();
                }
                PopUpDialog.dismissDialog(mParent);
            }
        }.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ImageView iconView = (ImageView) view.findViewById(R.id.icon_view);

        int drawID = -1;

        if(this.mIcon == ICONTYPE.CHECKMARK) drawID = R.drawable.checkmark_icon;
        else if(this.mIcon == ICONTYPE.WARNING) drawID = R.drawable.warning_icon;

        Drawable d;
        if(drawID != -1) {
            d = getResources().getDrawable(drawID);
            iconView.setVisibility(View.VISIBLE);
            iconView.setImageDrawable(d);
        } else iconView.setVisibility(View.GONE);

        return view;
    }

    @Override
    protected DialogIdBundle getDialogIdBundle() {
        // pass layout, title, subtile and progressBar ids here (in the correct order)
        return DialogIdBundle.setBundle( R.layout.feedback_dialog, R.id.title, R.id.subtitle, R.id.progress_bar);
    }
}
