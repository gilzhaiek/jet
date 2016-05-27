package com.reconinstruments.lockdown;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import com.reconinstruments.utils.UIUtils;

public class LockDownActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lockdown_jet);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        UIUtils.setButtonHoldShouldLaunchApp(this,false);

    }

    @Override
    protected void onPause() {
        super.onPause();
        UIUtils.setButtonHoldShouldLaunchApp(this,true);
    }

}
