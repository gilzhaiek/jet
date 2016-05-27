package com.reconinstruments.dashlivestats;

import android.content.Context;
import android.view.KeyEvent;
import com.reconinstruments.utils.DeviceUtils;

public class SearchingGPSDialog extends JetDialog {

    public SearchingGPSDialog(Context context, int layout) {
        super(context, layout);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if(DeviceUtils.isSun()){ // press select button to dismiss the dialog for jet only
                    this.dismiss();
                }
                // do nothing
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                // do nothing
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return mActivity.onKeyDown(keyCode, event);

            default:
                return super.onKeyDown(keyCode, event);
        }
    }
    @Override
    public void onBackPressed() {
	mActivity.onBackPressed();
    }


}
