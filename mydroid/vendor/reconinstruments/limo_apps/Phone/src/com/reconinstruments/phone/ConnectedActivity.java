package com.reconinstruments.phone;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;

import com.reconinstruments.ifisoakley.OakleyDecider;
import com.reconinstruments.utils.BTHelper;

// copied from dash launcher

public abstract class ConnectedActivity extends FragmentActivity {
	static final String TAG = "ConnectedActivity";

	// this has to be called after sub activity on create
	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		if(!BTHelper.isConnected(this))
			onDisconnect();
	}
	public void onDisconnect(){


		FrameLayout mainView = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
		View overlay = (View) inflater.inflate(R.layout.activity_android_overlay, null);
		if (OakleyDecider.isOakley()) {
		    TextView conn = (TextView) findViewById(R.id.activity_android_overlay_text);
		    conn.setText(R.string.android_overlay_oakley);
		}

		mainView.addView(overlay, new LinearLayout.LayoutParams(mainView.getLayoutParams().width, mainView.getLayoutParams().height));
	}
}
