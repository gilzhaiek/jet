package com.patrick.jumpdebug;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class JumpDebugActivity extends Activity {
	/** Called when the activity is first created. */
	
	boolean debug;
	float factor;
	int minimum;
	
	private TextView debug_status;
	private Button debug_on;
	private Button debug_off;

	private TextView factor_status;
	private EditText factor_edit;
	private Button factor_plus;
	private Button factor_minus;
	private Button factor_set;

	private TextView minimum_status;
	private EditText minimum_edit;
	private Button minimum_plus;
	private Button minimum_minus;
	private Button minimum_set;
	
	private BroadcastReceiver br = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context , Intent intent){
			Bundle extra = intent.getExtras();
			debug = extra.getBoolean("debug");
			factor = extra.getFloat("factor");
			minimum = extra.getInt("minimum");
			
			Log.d("JumpDebug" , "debug:"+debug+" factor:"+factor+" minimum:"+minimum);
			
			setupDebug();
			setupFactor();
			setupMinimum();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		registerReceiver(br, new IntentFilter("com.patrick.debugjump.info"));
		sendBroadcast(new Intent("com.patrick.debugjump.refresh"));
		
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		unregisterReceiver(br);
	}
	private void setupDebug(){
		
		debug_status = (TextView) findViewById(R.id.debug);
		debug_status.setText("Debug:"+debug);
		debug_on = (Button) findViewById(R.id.debug_on);
		debug_off = (Button) findViewById(R.id.debug_off);

		debug_on.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				Intent intent = new Intent("com.patrick.debugjump.debug");
				intent.putExtra("debug", true);
				Log.d("JumpDebugActivity","debug intent broadcasted");
				sendBroadcast(intent);
			}
		});
		debug_off.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				Intent intent = new Intent("com.patrick.debugjump.debug");
				intent.putExtra("debug", false);
				Log.d("JumpDebugActivity","debug intent broadcasted");
				sendBroadcast(intent);
			}
		});
	}
	private void setupFactor(){

		factor_status = (TextView) findViewById(R.id.factor);
		factor_status.setText("k_factor:"+factor);
		factor_edit = (EditText) findViewById(R.id.factor_edit);
		factor_edit.setText(""+factor);
		factor_edit.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK){
					try {
					factor = Float.parseFloat(""+factor_edit.getText());
					return true;
					}
					catch (NumberFormatException e) {
						Log.e("JumpDebug" , e.getLocalizedMessage());
						factor_edit.setText(""+factor);
						return true;
					}
				}
				return false;
			}});		
		
		factor_plus = (Button) findViewById(R.id.factor_plus);
		factor_minus = (Button) findViewById(R.id.factor_minus);
		
		factor_plus.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				factor += 0.01f;
				factor = Math.round(factor*100)/100.0f;
				factor_edit.setText(""+factor);
				
			}
			
		});
		factor_minus.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				factor -= 0.01f;
				factor = Math.round(factor*100)/100.0f;
				factor_edit.setText(""+factor);
				
			}
			
		});
		
		factor_set = (Button) findViewById(R.id.factor_set);
		factor_set.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				Intent intent = new Intent("com.patrick.debugjump.factor");
				intent.putExtra("factor", factor);
				sendBroadcast(intent);
			}
		});

	}

	private void setupMinimum(){

		minimum_status = (TextView) findViewById(R.id.minimum);
		minimum_status.setText("MinJumpAir:"+minimum);
		minimum_edit = (EditText) findViewById(R.id.minimum_edit);
		
		
		minimum_edit.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK){
					try {
					minimum = Integer.parseInt(""+minimum_edit.getText());
					return true;
					}
					catch (NumberFormatException e) {
						Log.e("JumpDebug" , e.getLocalizedMessage());
						minimum_edit.setText(""+minimum);
						return true;
					}
				}
				return false;
			}});
		
		minimum_plus = (Button) findViewById(R.id.minimum_plus);
		minimum_minus = (Button) findViewById(R.id.minimum_minus);
		
		minimum_plus.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				minimum += 100;
				minimum_edit.setText(""+minimum);
				
			}
			
		});
		minimum_minus.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				minimum -= 100;
				minimum_edit.setText(""+minimum);
				
			}
			
		});
		
		minimum_set = (Button) findViewById(R.id.minimum_set);
		minimum_set.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View view){
				Intent intent = new Intent("com.patrick.debugjump.minimum");
				intent.putExtra("minimum", minimum);
				sendBroadcast(intent);


			}
		});
	}
}
