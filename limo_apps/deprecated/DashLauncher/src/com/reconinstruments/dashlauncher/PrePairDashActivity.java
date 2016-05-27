package com.reconinstruments.dashlauncher;


import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;

public class PrePairDashActivity extends BoardActivity {
	protected static final String TAG = "ConnectActivity";
	
	private View setupItem, noshowItem;
	
	int CONNECT_REQUEST_CODE = 0;
	@Override
	protected void onCreate(Bundle arg0)
	{
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_pre_pair_dash);	
		
		setupItem = findViewById(R.id.setup_item);
		noshowItem = findViewById(R.id.no_show_item);
		
		setupItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Settings.System.putString(getContentResolver(), "DisableSmartphone", "false");
				
				startActivityForResult(new Intent("com.reconinstruments.connectdevice.CONNECT"),CONNECT_REQUEST_CODE);
			}
		});
		
		setupItem.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v.getBackground();
				if(hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
		
		noshowItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				Settings.System.putString(getContentResolver(), "DisableSmartphone", "true");
				finish();
			}
		});
		
		noshowItem.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				TransitionDrawable transition = (TransitionDrawable) v.getBackground();
				if(hasFocus) {
					transition.startTransition(300);
				} else {
					transition.resetTransition();
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==CONNECT_REQUEST_CODE)
			finish();
	}
	
}
