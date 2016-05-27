package com.reconinstruments.commonwidgets;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.graphics.drawable.ColorDrawable;

/**
 * <code>ProgressDialog</code> represents a progress message.
 */
public class ProgressDialog extends DialogFragment{
    protected String mTitle;
    private TextView mMsgTV;
    private int mLayout = -1;

    // for text only dialog
    public ProgressDialog(String title) {
        this.mTitle = title;
    }

    // for custom layout dialogs
    public ProgressDialog(int layout){
        mLayout = layout;
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
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view;
        if (mLayout == -1) {
            view = inflater.inflate(R.layout.progress_dialog, container);
            mMsgTV = (TextView) view.findViewById(R.id.text_view);
            mMsgTV.setText(mTitle);
        }
        else view = inflater.inflate(mLayout, container);
        return view;
    }
    
    /**
     * set message only if it's changed
     * @param msg
     */
    public void setMessage(String msg){
       if(mMsgTV != null && !mMsgTV.getText().equals(msg)){
            mMsgTV.setText(msg);
        }
    }

    /**
     * show the progress dialog
     * @param parent
     * @param title
     */
    public static void showProgressDialog(FragmentActivity parent, String title) {
        android.support.v4.app.FragmentManager fm = parent.getSupportFragmentManager();
        Fragment dialog = fm.findFragmentByTag("progress_dialog");
        if(dialog == null){
            ProgressDialog progressDialog = new ProgressDialog(title);
            progressDialog.show(fm, "progress_dialog");
        }else{
            ((ProgressDialog)dialog).setMessage(title);
        }
    }

    /**
     * dismiss the progress diaglog
     * @param parent
     */
    public static void dismissProgressDialog(FragmentActivity parent) {
        Fragment dialog = parent.getSupportFragmentManager().findFragmentByTag("progress_dialog");
        if (dialog != null) {
            DialogFragment df = (DialogFragment) dialog;
            df.dismissAllowingStateLoss();
        }
    }
}
