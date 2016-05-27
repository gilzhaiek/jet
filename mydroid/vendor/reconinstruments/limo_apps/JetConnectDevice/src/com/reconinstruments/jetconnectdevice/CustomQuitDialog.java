//-*-  indent-tabs-mode:nil;  -*-
package com.reconinstruments.jetconnectdevice;

import java.lang.reflect.Field;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;

import com.reconinstruments.commonwidgets.CommonUtils;
import com.reconinstruments.commonwidgets.ReconJetDialog;

public class CustomQuitDialog extends ReconJetDialog {
    
    private static final String TAG = CustomQuitDialog.class.getSimpleName();
    private static Field sChildFragmentManagerField;
    private Activity mActivity;

    static {
        Field f = null;
        try {
            f = Fragment.class.getDeclaredField("mChildFragmentManager");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "Error getting mChildFragmentManager field", e);
        }
        sChildFragmentManagerField = f;
    }
    
    public CustomQuitDialog(Activity activity, String title, String desc, List<Fragment> fragmentList, int layout){ 
       super(title, desc, fragmentList, layout);
        mActivity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (sChildFragmentManagerField != null) {
            try {
                sChildFragmentManagerField.set(this, null);
            } catch (Exception e) {
                Log.e(TAG, "Error setting mChildFragmentManager field", e);
            }
        }
    }

    @Override
    protected void setupKeyListener() {
        getDialog().setOnKeyListener(
                                     new DialogInterface.OnKeyListener() {

                                         @Override
                                         public boolean onKey(DialogInterface dialog,
                                                              int keyCode, KeyEvent event) {
                        

                                             switch (keyCode) {
                                             case KeyEvent.KEYCODE_ENTER:
                                             case KeyEvent.KEYCODE_DPAD_CENTER:
												 if (event.getAction() == KeyEvent.ACTION_DOWN) return true;
                                                 if (mPager.getCurrentItem() == 0) {
                                
                                                     FragmentManager fm = ((FragmentActivity)mActivity).getSupportFragmentManager(); 
                                                     Fragment frag = fm.findFragmentByTag("quit_dialog");
                                                     if(frag != null){
                                                         DialogFragment df = (DialogFragment) frag;
                                                         df.dismissAllowingStateLoss();
                                                     }
                                                     //                             getDialog().dismiss();
                                                     return true;
                                                 } else {
                                                     FragmentManager fm = ((FragmentActivity)mActivity).getSupportFragmentManager();
                                                     Fragment frag = ((FragmentActivity)mActivity).getSupportFragmentManager().findFragmentByTag("quit_dialog");
                                                     if(frag != null){
                                                         DialogFragment df = (DialogFragment) frag;
                                                         df.dismissAllowingStateLoss();
                                                     }
                                                     //                             getDialog().dismiss();
                                                     if(mActivity instanceof WaitingForJetMobileActivity){
                                                         CommonUtils.launchPrevious(mActivity,null,true);
                                                     }
                                                     else {
                                                         mActivity.finish();
                                                     }
                                                 }
                                                 break;
                                             default:
                                                 break;
                                             }
                                             return false;
                                         }

                                     });
    }
}
