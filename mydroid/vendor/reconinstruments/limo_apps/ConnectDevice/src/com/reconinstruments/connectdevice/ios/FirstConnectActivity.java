package com.reconinstruments.connectdevice.ios;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;

import com.reconinstruments.connectdevice.ChooseDeviceActivity;
import com.reconinstruments.connectdevice.ConnectionActivity;
import com.reconinstruments.connectdevice.R;

import com.reconinstruments.ifisoakley.OakleyDecider;
import com.reconinstruments.utils.DeviceUtils;


public class FirstConnectActivity extends ConnectionActivity {
	protected static final String TAG = "FirstConnectActivity";

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		this.setContentView(R.layout.activity_ios_first_jet);
		if(DeviceUtils.isLimo()) {
			TextView textTV = (TextView) findViewById(R.id.activity_ios_first_text);
			if (OakleyDecider.isOakley()) {
				textTV
				.setText
				(Html
						.fromHtml(getResources().getString(R.string.ios_first_oakley)));
			}else{
				textTV
				.setText
				(Html
						.fromHtml(getResources().getString(R.string.ios_first)));
			}
//			this.setContentView(R.layout.activity_ios_first);
		}else{
			
			TextView textTV = (TextView) findViewById(R.id.activity_ios_first_text);
			textTV
			.setText
			(Html
					.fromHtml(
							"Go to <b>Settings > Bluetooth</b> on your iPhone and ensure Bluetooth is &nbsp<img src=\"on_switch.png\" align=\"middle\"> Stay in your Bluetooth Settings while your HUD searches for your iPhone.",
							new ImageGetter() {
								@Override
								public Drawable getDrawable(String source) {
									int id;
									if (source
											.equals("on_switch.png")) {
										id = R.drawable.on_switch;
									} else {
										return null;
									}
									LevelListDrawable d = new LevelListDrawable();
									Drawable empty = getResources()
											.getDrawable(id);
									d.addLevel(0, 0, empty);
									d.setBounds(0, 0,
											empty.getIntrinsicWidth(),
											empty.getIntrinsicHeight());
									return d;
								}
							}, null));
		}

		if(DeviceUtils.isLimo()) {
			final View next = (View) findViewById(R.id.next);
			next.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					next.setFocusable(false);
//					if(DeviceUtils.isLimo()){
//						startActivity(new Intent(FirstConnectActivity.this,
//								ConnectActivity.class)
//								.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY));
//					}else{
//						startActivity(new Intent(FirstConnectActivity.this,
//								BtNotificationThirdActivity.class)
//								.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
//					}
//					FirstConnectActivity.this.finish();
				}
			});
		
			next.setOnFocusChangeListener(new OnFocusChangeListener() {
	
				public void onFocusChange(View v, boolean hasFocus) {
				    // Was causing crashes on MOLive. Not even sure why.
					// TransitionDrawable transition = (TransitionDrawable) v
					// 		.getBackground();
					// if (hasFocus) {
					// 	transition.startTransition(300);
					// } else {
					// 	transition.resetTransition();
					// }
				}
			});
		}

		// if (OakleyDecider.isOakley()) {
		// 	TextView conn = (TextView) findViewById(R.id.activity_ios_first_text);
		// 	conn.setText(R.string.ios_first_oakley);
		// }

	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
	               keyCode == KeyEvent.KEYCODE_ENTER){
		    if(!DeviceUtils.isLimo()){
				startActivity(new Intent(FirstConnectActivity.this,
						BtNotificationThirdActivity.class));
				
			}else{
				startActivity(new Intent(FirstConnectActivity.this,
						ConnectActivity.class));
			}
			FirstConnectActivity.this.finish();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public void onBackPressed() {
//		if(!DeviceUtils.isLimo()){
			startActivity(new Intent(FirstConnectActivity.this,
					ChooseDeviceActivity.class));
			FirstConnectActivity.this.finish();
//		}
	}
}
