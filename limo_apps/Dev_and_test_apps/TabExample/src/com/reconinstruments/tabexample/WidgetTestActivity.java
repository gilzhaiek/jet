package com.reconinstruments.tabexample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class WidgetTestActivity extends Activity {
    
	private View mTabView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mTabView = new TestTabView(this);
        
        setContentView(mTabView);
    }
        
}