package com.reconinstruments.camera.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.reconinstruments.camera.R;
import com.reconinstruments.commonwidgets.ReconJetDialog;

import java.util.List;

/**
 * Created by jinkim on 09/03/15.
 */
public class DeleteDialog extends ReconJetDialog {

    private static final String TAG = DeleteDialog.class.getSimpleName();
    private View mInflatedDialogView;

    public interface DialogKeyListener {

        /**
         * Handles the key event for deleting photos when currently in fullscreen mode.
         * @param index index of the current Pager index
         * @param keyCode the input key code
         * @param event the KeyEvent object
         * @return <code>true</code> if the event has been handled, <code>false</code> otherwise
         */
        public boolean handleKeyEvent(DialogInterface dialog, int index, int keyCode, KeyEvent event);
    }

    protected Activity mActivity;
    protected DialogKeyListener mDialogKeyListener;
    protected int mLayout;

    public DeleteDialog(Activity activity, String title, List<Fragment> list, int layout) {
        this(activity, title, list, layout, 300);
    }

    public DeleteDialog(Activity activity, String title, List<Fragment> list, int layout, int width) {
        super(title, list, layout, width);
        mLayout = layout;
        mActivity = activity;
        setCancelable(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInflatedDialogView = super.onCreateView(inflater, container,savedInstanceState);
        TextView titleText = (TextView) mInflatedDialogView.findViewById(R.id.delete_dialog_title);
        titleText.setText(mTitle);
        return mInflatedDialogView;
    }

    @Override
    protected void setupKeyListener() {
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(mDialogKeyListener == null) {
                    Log.e(TAG, "Error: no dialog key listener set!");
                    return false;
                }
                else return mDialogKeyListener.handleKeyEvent(dialog, mPager.getCurrentItem(), keyCode, event);
            }
        });
    }

    public void setDialogKeyListener(DialogKeyListener listener){
        mDialogKeyListener = listener;
    }

    public void setDefaultText(String text, int index){
        DeleteDialogFragment fragment = (DeleteDialogFragment)mList.get(index);
        fragment.setDefaultText(text);
    }

    public void setTitleText(String title){
        mTitle = title;
        TextView titleText = (TextView) mInflatedDialogView.findViewById(R.id.delete_dialog_title);
        titleText.setText(mTitle);
    }
}
