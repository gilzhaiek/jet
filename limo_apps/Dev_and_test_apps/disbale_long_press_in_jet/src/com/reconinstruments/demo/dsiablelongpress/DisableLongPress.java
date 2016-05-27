package com.reconinstruments.demo.disablelongpress;

import android.app.Activity;
import android.os.Bundle;
import com.reconinstruments.utils.UIUtils;

public class DisableLongPress extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
	UIUtils.setButtonHoldShouldLaunchApp(this,false);
	// ^ Set to true to enable it back
    }
}
