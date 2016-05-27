package com.reconinstruments.commonwidgets;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/**
 * <code>GeneralizedDialog</code> generates a DialogFragment overlay
 * for your activity to show an appropriate message.
 *
 * *********
 * IMPORTANT: The child class should overwrite a "getDialogIdBundle" method passing
 * the IDs for layout, title view, subtitle view and progressBar in "DialogIdBundle".
 *
 * *********
 */
public class GeneralizedDialog extends DialogFragment {

    private String titleText;
    private String subTitleText;
    boolean showProgressBar;
    protected TextView titleTV;
    protected TextView subTitleTV;
    private ProgressBar progressBar;
    private Context context;
    private ImageGetter mImageGetter = new ImageGetter();
    private boolean mFullScreen = true; // to set if this dialog is full screen or not
    protected static String TAG = GeneralizedDialog.class.getSimpleName();

    public GeneralizedDialog(String title, String subTitle, boolean showProgressBar, FragmentActivity parent){
        this(title, subTitle, showProgressBar, parent, true);
    }
    
    public GeneralizedDialog(String title, String subTitle, boolean showProgressBar, FragmentActivity parent, boolean fullScreen){
        titleText = title;
        subTitleText = subTitle;
        this.showProgressBar = showProgressBar;
        context = parent.getApplicationContext();
        mImageGetter = new ImageGetter();
        mFullScreen = fullScreen;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        Window window = getDialog().getWindow();
        window.getAttributes().windowAnimations = R.style.dialog_animation;
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(mFullScreen)
            setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        else
            setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Light);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        DialogIdBundle dib = getDialogIdBundle();

        int layoutID = dib.getLayout();
        View view =  inflater.inflate(layoutID, container);

        titleTV = (TextView) view.findViewById(dib.getTitleTV());
        if(titleText == null) titleTV.setVisibility(View.GONE);
        else titleTV.setText(Html.fromHtml(titleText, mImageGetter, null));

        subTitleTV = (TextView) view.findViewById(dib.getSubTitleTV());
        if(subTitleText == null) subTitleTV.setVisibility(View.GONE);
        else subTitleTV.setText(Html.fromHtml(subTitleText, mImageGetter, null));

        progressBar = (ProgressBar) view.findViewById(dib.getProgressBar());
        if(!showProgressBar) progressBar.setVisibility(View.GONE);

        return view;
    }

    public static void showDialog(FragmentActivity parent, String title, String subtitle, boolean showProgressBar) {
        android.support.v4.app.FragmentManager fm = parent.getSupportFragmentManager();
        Fragment dialog = fm.findFragmentByTag(getMyTag());
        if(dialog != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(dialog);
            ft.addToBackStack(null);
        }
        GeneralizedDialog cd = new GeneralizedDialog(title, subtitle, showProgressBar, parent);
        cd.show(fm, getMyTag());
    }

    public static void dismissDialog(FragmentActivity parent) {
        Fragment dialog = parent.getSupportFragmentManager().findFragmentByTag(getMyTag());
        if (dialog != null) {
            GeneralizedDialog df = (GeneralizedDialog) dialog;
            df.dismissAllowingStateLoss();
        }
    }

    public class ImageGetter implements Html.ImageGetter {
        @Override
        public Drawable getDrawable(String source) {
            String name = source;
            if(name.contains("")) name = name.substring(0, name.lastIndexOf('.')); // TODO: better name detection
            int resID = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
            Drawable d = null;
            if(resID != -1) {
                d = context.getResources().getDrawable(resID);
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            }
            return d;
        }
    }

    protected static String getMyTag(){
        return TAG;
    }

    protected DialogIdBundle getDialogIdBundle() {
        return DialogIdBundle.setBundle(R.layout.generalized_dialog, R.id.titleView,
                R.id.subTitleView, R.id.progressBar);
    }

    protected static class DialogIdBundle {
        // private IDs
        private int layout, titleTV, subtitleTV, progressBar;

        public DialogIdBundle(int layoutId, int titleId, int subTitleId, int progressBarId){
            this.layout = layoutId;
            this.titleTV = titleId;
            this.subtitleTV = subTitleId;
            this.progressBar = progressBarId;
        }

        public static DialogIdBundle setBundle(int layoutId, int titleId, int subTitleId, int progressBarId){
            return new DialogIdBundle(layoutId, titleId, subTitleId, progressBarId);
        }

        // ID getter functions
        public int getTitleTV(){ return titleTV; }
        public int getSubTitleTV(){ return subtitleTV; }
        public int getProgressBar(){ return progressBar; }
        public int getLayout(){ return layout; }
    }
}